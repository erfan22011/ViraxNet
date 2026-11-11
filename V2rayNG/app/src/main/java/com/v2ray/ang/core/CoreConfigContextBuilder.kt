package com.v2ray.ang.core

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.CoreConfigContext
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.BalancerStrategyType
import com.v2ray.ang.enums.CoreResolvedType
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils

/**
 * Build runtime context from the selected profile.
 *
 * All outbound type analysis is completed here for both the selected profile
 * and routing targets. Custom profiles are returned immediately without
 * entering the normal analysis flow.
 */
object CoreConfigContextBuilder {

    /**
     * Load one profile and produce a fully analyzed context.
     *
     * Null is returned only when the selected profile cannot be loaded.
     */
    fun build(context: Context, guid: String): CoreConfigContext? {
        val config = MmkvManager.decodeServerConfig(guid) ?: return null

        // CUSTOM: return immediately — CoreConfigManager handles this path on its own.
        if (config.configType == EConfigType.CUSTOM) {
            return CoreConfigContext(context = context, guid = guid, isCustom = true)
        }

        val spoofConfig = CoreSniSpoofManager.decodeConfig()

        // Step 1: Resolve the main outbound (always tag = TAG_PROXY).
        val primaryResolvedOutbound = resolveOutbound(
            tag = AppConfig.TAG_PROXY,
            profile = config,
            profileGuid = guid,
            spoofConfig = spoofConfig
        ) ?: run {
            LogUtil.e(AppConfig.TAG, "Failed to resolve main outbound for '${config.remarks}'")
            return null
        }

        // Step 2: Resolve all non-builtin routing outbound tags.
        val routingResolvedOutbounds = resolveRoutingOutbounds(spoofConfig)
        val resolvedOutbounds = listOf(primaryResolvedOutbound) + routingResolvedOutbounds
        val fallbackResolvedOutbounds = resolveFallbackOutbounds(resolvedOutbounds)
        val routingDomainRules = collectRoutingDomainRulesForDns(context)

        return CoreConfigContext(
            context = context,
            guid = guid,
            resolvedOutbounds = resolvedOutbounds + fallbackResolvedOutbounds,
            routingDomainRules = routingDomainRules,
        )
    }

    /**
     * Resolve one outbound target into a normalized outbound entry.
     *
     * Custom profiles are ignored at this stage and produce no entry.
     */
    private fun resolveOutbound(
        tag: String,
        profile: ProfileItem,
        profileGuid: String? = null,
        spoofConfig: SniSpoofConfig = CoreSniSpoofManager.decodeConfig()
    ): CoreConfigContext.ResolvedOutbound? {
        if (profile.configType == EConfigType.CUSTOM) {
            return null
        }

        val (resolvedProfiles, resolvedType) = when (profile.configType) {
            EConfigType.POLICYGROUP -> Pair(
                resolvePolicyGroupProfiles(profile, spoofConfig),
                CoreResolvedType.POLICYGROUP,
            )

            EConfigType.PROXYCHAIN -> {
                val chainProfiles = resolveProxyChainProfiles(profile, profileGuid, spoofConfig)
                val type = if (chainProfiles.size <= 1) CoreResolvedType.NORMAL else CoreResolvedType.PROXYCHAIN
                Pair(chainProfiles, type)
            }

            else -> {
                val chainProfiles = resolveProxyChainProfilesFromGroup(profile, profileGuid, spoofConfig)
                val type = if (chainProfiles.size <= 1) CoreResolvedType.NORMAL else CoreResolvedType.PROXYCHAIN
                Pair(chainProfiles, type)
            }
        }

        return CoreConfigContext.ResolvedOutbound(
            tag = tag,
            profile = profile,
            resolvedProfiles = resolvedProfiles,
            resolvedType = resolvedType,
        )
    }

