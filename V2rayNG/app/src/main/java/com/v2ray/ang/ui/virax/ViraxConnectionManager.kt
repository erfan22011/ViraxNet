package com.v2ray.ang.ui.virax

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServerAffiliationInfo
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.atomic.AtomicBoolean

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
            ?: servers.firstOrNull { it.guid == selectedGuid }
            ?: servers.firstOrNull()
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
