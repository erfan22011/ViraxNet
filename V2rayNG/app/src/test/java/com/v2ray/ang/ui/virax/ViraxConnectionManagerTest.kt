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
}
