package com.v2ray.ang.ui.virax

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServerAffiliationInfo
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.extension.DIVISOR
import com.v2ray.ang.extension.THRESHOLD
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class ViraxConnectionSession(
    val connectedElapsedMs: Long,
    val baseRxBytes: Long,
    val baseTxBytes: Long
)

data class ViraxSessionTrafficSnapshot(
    val durationText: String,
    val downloadText: String,
    val uploadText: String,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val elapsedSeconds: Long
)

object ViraxConnectionManager {

    private val isConnecting = AtomicBoolean(false)
    private val isUpdatingSubscription = AtomicBoolean(false)

    /**
     * Finds the fastest healthy server among the provided servers.
     * Criteria:
     * - `testDelayMillis > 0` (ping succeeded)
     * - Sorted in ascending order (lowest latency first)
     *
     * @return The best server item, or null if no healthy server exists.
     */
    fun findFastestHealthyServer(servers: List<ViraxServerItem>): ViraxServerItem? {
        return servers
            .filter { it.pingMillis > 0 }
            .minByOrNull { it.pingMillis }
    }

    /**
     * Resolves the best target server to connect to: prefers the lowest-ping healthy server,
     * falling back to the currently selected server or the first available server.
     */
    fun resolveConnectionTarget(
        servers: List<ViraxServerItem>,
        selectedGuid: String?
    ): ViraxServerItem? {
        return findFastestHealthyServer(servers)
            ?: servers.firstOrNull { !selectedGuid.isNullOrBlank() && it.guid == selectedGuid }
            ?: servers.firstOrNull()
    }

    /**
     * Resolves the explicitly selected server GUID when present in [servers],
     * otherwise selects the first available server GUID in the list.
     */
    fun resolveExplicitOrFirstServerGuid(
        servers: List<ViraxServerItem>,
        selectedGuid: String?
    ): String? {
        if (servers.isEmpty()) return null
        return servers.firstOrNull { !selectedGuid.isNullOrBlank() && it.guid == selectedGuid }?.guid
            ?: servers.first().guid
    }

    /**
     * Returns the GUID of the server with the lowest positive ping delay, or null if none succeeded.
     */
    fun findFastestServerGuid(servers: List<ServersCache>): String? {
        return findFastestServerGuidExcluding(servers, emptySet())
    }

    /**
     * Returns the GUID of the lowest-ping healthy server not in [excludedGuids].
     */
    fun findFastestServerGuidExcluding(
        servers: List<ServersCache>,
        excludedGuids: Set<String>
    ): String? {
        return servers
            .filter { it.testDelayMillis > 0L && it.guid !in excludedGuids }
            .minByOrNull { it.testDelayMillis }
            ?.guid
    }

