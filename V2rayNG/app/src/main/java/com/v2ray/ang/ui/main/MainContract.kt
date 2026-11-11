package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.ViraxSniServerOption
import com.v2ray.ang.handler.ViraxAccessManager
import com.v2ray.ang.handler.ViraxPlanBadge

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val groupsLoaded: Boolean = false,
    val subscriptions: List<com.v2ray.ang.dto.entities.SubscriptionCache> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null,
    val localDnsEnabled: Boolean = false,
    val fakeDnsEnabled: Boolean = false,
    val bypassIranianSites: Boolean = false,
    val sniSpoofConfig: SniSpoofConfig = SniSpoofConfig(),
    val sniServerOptions: List<ViraxSniServerOption> = emptyList(),
    val delayTestUrl: String = com.v2ray.ang.AppConfig.DELAY_TEST_URL,
    val languageCode: String = "auto",
    val startFailureCounter: Long = 0L,
    val isSmartConnecting: Boolean = false,
    val isAutoPinging: Boolean = false,
    val hasAnyServer: Boolean = false,
    val sessionDurationText: String = "00:00:00",
    val sessionDownloadText: String = "0 B",
    val sessionUploadText: String = "0 B",
) {
    val activeSubscriptions: List<com.v2ray.ang.dto.entities.SubscriptionCache>
        get() = subscriptions.filter { it.subscription.url.isNotBlank() }

    val hasActiveSubscription: Boolean
        get() = activeSubscriptions.any { it.subscription.enabled }

    val planBadge: ViraxPlanBadge
        get() = ViraxAccessManager.resolvePlanBadge(
            subscriptions = activeSubscriptions.map { it.subscription },
            hasAnyServer = hasAnyServer
        )
}

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data class UpdateGroupSubscription(val groupId: String) : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class SelectServerOnly(val guid: String) : MainAction
    data class ConnectServer(val guid: String) : MainAction
    data object SmartConnect : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: com.v2ray.ang.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction
    data class OpenSubscriptionLink(val url: String) : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction
    data class ImportSubscriptionUrl(val url: String) : MainAction
    data class ImportDedicatedCode(val code: String) : MainAction
    data class TestSingleServer(val guid: String) : MainAction
    data class ToggleSubscriptionEnabled(val subId: String, val enabled: Boolean) : MainAction
    data class ToggleSubscriptionAutoUpdate(val subId: String, val autoUpdate: Boolean) : MainAction
    data class DeleteSubscription(val subId: String) : MainAction
    data class SetLocalDnsEnabled(val enabled: Boolean) : MainAction
    data class SetFakeDnsEnabled(val enabled: Boolean) : MainAction
    data class SetBypassIranianSites(val enabled: Boolean) : MainAction
    data class UpdateSniSpoofingConfig(val config: SniSpoofConfig) : MainAction
    data class SetDelayTestUrl(val url: String) : MainAction
    data class SetLanguageCode(val code: String) : MainAction

    data object LocateHandled : MainAction
}

/** One-shot side effects emitted from [MainViewModel] to [MainActivity]. */
sealed interface MainUiEffect {
    data object StopForSmartConnect : MainUiEffect
    data object ConnectOrRestart : MainUiEffect
}
