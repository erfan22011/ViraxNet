package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.util.Utils
import android.content.Context
import java.io.File

/**
 * Builds runtime routing and DNS bypass rules for routing Iranian websites and IPs directly.
 */
object CoreRoutingBypassHelper {

    fun resolveEffectiveDomainStrategy(
        configuredStrategy: String?,
        bypassIranianSites: Boolean
    ): String {
        val base = configuredStrategy?.takeIf { it.isNotBlank() } ?: "AsIs"
        if (!bypassIranianSites) return base
        return if (base.equals("AsIs", ignoreCase = true)) "IPIfNonMatch" else base
    }

    fun buildBypassIranianDomains(hasGeositeDat: Boolean): List<String> {
        return buildList {
            add(AppConfig.DOMAIN_IR)
            if (hasGeositeDat) {
                add(AppConfig.GEOSITE_CATEGORY_IR)
            }
        }
    }

    fun buildBypassIranianDomainRule(hasGeositeDat: Boolean): RulesetItem {
        return RulesetItem(
            remarks = "ViraxNet Bypass Iranian Domains",
            domain = ArrayList(buildBypassIranianDomains(hasGeositeDat)),
            outboundTag = AppConfig.TAG_DIRECT,
            enabled = true,
        )
    }

    fun buildBypassIranianIpRule(hasGeoipDat: Boolean): RulesetItem? {
        if (!hasGeoipDat) return null
        return RulesetItem(
            remarks = "ViraxNet Bypass Iranian IPs",
            ip = arrayListOf(AppConfig.GEOIP_IR),
            outboundTag = AppConfig.TAG_DIRECT,
            enabled = true,
        )
    }

    fun buildBypassRulesets(
        bypassIranianSites: Boolean,
        hasGeositeDat: Boolean,
        hasGeoipDat: Boolean
    ): List<RulesetItem> {
        if (!bypassIranianSites) return emptyList()
        return buildList {
            add(buildBypassIranianDomainRule(hasGeositeDat))
            buildBypassIranianIpRule(hasGeoipDat)?.let(::add)
        }
    }

    fun hasAssetFile(context: Context, fileName: String): Boolean {
        return runCatching {
            val dir = File(Utils.userAssetPath(context))
            val file = File(dir, fileName)
            file.exists() && file.length() > 0L
        }.getOrDefault(false)
    }
}