    fun formatDurationSeconds(elapsedSeconds: Long): String {
        val safeSeconds = elapsedSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600L
        val minutes = (safeSeconds % 3600L) / 60L
        val seconds = safeSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTrafficVolume(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < THRESHOLD) return "$bytes B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= THRESHOLD && unitIndex < units.size - 1) {
            size /= DIVISOR
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    fun computeSessionTrafficSnapshot(
        isRunning: Boolean,
        session: ViraxConnectionSession?,
        currentElapsedMs: Long,
        currentRxBytes: Long,
        currentTxBytes: Long
    ): ViraxSessionTrafficSnapshot {
        if (!isRunning || session == null || session.connectedElapsedMs <= 0L) {
            return ViraxSessionTrafficSnapshot(
                durationText = "00:00:00",
                downloadText = "0 B",
                uploadText = "0 B",
                downloadedBytes = 0L,
                uploadedBytes = 0L,
                elapsedSeconds = 0L
            )
        }
        val elapsedSec = ((currentElapsedMs - session.connectedElapsedMs) / 1000L).coerceAtLeast(0L)
        val downloaded = if (session.baseRxBytes >= 0L && currentRxBytes >= 0L) {
            (currentRxBytes - session.baseRxBytes).coerceAtLeast(0L)
        } else {
            0L
        }
        val uploaded = if (session.baseTxBytes >= 0L && currentTxBytes >= 0L) {
            (currentTxBytes - session.baseTxBytes).coerceAtLeast(0L)
        } else {
            0L
        }
        return ViraxSessionTrafficSnapshot(
            durationText = formatDurationSeconds(elapsedSec),
            downloadText = formatTrafficVolume(downloaded),
            uploadText = formatTrafficVolume(uploaded),
            downloadedBytes = downloaded,
            uploadedBytes = uploaded,
            elapsedSeconds = elapsedSec
        )
    }

    /**
     * Auto-Connect execution logic:
     * 1. Evaluates all valid servers
     * 2. Filters out failed servers
     * 3. Selects the fastest healthy server
     * 4. Establishes the connection
     *
     * @return The selected server GUID, or null if no healthy server was available.
     */
    fun executeAutoConnect(
        context: Context,
        servers: List<ViraxServerItem>,
        onNoHealthyServer: () -> Unit = {}
    ): String? {
        if (!isConnecting.compareAndSet(false, true)) {
            LogUtil.w(AppConfig.TAG, "Connection attempt already in progress, ignoring duplicate trigger")
            return null
        }

        try {
            val fastest = findFastestHealthyServer(servers)
            if (fastest == null) {
                onNoHealthyServer()
                return null
            }

            // Persist the selected server GUID
            MmkvManager.setSelectServer(fastest.guid)

            // Start VPN core
            LauncherManager.startService(context)
            return fastest.guid
        } finally {
            isConnecting.set(false)
        }
    }

    /**
     * Fallback to the next fastest server if the current connection attempt fails.
     */
    fun executeFallbackNextServer(
        context: Context,
        servers: List<ViraxServerItem>,
        failedGuid: String
    ): String? {
        val remaining = servers.filter { it.guid != failedGuid && it.pingMillis > 0 }
        val nextBest = findFastestHealthyServer(remaining) ?: return null

        MmkvManager.setSelectServer(nextBest.guid)
        LauncherManager.startService(context)
        return nextBest.guid
    }

    /**
     * Extracts subscription usage info from MMKV without logging sensitive URLs.
     */
    fun getSubscriptionUsage(subId: String?): ViraxSubscriptionUiData {
        if (subId.isNullOrBlank()) {
            val firstSub = MmkvManager.decodeSubscriptions().firstOrNull()
            if (firstSub == null) {
                return ViraxSubscriptionUiData(
                    hasUserInfo = false,
                    uploadBytes = 0,
                    downloadBytes = 0,
                    totalBytes = 0,
                    expireTimestamp = 0
                )
            }
            return createUiDataFromItem(firstSub.subscription)
        }

        val subItem = MmkvManager.decodeSubscription(subId)
        if (subItem == null) {
            return ViraxSubscriptionUiData(
                hasUserInfo = false,
                uploadBytes = 0,
                downloadBytes = 0,
                totalBytes = 0,
                expireTimestamp = 0
            )
        }

        return createUiDataFromSubscription(subItem)
    }

    fun createUiDataFromSubscription(subItem: SubscriptionItem?): ViraxSubscriptionUiData {
        if (subItem == null) {
            return ViraxSubscriptionUiData(
                hasUserInfo = false,
                uploadBytes = 0,
                downloadBytes = 0,
                totalBytes = 0,
                expireTimestamp = 0
            )
        }
        return createUiDataFromItem(subItem)
    }

    private fun createUiDataFromItem(subItem: SubscriptionItem): ViraxSubscriptionUiData {
        val info = subItem.usageInfo
        if (info == null || (info.upload == 0L && info.download == 0L && info.total == 0L && info.expire == 0L)) {
            return ViraxSubscriptionUiData(
                hasUserInfo = false,
                uploadBytes = 0,
                downloadBytes = 0,
                totalBytes = 0,
                expireTimestamp = 0
            )
        }

        return ViraxSubscriptionUiData(
            hasUserInfo = true,
            uploadBytes = info.upload,
            downloadBytes = info.download,
            totalBytes = info.total,
            expireTimestamp = info.expire
        )
    }

    /**
     * Deduplicates a list of profiles based on their server endpoint and credentials identity.
     */
    fun deduplicateProfiles(profiles: List<Pair<String, ProfileItem>>): List<String> {
        val seen = HashSet<String>()
        val duplicates = mutableListOf<String>()

        for ((guid, profile) in profiles) {
            val endpoint = "${profile.server}:${profile.serverPort}:${profile.configType}"
            if (!seen.add(endpoint)) {
                duplicates.add(guid)
            }
        }
        return duplicates
    }

    /**
     * Safely triggers subscription update without concurrent collisions.
     */
    fun beginSubscriptionUpdate(action: () -> Unit): Boolean {
        if (!isUpdatingSubscription.compareAndSet(false, true)) {
            return false
        }
        try {
            action()
            return true
        } finally {
            isUpdatingSubscription.set(false)
        }
    }
}
