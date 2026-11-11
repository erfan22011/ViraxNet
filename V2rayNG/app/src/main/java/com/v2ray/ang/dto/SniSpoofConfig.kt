package com.v2ray.ang.dto

/**
 * Target scope selector for experimental SNI Spoofing.
 */
enum class SniSpoofScope {
    SINGLE_SERVER,
    MULTI_SERVER,
    SUBSCRIPTION,
    GLOBAL
}

/**
 * Configuration model for SNI Spoofing (Fake SNI + Connect IP + target scope).
 */
data class SniSpoofConfig(
    val enabled: Boolean = false,
    val fakeSni: String = "",
    val connectIp: String = "",
    val scope: SniSpoofScope = SniSpoofScope.GLOBAL,
    val targetServerGuids: Set<String> = emptySet(),
    val targetSubscriptionIds: Set<String> = emptySet(),
)

/**
 * Safe non-secret projection of a server shown in the SNI Spoofing target picker.
 */
data class ViraxSniServerOption(
    val guid: String,
    val remarks: String,
    val subscriptionId: String,
)

/**
 * Safe non-secret projection of a subscription shown in the SNI Spoofing target picker.
 */
data class ViraxSniSubscriptionOption(
    val id: String,
    val remarks: String,
    val isProtected: Boolean = false,
)