    /**
     * Collect and resolve non-builtin routing targets from enabled rules.
     *
     * Invalid or empty targets are skipped and handled by fallback logic later.
     */
    private fun resolveRoutingOutbounds(spoofConfig: SniSpoofConfig): List<CoreConfigContext.ResolvedOutbound> {
        val rulesetItems = MmkvManager.decodeRoutingRulesets() ?: return emptyList()
        val resolvedOutbounds = mutableListOf<CoreConfigContext.ResolvedOutbound>()
        val processedTags = mutableSetOf<String>()

        try {
            rulesetItems
                .filter { it.enabled }
                .mapNotNull { it.outboundTag.takeIf { tag -> tag.isNotBlank() } }
                .filter { tag -> tag !in AppConfig.BUILTIN_OUTBOUND_TAGS }
                .distinct()
                .forEach { tag ->
                    if (tag in processedTags) {
                        return@forEach
                    }
                    processedTags.add(tag)

                    try {
                        val (targetGuid, profile) = SettingsManager.getServerWithGuidViaRemarks(tag) ?: run {
                            LogUtil.w(AppConfig.TAG, "Routing tag '$tag' has no matching profile — will fall back to proxy at routing time")
                            return@forEach
                        }
                        val resolvedOutbound = resolveOutbound(
                            tag = tag,
                            profile = profile,
                            profileGuid = targetGuid,
                            spoofConfig = spoofConfig
                        ) ?: run {
                            LogUtil.w(AppConfig.TAG, "Cannot use CUSTOM profile as routing outbound for tag '$tag', skipping")
                            return@forEach
                        }
                        if (resolvedOutbound.resolvedProfiles.isEmpty()) {
                            LogUtil.w(AppConfig.TAG, "Routing outbound '$tag' resolved to empty list, skipping")
                            return@forEach
                        }
                        resolvedOutbounds.add(resolvedOutbound)
                        LogUtil.d(AppConfig.TAG, "Resolved routing outbound: tag='$tag', type='${resolvedOutbound.resolvedType}', profiles=${resolvedOutbound.resolvedProfiles.size}")
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "Failed to resolve routing outbound for tag '$tag', skipping", e)
                    }
                }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve routing outbounds from rulesets", e)
        }

