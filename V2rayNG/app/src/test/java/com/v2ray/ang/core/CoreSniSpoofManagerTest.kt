package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.SniSpoofScope
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.NetworkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreSniSpoofManagerTest {

    @Test
    fun testValidationRequiresBothFieldsWhenEnabled() {
        assertFalse(CoreSniSpoofManager.isValidActiveConfig(SniSpoofConfig(enabled = false, fakeSni = "chatgpt.com", connectIp = "104.17.100.50")))
        assertFalse(CoreSniSpoofManager.isValidActiveConfig(SniSpoofConfig(enabled = true, fakeSni = "", connectIp = "104.17.100.50")))
        assertFalse(CoreSniSpoofManager.isValidActiveConfig(SniSpoofConfig(enabled = true, fakeSni = "chatgpt.com", connectIp = "  ")))
        assertTrue(CoreSniSpoofManager.isValidActiveConfig(SniSpoofConfig(enabled = true, fakeSni = "chatgpt.com", connectIp = "104.17.100.50", scope = SniSpoofScope.GLOBAL)))
    }

    @Test
    fun testAllFourScopesMatchCorrectly() {
        val p1 = ProfileItem(
            configType = EConfigType.VLESS,
            server = "cdn.example.com",
            serverPort = "443",
            security = AppConfig.TLS,
            network = NetworkType.WS.type,
            subscriptionId = "subA"
        )
        val p2 = ProfileItem(
            configType = EConfigType.VLESS,
            server = "edge.other.com",
            serverPort = "443",
            security = AppConfig.TLS,
            subscriptionId = "subB"
        )

        // 1. Single server
        val singleCfg = SniSpoofConfig(
            enabled = true,
            fakeSni = "chatgpt.com",
            connectIp = "104.17.100.50",
            scope = SniSpoofScope.SINGLE_SERVER,
            targetServerGuids = setOf("g1")
        )
        val singleApplied = CoreSniSpoofManager.applySpoofIfEligible("g1", p1, singleCfg)
        assertNotSame(p1, singleApplied)
        assertEquals("104.17.100.50", singleApplied.server)
        assertEquals("chatgpt.com", singleApplied.sni)
        assertEquals("cdn.example.com", singleApplied.host) // preserved CDN host
        assertEquals("edge.other.com", CoreSniSpoofManager.applySpoofIfEligible("g2", p2, singleCfg).server)

        // 2. Multi server
        val multiCfg = singleCfg.copy(scope = SniSpoofScope.MULTI_SERVER, targetServerGuids = setOf("g1", "g2"))
        assertEquals("104.17.100.50", CoreSniSpoofManager.applySpoofIfEligible("g1", p1, multiCfg).server)
        assertEquals("104.17.100.50", CoreSniSpoofManager.applySpoofIfEligible("g2", p2, multiCfg).server)
        assertEquals("cdn.example.com", CoreSniSpoofManager.applySpoofIfEligible("g3", p1, multiCfg).server)

        // 3. Subscription
        val subCfg = singleCfg.copy(scope = SniSpoofScope.SUBSCRIPTION, targetSubscriptionIds = setOf("subB"), targetServerGuids = emptySet())
        assertEquals("cdn.example.com", CoreSniSpoofManager.applySpoofIfEligible("g1", p1, subCfg).server)
        assertEquals("104.17.100.50", CoreSniSpoofManager.applySpoofIfEligible("g2", p2, subCfg).server)

        // 4. Global
        val globalCfg = singleCfg.copy(scope = SniSpoofScope.GLOBAL, targetServerGuids = emptySet())
        assertEquals("104.17.100.50", CoreSniSpoofManager.applySpoofIfEligible("g1", p1, globalCfg).server)
        assertEquals("104.17.100.50", CoreSniSpoofManager.applySpoofIfEligible("g2", p2, globalCfg).server)
        assertEquals("cdn.example.com", p1.server) // original stored profile untouched
    }

    @Test
    fun testPreservesExplicitHostAndSkipsWireguardOrDisabled() {
        val wsExplicitHost = ProfileItem(
            configType = EConfigType.VMESS,
            server = "origin.example.com",
            host = "worker.example.com",
            network = NetworkType.WS.type,
            security = AppConfig.TLS,
            sni = "origin.example.com"
        )
        val cfg = SniSpoofConfig(
            enabled = true,
            fakeSni = "custom-sni.org",
            connectIp = "1.2.3.4",
            scope = SniSpoofScope.GLOBAL
        )
        val applied = CoreSniSpoofManager.applySpoofIfEligible("g1", wsExplicitHost, cfg)
        assertEquals("1.2.3.4", applied.server)
        assertEquals("custom-sni.org", applied.sni)
        assertEquals("worker.example.com", applied.host)

        val wg = ProfileItem(configType = EConfigType.WIREGUARD, server = "10.0.0.1")
        assertEquals("10.0.0.1", CoreSniSpoofManager.applySpoofIfEligible("g_wg", wg, cfg).server)

        val roundtrip = CoreSniSpoofManager.parseConfigJson(CoreSniSpoofManager.serializeConfigJson(cfg))
        assertEquals(cfg, roundtrip)
    }

    @Test
    fun testTargetSelectionValidationAcrossAllScopes() {
        assertTrue(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.GLOBAL)))
        assertFalse(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SINGLE_SERVER)))
        assertFalse(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SINGLE_SERVER, targetServerGuids = setOf(" "))))
        assertTrue(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SINGLE_SERVER, targetServerGuids = setOf("g1"))))

        assertFalse(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.MULTI_SERVER)))
        assertTrue(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.MULTI_SERVER, targetServerGuids = setOf("g1", "g2"))))

        assertFalse(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SUBSCRIPTION)))
        assertFalse(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SUBSCRIPTION, targetSubscriptionIds = setOf(""))))
        assertTrue(CoreSniSpoofManager.hasValidTargetSelection(SniSpoofConfig(scope = SniSpoofScope.SUBSCRIPTION, targetSubscriptionIds = setOf("sub-1"))))
    }
}
