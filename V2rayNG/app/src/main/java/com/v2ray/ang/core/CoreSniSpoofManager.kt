package com.v2ray.ang.core

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.SniSpoofScope
import com.v2ray.ang.dto.ViraxSniServerOption
import com.v2ray.ang.dto.ViraxSniSubscriptionOption
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.ViraxAccessManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.Utils

/**
 * Resolves and applies experimental SNI Spoofing (Fake SNI + Connect IP) on runtime copies of profiles
 * without mutating stored server configurations.
 */
object CoreSniSpoofManager {

    private val HOST_PRESERVING_TRANSPORTS = setOf(
        NetworkType.WS.type,
        NetworkType.HTTP_UPGRADE.type,
        NetworkType.XHTTP.type,
        NetworkType.H2.type,
        NetworkType.HTTP.type,
    )

    fun decodeConfig(): SniSpoofConfig {
        val json = MmkvManager.decodeSettingsString(AppConfig.PREF_SNI_SPOOFING_CONFIG)
        if (json.isNullOrBlank()) return SniSpoofConfig()
        return parseConfigJson(json)
    }

    fun encodeConfig(config: SniSpoofConfig) {
        MmkvManager.encodeSettings(AppConfig.PREF_SNI_SPOOFING_CONFIG, serializeConfigJson(config))
    }

    fun loadSniServerOptions(): List<ViraxSniServerOption> =
        MmkvManager.decodeAllServerList().mapNotNull { guid ->
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            if (profile.configType.isComplexType() || profile.configType == EConfigType.WIREGUARD) {
                return@mapNotNull null
            }
            ViraxSniServerOption(
                guid = guid,
                remarks = profile.remarks.ifBlank { guid },
                subscriptionId = profile.subscriptionId,
            )
        }

    fun loadSniSubscriptionOptions(): List<ViraxSniSubscriptionOption> =
        MmkvManager.decodeSubscriptions()
            .filter { it.subscription.url.isNotBlank() }
            .map { sub ->
                ViraxSniSubscriptionOption(
                    id = sub.guid,
                    remarks = sub.subscription.remarks,
                    isProtected = ViraxAccessManager.isProtectedSubscription(sub.subscription),
                )
            }

    internal fun parseConfigJson(json: String?): SniSpoofConfig {
        if (json.isNullOrBlank()) return SniSpoofConfig()
        return JsonUtil.fromJsonSafe(json, SniSpoofConfig::class.java) ?: SniSpoofConfig()
    }

    internal fun serializeConfigJson(config: SniSpoofConfig): String {
        return JsonUtil.toJson(config)
    }

    fun hasValidFieldFormats(config: SniSpoofConfig): Boolean {
        val fake = config.fakeSni.trim()
        val ip = config.connectIp.trim()
        if (fake.isNotEmpty() && !isDomainHost(fake)) return false
        if (ip.isNotEmpty() && !Utils.isPureIpAddress(ip)) return false
        return true
    }

    fun hasRequiredFields(config: SniSpoofConfig): Boolean {
        return hasValidFieldFormats(config)
    }

    fun hasValidTargetSelection(config: SniSpoofConfig): Boolean {
        return when (config.scope) {
            SniSpoofScope.GLOBAL -> true
            SniSpoofScope.SUBSCRIPTION -> config.targetSubscriptionIds.any { it.isNotBlank() }
            SniSpoofScope.SINGLE_SERVER,
            SniSpoofScope.MULTI_SERVER -> config.targetServerGuids.any { it.isNotBlank() }
        }
    }

    fun isValidActiveConfig(config: SniSpoofConfig): Boolean {
        return config.enabled && hasValidTargetSelection(config) && hasValidFieldFormats(config)
    }