        return resolvedOutbounds
    }

    private fun resolvePolicyGroupProfiles(
        config: ProfileItem,
        spoofConfig: SniSpoofConfig
    ): List<ProfileItem> {
        try {
            val serverList = MmkvManager.decodeAllServerList()
            return serverList
                .asSequence()
                .mapNotNull { id -> MmkvManager.decodeServerConfig(id)?.let { id to it } }
                .filter { (_, profile) ->
                    val subscriptionId = config.policyGroupSubscriptionId
                    if (subscriptionId.isNullOrBlank()) {
                        true
                    } else {
                        profile.subscriptionId == subscriptionId
                    }
                }
                .filter { (_, profile) ->
                    val filter = config.policyGroupFilter
                    if (filter.isNullOrBlank()) {
                        true
                    } else {
                        try {
                            Regex(filter).containsMatchIn(profile.remarks)
                        } catch (_: Exception) {
                            profile.remarks.contains(filter)
                        }
                    }
                }
                .filter { (_, profile) -> profile.server.isNotNullEmpty() }
                .filter { (_, profile) -> Utils.isPureIpAddress(profile.server!!) || Utils.isValidUrl(profile.server!!) }
                .filter { (_, profile) -> !profile.configType.isComplexType() }
                .map { (id, profile) -> CoreSniSpoofManager.applySpoofIfEligible(id, profile, spoofConfig) }
                .toList()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve policy group profiles for '${config.remarks}'", e)
            return listOf(config)
        }
    }

    private fun resolveProxyChainProfiles(
        config: ProfileItem,
        configGuid: String?,
        spoofConfig: SniSpoofConfig
    ): List<ProfileItem> {
        if (config.proxyChainProfiles.isNullOrBlank()) {
            return listOf(CoreSniSpoofManager.applySpoofIfEligible(configGuid, config, spoofConfig))
        }

        try {
            return config.proxyChainProfiles.orEmpty().split(",")
                .asSequence()
                .mapNotNull { remark -> SettingsManager.getServerWithGuidViaRemarks(remark) }
                .filter { (_, profile) -> profile.server.isNotNullEmpty() }
                .filter { (_, profile) -> Utils.isPureIpAddress(profile.server!!) || Utils.isValidUrl(profile.server!!) }
                .filter { (_, profile) -> !profile.configType.isComplexType() }
                .map { (id, profile) -> CoreSniSpoofManager.applySpoofIfEligible(id, profile, spoofConfig) }
                .toList()
                .reversed()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve proxy chain profiles for '${config.remarks}'", e)
            return listOf(config)
        }
    }

    /**
     * Resolve chain nodes from subscription neighbors in order: next, current, prev.
     *
     * When no chain is available, return a single-node result.
     */
    private fun resolveProxyChainProfilesFromGroup(
        config: ProfileItem,
        configGuid: String?,
        spoofConfig: SniSpoofConfig
    ): List<ProfileItem> {
        val currentEffective = CoreSniSpoofManager.applySpoofIfEligible(configGuid, config, spoofConfig)
        if (config.subscriptionId.isEmpty()) {
            return listOf(currentEffective)
        }

        try {
            val subItem = MmkvManager.decodeSubscription(config.subscriptionId) ?: return listOf(currentEffective)
            val resolved = mutableListOf<ProfileItem>()
            SettingsManager.getServerWithGuidViaRemarks(subItem.nextProfile)?.let { (nextId, nextProfile) ->
                resolved.add(CoreSniSpoofManager.applySpoofIfEligible(nextId, nextProfile, spoofConfig))
            }
            resolved.add(currentEffective)
            SettingsManager.getServerWithGuidViaRemarks(subItem.prevProfile)?.let { (prevId, prevProfile) ->
                resolved.add(CoreSniSpoofManager.applySpoofIfEligible(prevId, prevProfile, spoofConfig))
            }
            return resolved
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve proxy chain from group for '${config.remarks}'", e)
            return listOf(currentEffective)
        }
    }

    /**
     * Collect enabled routing domain rules in original order for DNS segmentation.
     *
     * outbounds are normalized into three tags only: proxy / direct / block.
     */
    private fun collectRoutingDomainRulesForDns(context: Context): List<CoreConfigContext.RoutingDomainRule> {
        val result = mutableListOf<CoreConfigContext.RoutingDomainRule>()
        val bypassIranianSites = MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_IRANIAN_SITES, false)
        if (bypassIranianSites) {
            val hasGeositeDat = CoreRoutingBypassHelper.hasAssetFile(context, AppConfig.GEOSITE_DAT)
            result.add(
                CoreConfigContext.RoutingDomainRule(
                    domain = CoreRoutingBypassHelper.buildBypassIranianDomains(hasGeositeDat),
                    outboundTag = AppConfig.TAG_DIRECT
                )
            )
        }

        val rulesetItems = MmkvManager.decodeRoutingRulesets() ?: return result

        rulesetItems
            .asSequence()
            .filter { it.enabled }
            .filter { !it.domain.isNullOrEmpty() }
            .forEach { rule ->
                val normalizedOutboundTag = when (rule.outboundTag) {
                    AppConfig.TAG_DIRECT -> AppConfig.TAG_DIRECT
                    AppConfig.TAG_BLOCKED -> AppConfig.TAG_BLOCKED
                    else -> AppConfig.TAG_PROXY
                }
                result.add(
                    CoreConfigContext.RoutingDomainRule(
                        domain = rule.domain.orEmpty(),
                        outboundTag = normalizedOutboundTag
                    )
                )
            }

        return result
    }

    /**
     * Resolve and collect fallback outbounds from all POLICYGROUP nodes.
     *
     * Fallback targets must not overlap with already resolved tags or builtin tags.
     */
    private fun resolveFallbackOutbounds(resolvedOutbounds: List<CoreConfigContext.ResolvedOutbound>): List<CoreConfigContext.ResolvedOutbound> {
        return resolvedOutbounds
            .asSequence()
            .filter { it.resolvedType == CoreResolvedType.POLICYGROUP }
            .filter { BalancerStrategyType.from(it.profile.policyGroupType).supportsObservatory && it.profile.policyGroupTestOutbounds != false }
            .mapNotNull { it.profile.policyGroupFallbackTag }
            .filter { it !in AppConfig.BUILTIN_OUTBOUND_TAGS && resolvedOutbounds.none { outbound -> outbound.tag == it } }
            .distinct()
            .mapNotNull { tag ->
                SettingsManager.getServerViaRemarks(tag)
                    ?.takeUnless { it.configType == EConfigType.CUSTOM || it.configType == EConfigType.POLICYGROUP }
                    ?.let { resolveOutbound(tag, it) }
            }
            .toList()
    }
}
