package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class ViraxAccessManagerTest {

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testDecodedBaseUrlMatchesExpectedFingerprint() {
        val baseUrl = ViraxAccessManager.decodeProtectedBaseUrl()
        assertEquals(
            "26ae35586c81d1bbb2dab099720e722d4bf29c97af2209aa456499ca264a9f80",
            sha256Hex(baseUrl)
        )
    }

    @Test
    fun testBuildProtectedSubscriptionUrlFromDedicatedCode() {
        val url = ViraxAccessManager.buildProtectedSubscriptionUrl("  hdeiejjeodkd  ")
        assertEquals(
            "aa860072d9a716d7ca8655e5bbab825be1dc9c51d0ea999d6ac94ee42dbb37cc",
            sha256Hex(url!!)
        )
        assertTrue(ViraxAccessManager.isProtectedUrl(url))
    }

    @Test
    fun testBuildProtectedSubscriptionUrlFromFullProtectedUrlOrLeadingSlash() {
        val base = ViraxAccessManager.decodeProtectedBaseUrl()
        assertEquals("${base}abc123", ViraxAccessManager.buildProtectedSubscriptionUrl("/abc123"))
        assertEquals("${base}abc123", ViraxAccessManager.buildProtectedSubscriptionUrl("${base}abc123#MyLabel"))
    }

    @Test
    fun testBuildProtectedSubscriptionUrlRejectsInvalidInputs() {
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl(""))
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl("   "))
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl("https://example.com/sub/abc"))
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl("abc def"))
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl("abc/def"))
        assertNull(ViraxAccessManager.buildProtectedSubscriptionUrl("abc?token=1"))
    }

    @Test
    fun testPlanBadgeResolution() {
        assertEquals(ViraxPlanBadge.NONE, ViraxAccessManager.resolvePlanBadge(emptyList(), hasAnyServer = false))
        assertEquals(ViraxPlanBadge.CUSTOM, ViraxAccessManager.resolvePlanBadge(emptyList(), hasAnyServer = true))

        val customSub = SubscriptionItem(remarks = "Custom", url = "https://example.com/sub", enabled = true, isProtected = false)
        assertEquals(ViraxPlanBadge.CUSTOM, ViraxAccessManager.resolvePlanBadge(listOf(customSub), hasAnyServer = true))

        val protectedByFlag = SubscriptionItem(remarks = "VIP", url = "https://other.example/sub", enabled = true, isProtected = true)
        assertEquals(ViraxPlanBadge.PREMIUM, ViraxAccessManager.resolvePlanBadge(listOf(customSub, protectedByFlag), hasAnyServer = true))

        val protectedByUrl = SubscriptionItem(
            remarks = "Auto",
            url = ViraxAccessManager.buildProtectedSubscriptionUrl("hdeiejjeodkd")!!,
            enabled = true,
            isProtected = false
        )
        assertEquals(ViraxPlanBadge.PREMIUM, ViraxAccessManager.resolvePlanBadge(listOf(protectedByUrl), hasAnyServer = true))
    }

    @Test
    fun testProtectedServerResolutionAndLogSanitization() {
        val protectedSub = SubscriptionItem(remarks = "VIP", url = "https://example.org", isProtected = true)
        val normalSub = SubscriptionItem(remarks = "Public", url = "https://example.org", isProtected = false)

        val inheritedProfile = ProfileItem(configType = EConfigType.VLESS, subscriptionId = "sub1", isProtected = false)
        val flaggedProfile = ProfileItem(configType = EConfigType.VLESS, subscriptionId = "", isProtected = true)
        val normalProfile = ProfileItem(configType = EConfigType.VLESS, subscriptionId = "sub2", isProtected = false)

        assertTrue(ViraxAccessManager.resolveServerProtected(inheritedProfile, protectedSub))
        assertTrue(ViraxAccessManager.resolveServerProtected(flaggedProfile, null))
        assertFalse(ViraxAccessManager.resolveServerProtected(normalProfile, normalSub))

        val secretUrl = ViraxAccessManager.buildProtectedSubscriptionUrl("secretToken123")!!
        val sanitized = ViraxAccessManager.sanitizeTextForLogs("Failed to fetch $secretUrl from server")
        assertFalse(sanitized.contains("secretToken123"))
        assertFalse(sanitized.contains("tootcloud"))
        assertTrue(sanitized.contains("[REDACTED_SUB]"))
    }
}
