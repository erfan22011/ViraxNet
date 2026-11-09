package com.v2ray.ang.ui.virax

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
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
}
