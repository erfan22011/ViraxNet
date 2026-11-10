package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServerAffiliationInfo
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.ui.virax.ViraxConnectionSession
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface MainDataSource : Closeable {
    val mainServiceEvent: Flow<MainServiceEvent>

    fun getSelectedSubscriptionId(): String
    fun setSelectedSubscriptionId(id: String)

    fun getSelectServer(): String?
    fun setSelectServer(guid: String)

    fun getConfirmRemove(): Boolean
    fun getDoubleColumnDisplay(): Boolean
    fun isGroupAllDisplayEnabled(): Boolean

    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String

    fun getSubscriptions(): List<SubscriptionCache>
    fun getAllRawSubscriptions(): List<SubscriptionCache>
    fun getSubscriptionItem(id: String): SubscriptionItem?

    fun getServerGuidList(groupId: String): List<String>
    fun decodeServerConfig(guid: String): ProfileItem?
    fun decodeAffiliationInfo(guid: String): ServerAffiliationInfo?

    fun encodeServerList(guids: List<String>, groupId: String)

    fun removeServer(guid: String)
    fun removeAllServer(): Int
    fun removeInvalidServerByGuid(guid: String): Int
    fun removeInvalidServersInGroup(groupId: String): Int

    fun clearAllTestDelayResults(guids: List<String>)
    fun sortByTestResultsForSub(subId: String)
    fun getSubsList(): List<String>

    suspend fun importBatchConfig(
        server: String?,
        subscriptionId: String,
        updateUI: Boolean
    ): Pair<Int, Int>

    fun importOrUpdateSubscriptionUrl(
        url: String,
        allowInsecure: Boolean = true
    ): Pair<String?, SubscriptionUpdateResult>

    fun updateSubscriptionItem(subId: String, item: SubscriptionItem)
    fun deleteSubscription(subId: String)

    fun updateConfigViaSubAll(): SubscriptionUpdateResult
    fun updateConfigViaSub(subscriptionCache: SubscriptionCache): SubscriptionUpdateResult

    fun isValidUrl(value: String?): Boolean
    fun getSettingsBool(key: String, default: Boolean): Boolean
    fun setSettingsBool(key: String, value: Boolean)
    fun getSettingsString(key: String, default: String): String
    fun setSettingsString(key: String, value: String)
    fun getLocalDnsEnabled(): Boolean
    fun getFakeDnsEnabled(): Boolean
    fun getDelayTestUrl(): String
    fun getLanguageCode(): String
    fun setLanguageCode(code: String)

    fun shareNonCustomConfigsToClipboard(guids: List<String>): Int
    fun share2QRCode(guid: String): android.graphics.Bitmap?
    fun share2Clipboard(guid: String): Boolean

    fun sendMsg2Service(msgId: Int, content: String)
    fun sendMsg2TestService(msg: TestServiceMessage, requestId: String? = null)
    fun cancelAllPing()
    fun testCurrentServerRealPing(requestId: String)

    fun syncSubscriptions()
    fun initAssets()

    fun getConnectionSession(): ViraxConnectionSession?
    fun getElapsedRealtimeMs(): Long
    fun getAppRxBytes(): Long
    fun getAppTxBytes(): Long
}
