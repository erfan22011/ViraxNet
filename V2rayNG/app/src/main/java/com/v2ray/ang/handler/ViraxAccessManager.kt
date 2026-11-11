package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionItem

enum class ViraxPlanBadge {
    NONE,
    PREMIUM,
    CUSTOM
}

/**
 * Manages dedicated protected subscription access, plan badge resolution,
 * and log/UI redaction for protected configurations.
 */
object ViraxAccessManager {

    const val PROTECTED_SUB_DEFAULT_REMARKS = "ViraxNet Premium"

    private val BASE_URL_KEY = intArrayOf(0x5A, 0xA3, 0x3C, 0x91, 0x7E, 0xC5, 0x2F, 0x84)
    private val BASE_URL_ENC = intArrayOf(
        50, 198, 106, 210, 73, 170, 102, 220, 161, 88, 184, 94, 221, 119, 181, 24,
        38, 237, 123, 182, 4, 211, 49, 108, 178, 48, 180, 106, 155, 30, 254, 248,
        15, 240, 81
    )

    private val cachedBaseUrl: String by lazy {
        buildString(BASE_URL_ENC.size) {
            for (i in BASE_URL_ENC.indices) {
                val decoded = BASE_URL_ENC[i] xor BASE_URL_KEY[i % BASE_URL_KEY.size] xor ((i * 17) and 0xFF)
                append(decoded.toChar())
            }
        }
    }

    private val cachedHostWithPort: String by lazy {
        cachedBaseUrl.substringAfter("://").substringBefore("/")
    }

    private val cachedHost: String by lazy {
        cachedHostWithPort.substringBefore(":")
    }

    fun decodeProtectedBaseUrl(): String = cachedBaseUrl

    fun normalizeDedicatedCode(rawInput: String?): String? {
        val trimmed = rawInput?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val baseUrl = decodeProtectedBaseUrl()
        val candidate = if (trimmed.startsWith(baseUrl, ignoreCase = true)) {
            trimmed.substring(baseUrl.length).substringBefore('#').trim()
        } else {
            trimmed.trimStart('/')
        }

        if (candidate.isEmpty() ||
            candidate.any { it.isWhitespace() || it.isISOControl() } ||
            candidate.contains('/') ||
            candidate.contains('\\') ||
            candidate.contains('?') ||
            candidate.contains('#') ||
            candidate.contains("://")
        ) {
            return null
        }
        return candidate
    }

    fun buildProtectedSubscriptionUrl(codeInput: String?): String? {
        val code = normalizeDedicatedCode(codeInput) ?: return null
        return decodeProtectedBaseUrl() + code
    }

    fun isProtectedUrl(url: String?): Boolean {
        val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        if (trimmed.startsWith(decodeProtectedBaseUrl(), ignoreCase = true)) {
            return true
        }
        val host = extractHost(trimmed) ?: return false
        return host.equals(cachedHost, ignoreCase = true)
    }

    fun isProtectedSubscription(item: SubscriptionItem?): Boolean {
        return item != null && (item.isProtected || isProtectedUrl(item.url))
    }

    fun isProtectedSubscriptionId(
        subId: String?,
        subscriptionLookup: (String) -> SubscriptionItem? = { MmkvManager.decodeSubscription(it) }
    ): Boolean {
        val cleanId = subId?.takeIf { it.isNotBlank() } ?: return false
        return isProtectedSubscription(subscriptionLookup(cleanId))
    }

    fun resolveServerProtected(profile: ProfileItem?, subscription: SubscriptionItem?): Boolean {
        return profile?.isProtected == true || isProtectedSubscription(subscription)
    }

    fun isProtectedServer(
        guid: String?,
        profile: ProfileItem? = null,
        profileLookup: (String) -> ProfileItem? = { MmkvManager.decodeServerConfig(it) },
        subscriptionLookup: (String) -> SubscriptionItem? = { MmkvManager.decodeSubscription(it) }
    ): Boolean {
        val resolvedProfile = profile ?: guid?.takeIf { it.isNotBlank() }?.let(profileLookup)
        if (resolvedProfile?.isProtected == true) return true
        val subId = resolvedProfile?.subscriptionId?.takeIf { it.isNotBlank() } ?: return false
        return isProtectedSubscription(subscriptionLookup(subId))
    }

    fun resolvePlanBadge(subscriptions: List<SubscriptionItem>, hasAnyServer: Boolean): ViraxPlanBadge {
        if (subscriptions.isEmpty() && !hasAnyServer) {
            return ViraxPlanBadge.NONE
        }
        val relevant = subscriptions.filter { it.enabled }.ifEmpty { subscriptions }
        return if (relevant.any { isProtectedSubscription(it) }) {
            ViraxPlanBadge.PREMIUM
        } else {
            ViraxPlanBadge.CUSTOM
        }
    }

    fun sanitizeTextForLogs(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val escapedHost = Regex.escape(cachedHost)
        val urlRegex = Regex("https?://[^\\s\"']*?$escapedHost[^\\s\"']*", RegexOption.IGNORE_CASE)
        var sanitized = urlRegex.replace(text, "[REDACTED_SUB]")
        if (sanitized.contains(cachedHostWithPort, ignoreCase = true)) {
            sanitized = sanitized.replace(cachedHostWithPort, "[REDACTED_HOST]", ignoreCase = true)
        }
        if (sanitized.contains(cachedHost, ignoreCase = true)) {
            sanitized = sanitized.replace(cachedHost, "[REDACTED_HOST]", ignoreCase = true)
        }
        return sanitized
    }

    private fun extractHost(url: String): String? {
        val afterScheme = if (url.contains("://")) {
            url.substringAfter("://")
        } else {
            return null
        }
        val authority = afterScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
        if (authority.isEmpty()) return null
        return if (authority.startsWith("[")) {
            authority.substringBefore(']').removePrefix("[")
        } else {
            authority.substringBefore(':')
        }
    }
}
