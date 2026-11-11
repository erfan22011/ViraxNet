package com.v2ray.ang.ui.virax

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.handler.ViraxAccessManager
import com.v2ray.ang.handler.ViraxPlanBadge
import com.v2ray.ang.ui.main.MainUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViraxConnectionManagerTest {

    @Test
    fun testFindFastestHealthyServer() {
        val servers = listOf(
            ViraxServerItem("s1", "Failed Server 1", -1L, false),
            ViraxServerItem("s2", "Slow Server", 120L, false),
            ViraxServerItem("s3", "Fast Server", 18L, false),
            ViraxServerItem("s4", "Untested Server", 0L, false),
            ViraxServerItem("s5", "Medium Server", 45L, false)
        )

        val fastest = ViraxConnectionManager.findFastestHealthyServer(servers)
        assertNotNull(fastest)
        assertEquals("s3", fastest?.guid)
        assertEquals(18L, fastest?.pingMillis)
    }

    @Test
    fun testFindFastestHealthyServerWhenAllFailed() {
        val servers = listOf(
            ViraxServerItem("s1", "Failed 1", -1L, false),
            ViraxServerItem("s2", "Failed 2", -10L, false),
            ViraxServerItem("s3", "Zero Ping", 0L, false)
        )

        val fastest = ViraxConnectionManager.findFastestHealthyServer(servers)
        assertNull(fastest)
    }

    @Test
    fun testDeduplicateProfiles() {
        val p1 = ProfileItem(configType = EConfigType.VMESS, server = "1.2.3.4", serverPort = "443")
        val p2 = ProfileItem(configType = EConfigType.VMESS, server = "1.2.3.4", serverPort = "443") // Duplicate of p1
        val p3 = ProfileItem(configType = EConfigType.VLESS, server = "1.2.3.4", serverPort = "443") // Different type
        val p4 = ProfileItem(configType = EConfigType.VMESS, server = "5.6.7.8", serverPort = "443") // Different IP

        val list = listOf(
            "guid1" to p1,
            "guid2" to p2,
            "guid3" to p3,
            "guid4" to p4
        )

        val duplicates = ViraxConnectionManager.deduplicateProfiles(list)
        assertEquals(1, duplicates.size)
        assertEquals("guid2", duplicates[0])
    }

    @Test
    fun testSubscriptionUiDataExpiryAndUsage() {
        val nowSec = System.currentTimeMillis() / 1000L

        // Expired subscription
        val expiredData = ViraxSubscriptionUiData(
            hasUserInfo = true,
            uploadBytes = 100,
            downloadBytes = 200,
            totalBytes = 1000,
            expireTimestamp = nowSec - 3600
        )
        assertTrue(expiredData.isExpired)
        assertEquals(300L, expiredData.usedBytes)
        assertEquals(0.3f, expiredData.progressFraction)

        // Active subscription
        val activeData = ViraxSubscriptionUiData(
            hasUserInfo = true,
            uploadBytes = 2000,
            downloadBytes = 3000,
            totalBytes = 10000,
            expireTimestamp = nowSec + 86400 * 10
        )
        assertFalse(activeData.isExpired)
        assertEquals(5000L, activeData.usedBytes)
        assertEquals(0.5f, activeData.progressFraction)
    }

    @Test
    fun testFindFastestHealthyServerWithEmptyList() {
        val servers = emptyList<ViraxServerItem>()
        val fastest = ViraxConnectionManager.findFastestHealthyServer(servers)
        assertNull(fastest)
    }

    @Test
    fun testFallbackNextServerSelection() {
        val servers = listOf(
            ViraxServerItem("s1", "Failed Target", 10L, false),
            ViraxServerItem("s2", "Next Fastest", 25L, false),
            ViraxServerItem("s3", "Slow", 200L, false)
        )
        // Skip s1 which failed, next fastest should be s2
        val remaining = servers.filter { it.guid != "s1" && it.pingMillis > 0 }
        val nextBest = ViraxConnectionManager.findFastestHealthyServer(remaining)
        assertNotNull(nextBest)
        assertEquals("s2", nextBest?.guid)
        assertEquals(25L, nextBest?.pingMillis)
    }

    @Test
    fun testFilterServers() {
        val servers = listOf(
            ViraxServerItem("s1", "Germany - Frankfurt", 12L, false),
            ViraxServerItem("s2", "Netherlands - Amsterdam", 180L, false),
            ViraxServerItem("s3", "Singapore - SG", 22L, false),
            ViraxServerItem("s4", "Turkey - Istanbul", -1L, false),
            ViraxServerItem("s5", "United States - NY", 0L, false)
        )

        // ALL filter
        val all = filterServers(servers, ViraxServerFilter.ALL)
        assertEquals(5, all.size)

        // HIGH_SPEED filter (1..150 ms, sorted ascending)
        val highSpeed = filterServers(servers, ViraxServerFilter.HIGH_SPEED)
        assertEquals(2, highSpeed.size)
        assertEquals("s1", highSpeed[0].guid)
        assertEquals("s3", highSpeed[1].guid)

        // STABLE filter (>0 ms, sorted ascending)
        val stable = filterServers(servers, ViraxServerFilter.STABLE)
        assertEquals(3, stable.size)
        assertEquals(12L, stable[0].pingMillis)
        assertEquals(22L, stable[1].pingMillis)
        assertEquals(180L, stable[2].pingMillis)

        // Search query filter
        val searched = filterServers(servers, ViraxServerFilter.ALL, searchQuery = "Singapore")
        assertEquals(1, searched.size)
        assertEquals("s3", searched[0].guid)
    }

    @Test
    fun testSubscriptionUiDataMissingInfoAndEdgeCases() {
        // Missing user info
        val missingData = ViraxSubscriptionUiData(
            hasUserInfo = false,
            uploadBytes = 0,
            downloadBytes = 0,
            totalBytes = 0,
            expireTimestamp = 0
        )
        assertFalse(missingData.isExpired)
        assertEquals("", missingData.remainingDaysText)
        assertEquals("", missingData.jalaliExpiryDate)
        assertEquals("", missingData.trafficText)
        assertEquals(0f, missingData.progressFraction)

        // Zero totalBytes safeguard (prevents divide by zero)
        val zeroTotalData = ViraxSubscriptionUiData(
            hasUserInfo = true,
            uploadBytes = 500,
            downloadBytes = 500,
            totalBytes = 0,
            expireTimestamp = 0
        )
        assertEquals(0f, zeroTotalData.progressFraction)

        // Used bytes exceeding total bytes (capped at 1.0f)
        val overflowData = ViraxSubscriptionUiData(
            hasUserInfo = true,
            uploadBytes = 6000,
            downloadBytes = 6000,
            totalBytes = 10000,
            expireTimestamp = 0
        )
        assertEquals(1.0f, overflowData.progressFraction)
    }

    @Test
    fun testSubscriptionDefaultsEnable12HourAutoUpdate() {
        val item = SubscriptionItem()
        assertTrue(item.enabled)
        assertTrue(item.autoUpdate)
        assertEquals(720L, item.updateInterval)
    }

    @Test
    fun testMainUiStatePremiumBadgeVisibilityFollowsActiveSubscriptions() {
        val emptyState = MainUiState(groupsLoaded = true)
        assertFalse(emptyState.hasActiveSubscription)
        assertTrue(emptyState.activeSubscriptions.isEmpty())

        val blankDefaultOnly = MainUiState(
            groupsLoaded = true,
            subscriptions = listOf(
                SubscriptionCache("default", SubscriptionItem(remarks = "Default", url = ""))
            )
        )
        assertFalse(blankDefaultOnly.hasActiveSubscription)
        assertTrue(blankDefaultOnly.activeSubscriptions.isEmpty())

        val disabledSub = MainUiState(
            groupsLoaded = true,
            subscriptions = listOf(
                SubscriptionCache("sub1", SubscriptionItem(remarks = "Sub", url = "https://example.com/sub", enabled = false))
            )
        )
        assertFalse(disabledSub.hasActiveSubscription)
        assertEquals(1, disabledSub.activeSubscriptions.size)

        val activeSub = MainUiState(
            groupsLoaded = true,
            subscriptions = listOf(
                SubscriptionCache("sub1", SubscriptionItem(remarks = "Sub", url = "https://example.com/sub", enabled = true))
            )
        )
        assertTrue(activeSub.hasActiveSubscription)
    }

    @Test
    fun testCreateUiDataFromSubscription() {
        val fromNull = ViraxConnectionManager.createUiDataFromSubscription(null)
        assertFalse(fromNull.hasUserInfo)

        val noInfoSub = SubscriptionItem(remarks = "Sub", url = "https://example.com")
        assertFalse(ViraxConnectionManager.createUiDataFromSubscription(noInfoSub).hasUserInfo)

        val validSub = SubscriptionItem(
            remarks = "Sub",
            url = "https://example.com",
            usageInfo = SubscriptionUsageInfo(upload = 100, download = 400, total = 1000, expire = 1900000000L)
        )
        val uiData = ViraxConnectionManager.createUiDataFromSubscription(validSub)
        assertTrue(uiData.hasUserInfo)
        assertEquals(500L, uiData.usedBytes)
        assertEquals(0.5f, uiData.progressFraction)
    }

    @Test
    fun testResolveConnectionTargetFallbackOrder() {
        val untestedServers = listOf(
            ViraxServerItem("s1", "First", 0L, false),
            ViraxServerItem("s2", "Second", -1L, false)
        )
        assertEquals("s2", ViraxConnectionManager.resolveConnectionTarget(untestedServers, "s2")?.guid)
        assertEquals("s1", ViraxConnectionManager.resolveConnectionTarget(untestedServers, "missing")?.guid)

        val mixedServers = listOf(
            ViraxServerItem("s1", "Selected Slow", 200L, true),
            ViraxServerItem("s2", "Fastest", 35L, false)
        )
        assertEquals("s2", ViraxConnectionManager.resolveConnectionTarget(mixedServers, "s1")?.guid)
        assertNull(ViraxConnectionManager.resolveConnectionTarget(emptyList(), null))
    }

    @Test
    fun testResolveExplicitOrFirstServerGuid() {
        assertNull(ViraxConnectionManager.resolveExplicitOrFirstServerGuid(emptyList(), "s1"))

        val servers = listOf(
            ViraxServerItem("s1", "First", 100L, false),
            ViraxServerItem("s2", "Second", 200L, false)
        )
        assertEquals("s2", ViraxConnectionManager.resolveExplicitOrFirstServerGuid(servers, "s2"))
        assertEquals("s1", ViraxConnectionManager.resolveExplicitOrFirstServerGuid(servers, null))
        assertEquals("s1", ViraxConnectionManager.resolveExplicitOrFirstServerGuid(servers, ""))
        assertEquals("s1", ViraxConnectionManager.resolveExplicitOrFirstServerGuid(servers, "other-group-guid"))
    }

    @Test
    fun testFindFastestServerGuidFromCache() {
        val caches = listOf(
            ServersCache("g1", ProfileItem(configType = EConfigType.VLESS, remarks = "Failed"), testDelayMillis = -1L),
            ServersCache("g2", ProfileItem(configType = EConfigType.VLESS, remarks = "Untested"), testDelayMillis = 0L),
            ServersCache("g3", ProfileItem(configType = EConfigType.VLESS, remarks = "Slow"), testDelayMillis = 280L),
            ServersCache("g4", ProfileItem(configType = EConfigType.VLESS, remarks = "Best"), testDelayMillis = 42L)
        )
        assertEquals("g4", ViraxConnectionManager.findFastestServerGuid(caches))

        val noHealthy = listOf(
            ServersCache("g1", ProfileItem(configType = EConfigType.VLESS, remarks = "Failed"), testDelayMillis = -1L),
            ServersCache("g2", ProfileItem(configType = EConfigType.VLESS, remarks = "Zero"), testDelayMillis = 0L)
        )
        assertNull(ViraxConnectionManager.findFastestServerGuid(noHealthy))
        assertNull(ViraxConnectionManager.findFastestServerGuid(emptyList()))
    }

    @Test
    fun testDurationAndTrafficVolumeFormatting() {
        assertEquals("00:00:00", ViraxConnectionManager.formatDurationSeconds(-5L))
        assertEquals("00:00:00", ViraxConnectionManager.formatDurationSeconds(0L))
        assertEquals("00:01:05", ViraxConnectionManager.formatDurationSeconds(65L))
        assertEquals("01:01:01", ViraxConnectionManager.formatDurationSeconds(3661L))

        assertEquals("0 B", ViraxConnectionManager.formatTrafficVolume(-10L))
        assertEquals("0 B", ViraxConnectionManager.formatTrafficVolume(0L))
        assertEquals("512 B", ViraxConnectionManager.formatTrafficVolume(512L))
        assertEquals("1.0 KB", ViraxConnectionManager.formatTrafficVolume(1024L))
        assertEquals("12.5 MB", ViraxConnectionManager.formatTrafficVolume((12.5 * 1024.0 * 1024.0).toLong()))
        assertEquals("3.8 MB", ViraxConnectionManager.formatTrafficVolume((3.8 * 1024.0 * 1024.0).toLong()))
    }

    @Test
    fun testComputeSessionTrafficSnapshot() {
        val baseSession = ViraxConnectionSession(
            connectedElapsedMs = 1_000_000L,
            baseRxBytes = 5_000_000L,
            baseTxBytes = 1_000_000L
        )

        // Disconnected returns zeros regardless of stored session
        val disconnected = ViraxConnectionManager.computeSessionTrafficSnapshot(
            isRunning = false,
            session = baseSession,
            currentElapsedMs = 1_754_000L,
            currentRxBytes = 18_000_000L,
            currentTxBytes = 5_000_000L
        )
        assertEquals("00:00:00", disconnected.durationText)
        assertEquals("0 B", disconnected.downloadText)
        assertEquals("0 B", disconnected.uploadText)
        assertEquals(0L, disconnected.downloadedBytes)
        assertEquals(0L, disconnected.uploadedBytes)

        // Connected calculates exact elapsed time and cumulative consumed RX/TX deltas
        val dlDelta = (12.5 * 1024.0 * 1024.0).toLong()
        val ulDelta = (3.8 * 1024.0 * 1024.0).toLong()
        val connected = ViraxConnectionManager.computeSessionTrafficSnapshot(
            isRunning = true,
            session = baseSession,
            currentElapsedMs = baseSession.connectedElapsedMs + 754_000L,
            currentRxBytes = baseSession.baseRxBytes + dlDelta,
            currentTxBytes = baseSession.baseTxBytes + ulDelta
        )
        assertEquals(754L, connected.elapsedSeconds)
        assertEquals("00:12:34", connected.durationText)
        assertEquals(dlDelta, connected.downloadedBytes)
        assertEquals(ulDelta, connected.uploadedBytes)
        assertEquals("12.5 MB", connected.downloadText)
        assertEquals("3.8 MB", connected.uploadText)

        // Unsupported (-1) TrafficStats safely clamps traffic volumes to 0 without failing timer
        val unsupportedTraffic = ViraxConnectionManager.computeSessionTrafficSnapshot(
            isRunning = true,
            session = ViraxConnectionSession(10_000L, -1L, -1L),
            currentElapsedMs = 75_000L,
            currentRxBytes = -1L,
            currentTxBytes = -1L
        )
        assertEquals("00:01:05", unsupportedTraffic.durationText)
        assertEquals("0 B", unsupportedTraffic.downloadText)
        assertEquals("0 B", unsupportedTraffic.uploadText)

        // Initial MainUiState session defaults
        val initial = MainUiState()
        assertFalse(initial.isSmartConnecting)
        assertEquals("00:00:00", initial.sessionDurationText)
        assertEquals("0 B", initial.sessionDownloadText)
        assertEquals("0 B", initial.sessionUploadText)
        assertEquals(ViraxPlanBadge.NONE, initial.planBadge)
    }

    @Test
    fun testMainUiStatePlanBadgeVariants() {
        val manualOnly = MainUiState(hasAnyServer = true)
        assertEquals(ViraxPlanBadge.CUSTOM, manualOnly.planBadge)

        val customSub = MainUiState(
            hasAnyServer = true,
            subscriptions = listOf(
                SubscriptionCache("sub1", SubscriptionItem(remarks = "Custom", url = "https://example.com/sub", enabled = true))
            )
        )
        assertEquals(ViraxPlanBadge.CUSTOM, customSub.planBadge)

        val dedicatedSub = MainUiState(
            hasAnyServer = true,
            subscriptions = listOf(
                SubscriptionCache(
                    "sub2",
                    SubscriptionItem(
                        remarks = "Dedicated",
                        url = ViraxAccessManager.buildProtectedSubscriptionUrl("hdeiejjeodkd")!!,
                        enabled = true
                    )
                )
            )
        )
        assertEquals(ViraxPlanBadge.PREMIUM, dedicatedSub.planBadge)
    }

    @Test
    fun testFindFastestServerGuidExcludingForAutoConnectFallback() {
        val caches = listOf(
            ServersCache("g1", ProfileItem(configType = EConfigType.VMESS), testDelayMillis = 25L),
            ServersCache("g2", ProfileItem(configType = EConfigType.VMESS), testDelayMillis = 60L),
            ServersCache("g3", ProfileItem(configType = EConfigType.VMESS), testDelayMillis = -1L),
            ServersCache("g4", ProfileItem(configType = EConfigType.VMESS), testDelayMillis = 90L),
        )
        assertEquals("g1", ViraxConnectionManager.findFastestServerGuidExcluding(caches, emptySet()))
        assertEquals("g2", ViraxConnectionManager.findFastestServerGuidExcluding(caches, setOf("g1")))
        assertEquals("g4", ViraxConnectionManager.findFastestServerGuidExcluding(caches, setOf("g1", "g2")))
        assertNull(ViraxConnectionManager.findFastestServerGuidExcluding(caches, setOf("g1", "g2", "g4")))
    }

    @Test
    fun testSpeedtestManagerRemoteEndpointParsingFallbacks() {
        val ipSb = SpeedtestManager.parseRemoteEndpointInfo("""{"ip":"1.2.3.4","country_code":"DE"}""")
        assertEquals("1.2.3.4", ipSb?.ipAddress)
        assertEquals("DE", ipSb?.country)

        val ipWhoIs = SpeedtestManager.parseRemoteEndpointInfo("""{"ip":"5.6.7.8","country_code":"NL","success":true}""")
        assertEquals("5.6.7.8", ipWhoIs?.ipAddress)
        assertEquals("NL", ipWhoIs?.country)

        val ipInfoIo = SpeedtestManager.parseRemoteEndpointInfo("""{"ip":"9.9.9.9","country":"FR"}""")
        assertEquals("9.9.9.9", ipInfoIo?.ipAddress)
        assertEquals("FR", ipInfoIo?.country)

        assertNull(SpeedtestManager.parseRemoteEndpointInfo(""))
        assertNull(SpeedtestManager.parseRemoteEndpointInfo("{}"))
    }
}
