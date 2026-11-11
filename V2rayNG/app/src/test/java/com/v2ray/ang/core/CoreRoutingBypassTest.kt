package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreRoutingBypassTest {

    @Test
    fun testResolveEffectiveDomainStrategy() {
        assertEquals("AsIs", CoreRoutingBypassHelper.resolveEffectiveDomainStrategy("AsIs", false))
        assertEquals("IPIfNonMatch", CoreRoutingBypassHelper.resolveEffectiveDomainStrategy("AsIs", true))
        assertEquals("IPIfNonMatch", CoreRoutingBypassHelper.resolveEffectiveDomainStrategy(null, true))
        assertEquals("IPOnDemand", CoreRoutingBypassHelper.resolveEffectiveDomainStrategy("IPOnDemand", true))
    }

    @Test
    fun testBuildBypassRulesetsDisabledReturnsEmpty() {
        val rules = CoreRoutingBypassHelper.buildBypassRulesets(
            bypassIranianSites = false,
            hasGeositeDat = true,
            hasGeoipDat = true
        )
        assertTrue(rules.isEmpty())
    }

    @Test
    fun testBuildBypassRulesetsWithGeoAssetsIncludesDomainAndIpRules() {
        val rules = CoreRoutingBypassHelper.buildBypassRulesets(
            bypassIranianSites = true,
            hasGeositeDat = true,
            hasGeoipDat = true
        )
        assertEquals(2, rules.size)
        assertEquals(AppConfig.TAG_DIRECT, rules[0].outboundTag)
        assertEquals(listOf(AppConfig.DOMAIN_IR, AppConfig.GEOSITE_CATEGORY_IR), rules[0].domain)
        assertEquals(AppConfig.TAG_DIRECT, rules[1].outboundTag)
        assertEquals(listOf(AppConfig.GEOIP_IR), rules[1].ip)
    }

    @Test
    fun testBuildBypassRulesetsWithoutGeoAssetsFallsBackToDomainIrOnly() {
        val rules = CoreRoutingBypassHelper.buildBypassRulesets(
            bypassIranianSites = true,
            hasGeositeDat = false,
            hasGeoipDat = false
        )
        assertEquals(1, rules.size)
        assertEquals(AppConfig.TAG_DIRECT, rules[0].outboundTag)
        assertEquals(listOf(AppConfig.DOMAIN_IR), rules[0].domain)
    }
}