    fun doesProfileMatchScope(
        serverGuid: String?,
        profile: ProfileItem,
        config: SniSpoofConfig
    ): Boolean {
        return when (config.scope) {
            SniSpoofScope.GLOBAL -> true
            SniSpoofScope.SUBSCRIPTION ->
                profile.subscriptionId.isNotBlank() && profile.subscriptionId in config.targetSubscriptionIds
            SniSpoofScope.SINGLE_SERVER ->
                !serverGuid.isNullOrBlank() && config.targetServerGuids.firstOrNull { it.isNotBlank() } == serverGuid
            SniSpoofScope.MULTI_SERVER ->
                !serverGuid.isNullOrBlank() && serverGuid in config.targetServerGuids
        }
    }

    fun applySpoofIfEligible(
        serverGuid: String?,
        profile: ProfileItem,
        config: SniSpoofConfig
    ): ProfileItem {
        if (!isValidActiveConfig(config)) return profile
        if (profile.configType.isComplexType() || profile.configType == EConfigType.WIREGUARD) {
            return profile
        }
        if (!doesProfileMatchScope(serverGuid, profile, config)) {
            return profile
        }

        val cleanFakeSni = config.fakeSni.trim()
        val cleanConnectIp = config.connectIp.trim()
        val originalServer = profile.server.orEmpty().trim()

        // 1. Preserve destination server unless an explicit IP redirect is provided
        val effectiveServer = if (cleanConnectIp.isNotEmpty() && Utils.isPureIpAddress(cleanConnectIp)) {
            cleanConnectIp
        } else {
            originalServer
        }

        // 2. Preserve domain-based CDN Host headers when rewriting server address to Connect IP
        val effectiveHost = if (cleanConnectIp.isNotEmpty() &&
            profile.host.isNullOrBlank() &&
            profile.network in HOST_PRESERVING_TRANSPORTS &&
            isDomainHost(originalServer)
        ) {
            originalServer
        } else {
            profile.host
        }

        // 3. Fake SNI if specified, otherwise keep original SNI
        val effectiveSni = if (cleanFakeSni.isNotEmpty()) cleanFakeSni else profile.sni

        // 4. Inject native Anti-DPI TLS ClientHello fragmentation into finalMask
        val effectiveFinalMask = injectAntiDpiFinalMask(profile.finalMask)

        return profile.copy(
            server = effectiveServer,
            sni = effectiveSni,
            host = effectiveHost,
            finalMask = effectiveFinalMask,
        )
    }

    internal fun injectAntiDpiFinalMask(existingFinalMask: String?): String {
        return try {
            val root = existingFinalMask?.takeIf { it.isNotBlank() }
                ?.let { JsonUtil.parseString(it)?.takeIf { json -> json.isJsonObject }?.asJsonObject }
                ?: JsonObject()

            val tcpArray = if (root.has("tcp") && root.get("tcp").isJsonArray) {
                root.getAsJsonArray("tcp")
            } else {
                JsonArray().also { root.add("tcp", it) }
            }

            val hasFragment = (0 until tcpArray.size()).any { i ->
                tcpArray.get(i)?.takeIf { it.isJsonObject }?.asJsonObject?.get("type")?.asString == "fragment"
            }

            if (!hasFragment) {
                val fragmentObj = JsonObject().apply {
                    addProperty("type", "fragment")
                    val settings = JsonObject().apply {
                        addProperty("packets", "tlshello")
                        addProperty("length", "50-100")
                        addProperty("delay", "10-20")
                        addProperty("maxSplit", "10")
                    }
                    add("settings", settings)
                }
                tcpArray.add(fragmentObj)
            }
            JsonUtil.toJson(root)
        } catch (_: Exception) {
            existingFinalMask.orEmpty()
        }
    }

    private val DOMAIN_LABEL_REGEX = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")

    internal fun isDomainHost(host: String): Boolean {
        val candidate = host.trim().trimEnd('.')
        if (candidate.isEmpty() || candidate.length > 253 || Utils.isPureIpAddress(candidate)) {
            return false
        }
        val labels = candidate.split('.')
        return labels.size >= 2 && labels.all { DOMAIN_LABEL_REGEX.matches(it) }
    }
}
