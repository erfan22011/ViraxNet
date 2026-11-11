package com.v2ray.ang.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.RealPingResult
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.extension.delay
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.matchesPattern
import com.v2ray.ang.extension.moveItem
import com.v2ray.ang.handler.SubscriptionInfoParser
import com.v2ray.ang.handler.ViraxAccessManager
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.ui.virax.ViraxConnectionManager
import com.v2ray.ang.ui.virax.ViraxConnectionSession
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.PatternSyntaxException

private fun applyTestDelayResults(
    servers: List<ServersCache>,
    updates: Map<String, Long>,
): List<ServersCache> = servers.map { server ->
    val delayMillis = updates[server.guid]
    if (delayMillis == null || delayMillis == server.testDelayMillis) {
        server
    } else {
        server.copy(testDelayMillis = delayMillis)
    }
}

private fun applyTestDelayResultsToRows(
    rows: List<ServerRowUiModel>,
    updates: Map<String, Long>,
): List<ServerRowUiModel> = rows.map { row ->
    val delayMillis = updates[row.guid]
    if (delayMillis == null || delayMillis == row.testDelayMillis) {
        row
    } else {
        row.copy(testDelayMillis = delayMillis)
    }
}

class MainViewModel(
    application: Application,
    private val dataSource: MainDataSource
) : BaseViewModel(application) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val preloadDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    // ---------- UI state ----------
    private val _uiState = MutableStateFlow(
        MainUiState(
            subscriptions = dataSource.getAllRawSubscriptions(),
            selectedGroupId = dataSource.getSelectedSubscriptionId(),
            selectedGuid = dataSource.getSelectServer(),
            confirmRemove = dataSource.getConfirmRemove(),
            doubleColumnDisplay = dataSource.getDoubleColumnDisplay(),
            localDnsEnabled = dataSource.getLocalDnsEnabled(),
            fakeDnsEnabled = dataSource.getFakeDnsEnabled(),
            bypassIranianSites = dataSource.getBypassIranianSites(),
            hasAnyServer = dataSource.hasAnyServer(),
            delayTestUrl = dataSource.getDelayTestUrl(),
            languageCode = dataSource.getLanguageCode(),
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ---------- Keyword filtering ----------
    @Volatile
    private var keywordFilter: String = ""
    private var filterJob: Job? = null

    // ---------- Groups & cache ----------
    private val cacheMutex = Mutex()
    private val groupDataCache = mutableMapOf<String, List<ServersCache>>()
    private val groupUiFlows = ConcurrentHashMap<String, MutableStateFlow<ServerGroupUiState>>()
    private val groupServerFlows = ConcurrentHashMap<String, StateFlow<List<ServersCache>>>()
    private val groupLoadMutexes = ConcurrentHashMap<String, Mutex>()
    private val serverOrderPersistenceJobs = mutableMapOf<String, Job>()

    private var setupGroupJob: Job? = null
    private var preloadJob: Job? = null
    private var selectedGroupLoadJob: Job? = null
    private var reloadJob: Job? = null
    private var testResultFlushJob: Job? = null
    private val pendingTestResults = linkedMapOf<String, Long>()

    private val testRequests = MainTestRequests()
    private var bulkTestJob: Job? = null
    private var pendingSmartConnectRequestId: String? = null
    private var smartConnectPreStopJob: Job? = null
    private var smartConnectFallbackActive: Boolean = false
    private val smartConnectAttemptedGuids = mutableSetOf<String>()
    private val autoPingedGroups = ConcurrentHashMap<String, Long>()
    private val singleServerPingRequestIds = ConcurrentHashMap.newKeySet<String>()
    private var sessionMetricsJob: Job? = null
    private var fallbackConnectionSession: ViraxConnectionSession? = null

    private val _uiEffects = Channel<MainUiEffect>(Channel.BUFFERED)
    val uiEffects: Flow<MainUiEffect> = _uiEffects.receiveAsFlow()

    private val initialPageReady = CompletableDeferred<Unit>()

    // ---------- Service events ----------
    init {
        collectServiceEvents()
        setupGroupTab()
    }

    private fun collectServiceEvents() {
        viewModelScope.launch {
            dataSource.mainServiceEvent.collect { event ->
                handleServiceEvent(event)
            }
        }
    }

    private fun handleServiceEvent(event: MainServiceEvent) {
        when (event) {
            MainServiceEvent.StateRunning -> updateRunningState(true, clearTestingText = false)
            MainServiceEvent.StateNotRunning -> updateRunningState(false, clearTestingText = false)
            MainServiceEvent.StateStartSuccess -> {
                smartConnectFallbackActive = false
                smartConnectAttemptedGuids.clear()
                _uiState.update { it.copy(isSmartConnecting = false) }
                toastSuccess(R.string.toast_services_success)
                updateRunningState(true)
            }

            is MainServiceEvent.StateStartFailure -> {
                if (smartConnectFallbackActive && smartConnectAttemptedGuids.size < MAX_SMART_CONNECT_ATTEMPTS) {
                    val nextGuid = ViraxConnectionManager.findFastestServerGuidExcluding(
                        currentServers(),
                        smartConnectAttemptedGuids
                    )
                    if (nextGuid != null) {
                        smartConnectAttemptedGuids.add(nextGuid)
                        updateSelectedGuid(nextGuid)
                        _uiEffects.trySend(MainUiEffect.ConnectOrRestart)
                        return
                    }
                }
                smartConnectFallbackActive = false
                smartConnectAttemptedGuids.clear()
                _uiState.update { it.copy(isSmartConnecting = false) }
                if (!event.message.isNullOrBlank()) {
                    toastError(event.message)
                } else {
                    toastError(R.string.toast_services_failure)
                }
                updateRunningState(false)
                notifyStartFailure()
            }

            MainServiceEvent.StateStopSuccess -> updateRunningState(false)
            is MainServiceEvent.MeasureDelayResult -> {
                if (!uiState.value.isRunning || !testRequests.completeCurrent(event.requestId)) return
                _uiState.update { it.copy(isTesting = testRequests.isTesting, status = MainStatus.ConnectionTest(event.result)) }
            }

            is MainServiceEvent.MeasureConfigSuccess -> {
                val request = testRequests.bulk?.takeIf { it.id == event.requestId } ?: return
                queueTestResult(event.result, request)
            }

            is MainServiceEvent.MeasureConfigNotify -> {
                if (event.requestId == testRequests.bulk?.id) {
                    _uiState.update { it.copy(status = MainStatus.TestProgress(event.progress)) }
                }
            }

            is MainServiceEvent.MeasureConfigFinish -> {
                val request = testRequests.bulk?.takeIf { it.id == event.requestId } ?: return
                val scheduledFlush = testResultFlushJob
                testResultFlushJob = viewModelScope.launch {
                    scheduledFlush?.cancelAndJoin()
                    if (testRequests.bulk?.id != request.id) return@launch
                    flushPendingTestResults(request)
                    val isSingleServerPing = singleServerPingRequestIds.remove(request.id)
                    if (!isSingleServerPing) {
                        autoPingedGroups[request.groupId] = dataSource.getElapsedRealtimeMs()
                    }
                    val isSmartTarget = (pendingSmartConnectRequestId == request.id)
                    if (isSmartTarget) {
                        pendingSmartConnectRequestId = null
                    }
                    val testedServers = mutableServerGroupState(request.groupId).value.servers
                    onTestsFinished(request.id)
                    if (isSmartTarget) {
                        completeSmartConnect(testedServers)
                    }
                }
            }

            is MainServiceEvent.MeasureDelayCancelled -> {
                if (testRequests.completeCurrent(event.requestId)) resetTestStatus()
            }

            is MainServiceEvent.MeasureConfigCancelled -> {
                singleServerPingRequestIds.remove(event.requestId)
                if (pendingSmartConnectRequestId == event.requestId) {
                    pendingSmartConnectRequestId = null
                    smartConnectFallbackActive = false
                    smartConnectAttemptedGuids.clear()
                    _uiState.update { it.copy(isSmartConnecting = false) }
                    notifyStartFailure()
                }
                if (testRequests.completeBulk(event.requestId) != null) {
                    cancelPendingTestResults()
                    resetTestStatus()
                }
            }
        }
    }

    private fun queueTestResult(result: RealPingResult, request: MainTestRequests.Bulk) {
        pendingTestResults[result.guid] = result.delayMillis
        if (testResultFlushJob?.isActive == true) return

        testResultFlushJob = viewModelScope.launch {
            while (pendingTestResults.isNotEmpty()) {
                delay(TEST_RESULT_FLUSH_INTERVAL_MS)
                flushPendingTestResults(request)
            }
        }
    }

    private suspend fun flushPendingTestResults(request: MainTestRequests.Bulk) {
        if (pendingTestResults.isEmpty()) return
        if (testRequests.bulk?.id != request.id) return

        val updates = cacheMutex.withLock {
            val drained = pendingTestResults.toMap()
            pendingTestResults.clear()
            groupDataCache[request.groupId]?.let { cached ->
                groupDataCache[request.groupId] = applyTestDelayResults(cached, drained)
            }
            drained
        }
        if (updates.isEmpty()) return
        if (testRequests.bulk?.id != request.id) return
        mutableServerGroupState(request.groupId).update { current ->
            current.copy(
                servers = applyTestDelayResults(current.servers, updates),
                rows = applyTestDelayResultsToRows(current.rows, updates),
            )
        }
    }

    internal fun formatStatus(status: MainStatus): String = when (status) {
        MainStatus.Disconnected -> dataSource.getString(R.string.connection_not_connected)
        MainStatus.Connected -> dataSource.getString(R.string.connection_connected)
        MainStatus.Testing -> dataSource.getString(R.string.connection_test_testing)
        is MainStatus.TestProgress -> dataSource.getString(
            R.string.connection_running_task_left,
            status.progress
        )

        is MainStatus.ConnectionTest -> formatConnectionTestResult(status.result)
    }

    private fun formatConnectionTestResult(result: ConnectionTestResult): String {
        val status = if (result.delayMillis >= 0) {
            val delay = dataSource.getString(R.string.server_test_delay_value, result.delayMillis)
            dataSource.getString(R.string.connection_test_available, delay)
        } else {
            val detail = result.errorMessage.ifBlank {
                dataSource.getString(R.string.connection_test_empty_message)
            }
            dataSource.getString(R.string.connection_test_error, detail)
        }

        if (result.delayMillis < 0 || (result.country == null && result.ipAddress == null)) {
            return status
        }

        val unknown = dataSource.getString(R.string.value_unknown)
        return "$status\n(${result.country ?: unknown}) ${result.ipAddress ?: unknown}"
    }

    // ---------- Public state accessors ----------
    fun serversForGroup(groupId: String): StateFlow<List<ServersCache>> =
        groupServerFlows.computeIfAbsent(groupId) {
            val groupState = mutableServerGroupState(groupId)
            groupState
                .map { it.servers }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = groupState.value.servers,
                )
        }

    internal fun serverGroupState(groupId: String): StateFlow<ServerGroupUiState> =
        mutableServerGroupState(groupId).asStateFlow()

    private fun mutableServerGroupState(groupId: String): MutableStateFlow<ServerGroupUiState> =
        groupUiFlows.computeIfAbsent(groupId) { MutableStateFlow(ServerGroupUiState()) }

    private fun activeGroupId(): String =
        uiState.value.selectedGroupId.ifEmpty { uiState.value.groups.firstOrNull()?.id.orEmpty() }

    private fun currentServers(): List<ServersCache> =
        mutableServerGroupState(activeGroupId()).value.servers

    // ---------- Action handler ----------
    fun onAction(action: MainAction) {
        when (action) {
            MainAction.Initialize -> initialize()
            MainAction.RefreshGroups -> setupGroupTab(forceRefresh = true)
            MainAction.TestAllServers -> testAllRealPing(true)
            MainAction.TestRealAllServers -> testAllRealPing()
            MainAction.SmartConnect -> smartConnect()
            MainAction.CancelTesting -> cancelAllPing()
            MainAction.RemoveAllServers -> removeAllServerAsync()
            MainAction.RemoveDuplicateServers -> removeDuplicateServerAsync()
            MainAction.RemoveInvalidServers -> removeInvalidServerAsync()
            MainAction.SortByTestResults -> sortByTestResultsAsync()
            MainAction.UpdateSubscriptions -> importConfigViaSub(uiState.value.selectedGroupId)
            is MainAction.UpdateGroupSubscription -> importConfigViaSub(action.groupId)
            MainAction.ExportAll -> exportAllAsync()
            is MainAction.SelectGroup -> subscriptionIdChanged(action.groupId)
            is MainAction.SelectServer -> {
                smartConnectFallbackActive = false
                smartConnectAttemptedGuids.clear()
                updateSelectedGuid(action.guid)
            }
            is MainAction.SelectServerOnly -> {
                smartConnectFallbackActive = false
                smartConnectAttemptedGuids.clear()
                updateSelectedGuid(action.guid)
            }
            is MainAction.RemoveServer -> removeServerAndRefresh(action.guid)
            is MainAction.Search -> filterConfig(action.query)
            is MainAction.ImportBatchConfig -> importBatchConfig(action.configText)
            is MainAction.ImportSubscriptionUrl -> importSubscriptionUrl(action.url)
            is MainAction.ImportDedicatedCode -> importDedicatedCode(action.code)
            is MainAction.TestSingleServer -> testSingleServerRealPing(action.guid)
            is MainAction.ToggleSubscriptionEnabled -> toggleSubscriptionEnabled(action.subId, action.enabled)
            is MainAction.ToggleSubscriptionAutoUpdate -> toggleSubscriptionAutoUpdate(action.subId, action.autoUpdate)
            is MainAction.DeleteSubscription -> deleteSubscription(action.subId)
            is MainAction.SetLocalDnsEnabled -> {
                dataSource.setSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED, action.enabled)
                _uiState.update { it.copy(localDnsEnabled = action.enabled) }
            }
            is MainAction.SetFakeDnsEnabled -> {
                dataSource.setSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED, action.enabled)
                _uiState.update { it.copy(fakeDnsEnabled = action.enabled) }
            }
            is MainAction.SetBypassIranianSites -> {
                dataSource.setBypassIranianSites(action.enabled)
                _uiState.update { it.copy(bypassIranianSites = action.enabled) }
                if (uiState.value.isRunning) {
                    _uiEffects.trySend(MainUiEffect.ConnectOrRestart)
                }
            }
            is MainAction.SetDelayTestUrl -> {
                dataSource.setSettingsString(AppConfig.PREF_DELAY_TEST_URL, action.url)
                _uiState.update { it.copy(delayTestUrl = action.url) }
            }
            is MainAction.SetLanguageCode -> {
                dataSource.setLanguageCode(action.code)
                _uiState.update { it.copy(languageCode = action.code) }
            }
            MainAction.LocateHandled -> consumeLocateTarget()
            is MainAction.ShareQRCode -> {
                if (ViraxAccessManager.isProtectedServer(action.guid)) {
                    toastError(R.string.virax_protected_edit_locked)
                } else {
                    val bitmap = dataSource.share2QRCode(action.guid)
                    _uiState.update { it.copy(shareQRCodeBitmap = bitmap) }
                }
            }

            MainAction.DismissQRCodeDialog -> {
                _uiState.update { it.copy(shareQRCodeBitmap = null) }
            }

            MainAction.ToggleService,
            is MainAction.ConnectServer,
            MainAction.TestCurrentServer,
            MainAction.ImportQRcode,
            MainAction.ImportClipboard,
            MainAction.ImportConfigLocal,
            is MainAction.ImportManually,
            MainAction.RestartService,
            MainAction.LocateSelectedServer,
            is MainAction.EditServer,
            is MainAction.ShareClipboard,
            is MainAction.ShareFullContent,
            is MainAction.OpenSubscriptionLink -> {
                // Handled by Activity via its onAction lambda
            }
        }
    }

    // ---------- Initialization ----------
    fun initialize() {
        viewModelScope.launch(preloadDispatcher) {
            try {
                initialPageReady.await()
                delay(32)
                dataSource.initAssets()
                dataSource.syncSubscriptions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Main background initialization failed", error)
            }
        }
    }

    fun refreshUiSettings() {
        _uiState.update {
            it.copy(
                subscriptions = dataSource.getAllRawSubscriptions(),
                hasAnyServer = dataSource.hasAnyServer(),
                confirmRemove = dataSource.getConfirmRemove(),
                doubleColumnDisplay = dataSource.getDoubleColumnDisplay(),
                localDnsEnabled = dataSource.getLocalDnsEnabled(),
                fakeDnsEnabled = dataSource.getFakeDnsEnabled(),
                bypassIranianSites = dataSource.getBypassIranianSites(),
                delayTestUrl = dataSource.getDelayTestUrl(),
                languageCode = dataSource.getLanguageCode(),
            )
        }
    }

    fun notifyStartFailure() {
        _uiState.update { it.copy(startFailureCounter = it.startFailureCounter + 1) }
    }

    // ---------- Group & server loading ----------
    private suspend fun buildServersCache(guids: List<String>): List<ServersCache> =
        guids.mapNotNull { guid ->
            currentCoroutineContext().ensureActive()
            val profile = dataSource.decodeServerConfig(guid) ?: return@mapNotNull null
            val affiliation = dataSource.decodeAffiliationInfo(guid)
            ServersCache(
                guid = guid,
                profile = profile.copy(),
                testDelayMillis = affiliation?.testDelayMillis ?: 0L
            )
        }

    private suspend fun loadGroup(
        groupId: String,
        forceRefresh: Boolean = false
    ): List<ServersCache> {
        val loadMutex = groupLoadMutexes.computeIfAbsent(groupId) { Mutex() }
        return loadMutex.withLock {
            if (!forceRefresh) {
                cacheMutex.withLock { groupDataCache[groupId]?.let { return@withLock it } }
            }
            val servers = buildServersCache(dataSource.getServerGuidList(groupId))
            currentCoroutineContext().ensureActive()
            cacheMutex.withLock { groupDataCache[groupId] = servers }
            servers
        }
    }

    private fun applyKeywordFilter(servers: List<ServersCache>): List<ServersCache> {
        val keyword = keywordFilter.trim()
        if (keyword.isEmpty()) return servers
        val regex = try {
            Regex(keyword, RegexOption.IGNORE_CASE)
        } catch (_: PatternSyntaxException) {
            return servers
        }
        return servers.filter { cache ->
            val profile = cache.profile
            val isProtected = ViraxAccessManager.isProtectedServer(
                guid = cache.guid,
                profile = profile,
                subscriptionLookup = { subId -> dataSource.getSubscriptionItem(subId) }
            )
            profile.remarks.matchesPattern(regex, keyword) ||
                profile.configType.name.matchesPattern(regex, keyword) ||
                (!isProtected && (
                    profile.description.orEmpty().matchesPattern(regex, keyword) ||
                        profile.server.orEmpty().matchesPattern(regex, keyword)
                ))
        }
    }

    private fun updateGroupUi(groupId: String, servers: List<ServersCache>) {
        val filteredServers = applyKeywordFilter(servers)
        mutableServerGroupState(groupId).value = ServerGroupUiState(
            servers = filteredServers,
            rows = buildServerRows(groupId, filteredServers)
        )
    }

    private fun buildServerRows(groupId: String, servers: List<ServersCache>): List<ServerRowUiModel> {
        val subscriptionItems = servers.asSequence()
            .map { it.profile.subscriptionId }
            .filter { it.isNotEmpty() }
            .distinct()
            .associateWith { subscriptionId ->
                dataSource.getSubscriptionItem(subscriptionId)
            }
        return servers.map { server ->
            val subItem = subscriptionItems[server.profile.subscriptionId]
            buildServerRowUiModel(
                server = server,
                subscriptionRemarks = if (groupId.isEmpty()) subItem?.remarks.orEmpty() else "",
                isSubscriptionProtected = ViraxAccessManager.isProtectedSubscription(subItem),
            )
        }
    }

    fun getSubscriptions(): List<SubscriptionCache> = dataSource.getSubscriptions()

    private fun resolveSelectedGroup(groups: List<GroupMapItem>): String {
        val current = uiState.value.selectedGroupId
        val resolved = when {
            groups.isEmpty() -> ""
            groups.any { it.id == current } -> current
            else -> groups.first().id
        }
        if (resolved != current) {
            dataSource.setSelectedSubscriptionId(resolved)
        }
        return resolved
    }

    private fun radialPreloadOrder(groups: List<GroupMapItem>, selectedIndex: Int): List<String> {
        if (groups.isEmpty()) return emptyList()
        val result = ArrayList<String>((groups.size - 1).coerceAtLeast(0))
        for (distance in 1 until groups.size) {
            val right = selectedIndex + distance
            val left = selectedIndex - distance
            if (right in groups.indices) result += groups[right].id
            if (left in groups.indices) result += groups[left].id
        }
        return result
    }

    fun setupGroupTab(forceRefresh: Boolean = false): Job {
        setupGroupJob?.cancel()
        preloadJob?.cancel()
        selectedGroupLoadJob?.cancel()

        return viewModelScope.launch(ioDispatcher) {
            try {
                if (forceRefresh) {
                    cacheMutex.withLock { groupDataCache.clear() }
                }
                val groups = dataSource.getSubscriptions().map {
                    val isProtectedSub = ViraxAccessManager.isProtectedSubscription(it.subscription)
                    GroupMapItem(
                        id = it.guid,
                        remarks = it.subscription.remarks,
                        usageInfo = it.subscription.usageInfo?.let { info ->
                            if (isProtectedSub) info.copy(webPageUrl = null)
                            else SubscriptionInfoParser.withWebPageFallback(info, it.subscription.url)
                        },
                    )
                }
                val rawSubscriptions = dataSource.getAllRawSubscriptions()
                val selectedGroup = resolveSelectedGroup(groups)
                val validIds = groups.mapTo(HashSet()) { it.id }.apply { add("") }
                groupUiFlows.keys.removeAll { it !in validIds }
                groupServerFlows.keys.removeAll { it !in validIds }
                groupLoadMutexes.keys.removeAll { it !in validIds }

                mutableServerGroupState("")
                groups.forEach { mutableServerGroupState(it.id) }

                val selectedServers = loadGroup(selectedGroup, forceRefresh)
                updateGroupUi(selectedGroup, selectedServers)

                var currentSelectedGuid = dataSource.getSelectServer()
                if (currentSelectedGuid.isNullOrBlank() && selectedServers.isNotEmpty()) {
                    currentSelectedGuid = selectedServers.first().guid
                    dataSource.setSelectServer(currentSelectedGuid)
                }

                _uiState.update {
                    it.copy(
                        groups = groups,
                        groupsLoaded = true,
                        subscriptions = rawSubscriptions,
                        hasAnyServer = dataSource.hasAnyServer() || selectedServers.isNotEmpty(),
                        selectedGroupId = selectedGroup,
                        selectedGuid = currentSelectedGuid,
                    )
                }

                if (!initialPageReady.isCompleted) {
                    initialPageReady.complete(Unit)
                }

                if (selectedServers.isNotEmpty()) {
                    withContext(Dispatchers.Main.immediate) {
                        triggerAutoPingIfNeeded(selectedGroup, selectedServers)
                    }
                }

                if (groups.isEmpty()) {
                    return@launch
                }

                val selectedIndex =
                    groups.indexOfFirst { it.id == selectedGroup }.coerceAtLeast(0)
                val preloadOrder = radialPreloadOrder(groups, selectedIndex)
                preloadJob = viewModelScope.launch(preloadDispatcher) {
                    preloadOrder.forEach { groupId ->
                        ensureActive()
                        delay(32)
                        val servers = loadGroup(groupId, forceRefresh)
                        updateGroupUi(groupId, servers)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to set up group tabs", error)
            } finally {
                if (!initialPageReady.isCompleted) {
                    initialPageReady.complete(Unit)
                }
            }
        }.also { setupGroupJob = it }
    }

    // ---------- Business actions (coroutine-based) ----------
    private fun importBatchConfig(configText: String) {
        val trimmed = configText.trim()
        if (trimmed.isEmpty()) {
            toastError(R.string.toast_none_data_clipboard)
            return
        }
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    val urlLines = lines.filter { dataSource.isValidUrl(it) }.distinct()
                    val configLinesText = lines.filterNot { dataSource.isValidUrl(it) }.joinToString("\n")

                    var importedConfigs = 0
                    var importedSubs = 0
                    var importedSubConfigs = 0
                    var lastSubGuid: String? = null

                    if (configLinesText.isNotBlank()) {
                        val (cnt, subCnt) = dataSource.importBatchConfig(
                            configLinesText, uiState.value.selectedGroupId, true
                        )
                        importedConfigs += cnt
                        importedSubs += subCnt
                    }

                    urlLines.forEach { url ->
                        val (subGuid, result) = dataSource.importOrUpdateSubscriptionUrl(url, allowInsecure = true)
                        if (subGuid != null && result.configCount > 0) {
                            lastSubGuid = subGuid
                            importedSubs += 1
                            importedSubConfigs += result.configCount
                        }
                    }

                    if (importedSubs > 0) {
                        dataSource.syncSubscriptions()
                    }
                    lastSubGuid?.let { guid ->
                        autoPingedGroups.remove(guid)
                        dataSource.setSelectedSubscriptionId(guid)
                        _uiState.update { it.copy(selectedGroupId = guid) }
                    }
                    if (importedConfigs > 0 || importedSubConfigs > 0) {
                        autoPingedGroups.remove(uiState.value.selectedGroupId)
                        autoPingedGroups.remove("")
                    }

                    when {
                        importedConfigs > 0 && importedSubConfigs == 0 -> {
                            toast(dataSource.getString(R.string.title_import_config_count, importedConfigs))
                            setupGroupTab(forceRefresh = true)
                        }
                        importedSubConfigs > 0 -> {
                            toast(
                                getQuantityString(
                                    R.plurals.title_update_config_count,
                                    importedSubConfigs,
                                    importedSubConfigs,
                                )
                            )
                            setupGroupTab(forceRefresh = true)
                        }
                        importedSubs > 0 -> {
                            setupGroupTab(forceRefresh = true)
                        }
                        else -> toastError(R.string.toast_failure)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun importSubscriptionUrl(url: String) {
        val clean = url.trim()
        if (clean.isEmpty() || !dataSource.isValidUrl(clean)) {
            toastError(R.string.toast_invalid_url)
            return
        }
        importBatchConfig(clean)
    }

    private fun importDedicatedCode(code: String) {
        if (ViraxAccessManager.buildProtectedSubscriptionUrl(code) == null) {
            toastError(R.string.virax_invalid_dedicated_code)
            return
        }
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val (subGuid, result) = dataSource.importDedicatedCode(code)
                    if (subGuid != null && (result.configCount > 0 || result.successCount > 0)) {
                        dataSource.syncSubscriptions()
                        autoPingedGroups.remove(subGuid)
                        autoPingedGroups.remove("")
                        dataSource.setSelectedSubscriptionId(subGuid)
                        _uiState.update { it.copy(selectedGroupId = subGuid) }
                        if (result.configCount > 0) {
                            toast(
                                getQuantityString(
                                    R.plurals.title_update_config_count,
                                    result.configCount,
                                    result.configCount,
                                )
                            )
                        } else {
                            toastSuccess(R.string.toast_success)
                        }
                        setupGroupTab(forceRefresh = true)
                    } else {
                        toastError(R.string.toast_failure)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to import dedicated code", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun toggleSubscriptionEnabled(subId: String, enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val item = dataSource.getSubscriptionItem(subId) ?: return@launch
            val updated = item.copy(enabled = enabled)
            dataSource.updateSubscriptionItem(subId, updated)
            setupGroupTab(forceRefresh = true)
        }
    }

    private fun toggleSubscriptionAutoUpdate(subId: String, autoUpdate: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            val item = dataSource.getSubscriptionItem(subId) ?: return@launch
            val updated = item.copy(
                autoUpdate = autoUpdate,
                updateInterval = if (item.updateInterval > 0) item.updateInterval else 720L
            )
            dataSource.updateSubscriptionItem(subId, updated)
            _uiState.update { it.copy(subscriptions = dataSource.getAllRawSubscriptions()) }
        }
    }

    private fun deleteSubscription(subId: String) {
        launchLoading {
            withContext(ioDispatcher) {
                dataSource.deleteSubscription(subId)
                cacheMutex.withLock { groupDataCache.clear() }
                setupGroupTab(forceRefresh = true).join()
            }
        }
    }

    private fun importConfigViaSub(subId: String) {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val result = if (subId.isEmpty()) {
                        dataSource.updateConfigViaSubAll()
                    } else {
                        val item = dataSource.getSubscriptionItem(subId) ?: return@withContext
                        dataSource.updateConfigViaSub(SubscriptionCache(subId, item))
                    }
                    when {
                        result.successCount + result.failureCount + result.skipCount == 0 ->
                            toast(R.string.title_update_subscription_no_subscription)

                        result.successCount > 0 && result.failureCount + result.skipCount == 0 ->
                            toast(
                                getQuantityString(
                                    R.plurals.title_update_config_count,
                                    result.configCount,
                                    result.configCount,
                                )
                            )

                        else ->
                            toast(dataSource.getString(R.string.title_update_subscription_result, result.configCount, result.successCount, result.failureCount, result.skipCount))
                    }
                    if (result.configCount > 0 || result.successCount > 0) {
                        autoPingedGroups.remove(subId)
                        autoPingedGroups.remove("")
                        setupGroupTab(forceRefresh = true)
                        refreshSelectedGuid()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Subscription update failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun exportAllAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val groupId = uiState.value.selectedGroupId
                    val list = if (groupId.isEmpty() && keywordFilter.isEmpty()) {
                        dataSource.getServerGuidList("")
                    } else {
                        currentServers().map { it.guid }
                    }
                    val ret = dataSource.shareNonCustomConfigsToClipboard(list)
                    if (ret > 0) {
                        toast(dataSource.getString(R.string.title_export_config_count, ret))
                    } else {
                        toastError(R.string.toast_failure)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Export failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeAllServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val count =
                        if (uiState.value.selectedGroupId.isEmpty() && keywordFilter.isEmpty()) {
                            dataSource.removeAllServer()
                        } else {
                            val guids = currentServers().map { it.guid }
                            guids.forEach { dataSource.removeServer(it) }
                            guids.size
                        }
                    viewModelScope.launch(ioDispatcher) {
                        cacheMutex.withLock { groupDataCache.clear() }
                    }
                    setupGroupTab(forceRefresh = true)
                    toast(dataSource.getString(R.string.title_del_config_count, count))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete all failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeDuplicateServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val seen = HashSet<ProfileItem>()
                    val duplicates = ArrayList<String>()
                    currentServers().forEach { server ->
                        val profile = server.profile
                        if (!profile.configType.isComplexType()) {
                            val identity = profile.duplicateIdentity()
                            if (!seen.add(identity)) duplicates += server.guid
                        }
                    }
                    duplicates.forEach { dataSource.removeServer(it) }
                    setupGroupTab(forceRefresh = true)
                    toast(dataSource.getString(R.string.title_del_duplicate_config_count, duplicates.size))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete duplicate failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeInvalidServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val count = removeInvalidServerInternal()
                    viewModelScope.launch(ioDispatcher) {
                        cacheMutex.withLock { groupDataCache.clear() }
                        setupGroupTab(forceRefresh = true)
                    }
                    toast(dataSource.getString(R.string.title_del_config_count, count))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete invalid failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeInvalidServerInternal(): Int {
        val visibleServersOnly =
            uiState.value.selectedGroupId.isNotEmpty() || keywordFilter.isNotBlank()
        return if (visibleServersOnly) {
            currentServers().sumOf { server ->
                dataSource.removeInvalidServerByGuid(server.guid)
            }
        } else {
            dataSource.removeInvalidServersInGroup("")
        }
    }

    private fun sortByTestResultsAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    sortByTestResultsInternal()
                    cacheMutex.withLock { groupDataCache.clear() }
                    setupGroupTab(forceRefresh = true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Sort by test results failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun sortByTestResultsInternal() {
        val subs = if (uiState.value.selectedGroupId.isEmpty()) {
            dataSource.getSubsList()
        } else {
            listOf(uiState.value.selectedGroupId)
        }
        subs.forEach { dataSource.sortByTestResultsForSub(it) }
    }

    fun subscriptionIdChanged(id: String) {
        if (id.isNotEmpty() && _uiState.value.groups.none { it.id == id } && _uiState.value.subscriptions.none { it.guid == id }) return
        mutableServerGroupState(id)
        if (uiState.value.selectedGroupId != id) {
            dataSource.setSelectedSubscriptionId(id)
            _uiState.update { it.copy(selectedGroupId = id) }
        }
        selectedGroupLoadJob?.cancel()
        selectedGroupLoadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val loadedServers = loadGroup(id)
                updateGroupUi(id, loadedServers)
                if (loadedServers.isNotEmpty()) {
                    withContext(Dispatchers.Main.immediate) {
                        triggerAutoPingIfNeeded(id, loadedServers)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load selected group: $id", error)
            }
        }
    }

    fun reloadServerList() {
        val groupId = uiState.value.selectedGroupId
        selectedGroupLoadJob?.cancel()
        selectedGroupLoadJob = viewModelScope.launch(ioDispatcher) {
            updateGroupUi(groupId, loadGroup(groupId, forceRefresh = true))
        }
    }

    fun reloadAllGroups(groupIds: List<String>) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch(preloadDispatcher) {
            val selected = uiState.value.selectedGroupId
            val order = buildList {
                if (selected in groupIds) add(selected)
                addAll(groupIds.filter { it != selected })
            }
            order.forEachIndexed { index, groupId ->
                ensureActive()
                if (index > 0) delay(32)
                updateGroupUi(groupId, loadGroup(groupId, forceRefresh = true))
            }
        }
    }

    fun filterConfig(keyword: String) {
        if (keyword == keywordFilter) return
        keywordFilter = keyword
        filterJob?.cancel()
        filterJob = viewModelScope.launch(defaultDispatcher) {
            delay(300)
            val snapshot = cacheMutex.withLock { groupDataCache.toMap() }
            ensureActive()
            snapshot.forEach { (groupId, servers) ->
                ensureActive()
                updateGroupUi(groupId, servers)
            }
        }
    }

    fun updateSelectedGuid(guid: String) {
        dataSource.setSelectServer(guid)
        _uiState.update { it.copy(selectedGuid = guid) }
    }

    fun refreshSelectedGuid() {
        _uiState.update { it.copy(selectedGuid = dataSource.getSelectServer()) }
    }

    fun removeServerAndRefresh(guid: String) {
        if (uiState.value.isRunning && guid == uiState.value.selectedGuid) {
            toast(R.string.toast_action_not_allowed)
            return
        }
        viewModelScope.launch(ioDispatcher) {
            dataSource.removeServer(guid)
            if (guid == uiState.value.selectedGuid) {
                dataSource.setSelectServer("")
            }
            cacheMutex.withLock { groupDataCache.clear() }
            setupGroupTab(forceRefresh = true).join()
        }
    }

    fun moveServer(groupId: String, fromPosition: Int, toPosition: Int) {
        val groupState = mutableServerGroupState(groupId).value
        val servers = groupState.servers.toMutableList()
        if (!servers.moveItem(fromPosition, toPosition)) return
        val rows = groupState.rows.toMutableList()
        rows.moveItem(fromPosition, toPosition)
        val guids = servers.map { it.guid }
        mutableServerGroupState(groupId).value = ServerGroupUiState(servers, rows)
        // A drag emits several moves; serialize writes so an older order cannot overwrite a newer one.
        val previousPersistenceJob = serverOrderPersistenceJobs[groupId]
        serverOrderPersistenceJobs[groupId] = viewModelScope.launch(ioDispatcher) {
            previousPersistenceJob?.join()
            dataSource.encodeServerList(guids, groupId)
            cacheMutex.withLock { groupDataCache[groupId] = servers }
        }
    }

    // ---------- Testing ----------
    fun cancelAllPing() {
        smartConnectPreStopJob?.cancel()
        smartConnectPreStopJob = null
        pendingSmartConnectRequestId = null
        smartConnectFallbackActive = false
        smartConnectAttemptedGuids.clear()
        bulkTestJob?.cancel()
        bulkTestJob = null
        singleServerPingRequestIds.clear()
        testRequests.cancelBulk()
        testRequests.invalidateCurrent()
        cancelPendingTestResults()
        _uiState.update { it.copy(isSmartConnecting = false, isAutoPinging = false) }
        resetTestStatus()
        dataSource.cancelAllPing()
    }

    private fun resetTestStatus() {
        _uiState.update {
            it.copy(
                isTesting = testRequests.isTesting,
                isAutoPinging = if (testRequests.bulk != null) it.isAutoPinging else false,
                status = if (testRequests.isTesting) MainStatus.Testing
                else if (it.isRunning) MainStatus.Connected else MainStatus.Disconnected
            )
        }
    }

    fun testAllRealPing(onlyTcp: Boolean = false) {
        cancelAllPing()
        startBulkRealPing(onlyTcp = onlyTcp, explicitGroupId = activeGroupId())
    }

    private fun testSingleServerRealPing(guid: String) {
        if (guid.isBlank()) return
        cancelAllPing()
        val requestId = startBulkRealPing(
            onlyTcp = false,
            explicitGroupId = activeGroupId(),
            targetGuids = listOf(guid),
            clearPreviousDelays = false
        )
        if (requestId != null) {
            singleServerPingRequestIds.add(requestId)
        }
    }

    private fun isGroupPingFresh(groupId: String): Boolean {
        val lastTestedAt = autoPingedGroups[groupId] ?: return false
        return (dataSource.getElapsedRealtimeMs() - lastTestedAt) <= AUTO_PING_FRESHNESS_MS
    }

    private fun triggerAutoPingIfNeeded(groupId: String, servers: List<ServersCache>) {
        if (servers.isEmpty()) return
        if (uiState.value.isRunning || uiState.value.isSmartConnecting || testRequests.isTesting) return
        if (isGroupPingFresh(groupId)) return
        autoPingedGroups[groupId] = dataSource.getElapsedRealtimeMs()
        startBulkRealPing(
            onlyTcp = false,
            explicitGroupId = groupId,
            clearPreviousDelays = false
        )
    }

    private fun startBulkRealPing(
        onlyTcp: Boolean,
        explicitGroupId: String,
        targetGuids: List<String>? = null,
        clearPreviousDelays: Boolean = true
    ): String? {
        val groupId = explicitGroupId
        val servers = mutableServerGroupState(groupId).value.servers
        if (servers.isEmpty()) {
            return null
        }
        val effectiveTargetGuids = if (!targetGuids.isNullOrEmpty()) {
            targetGuids
        } else {
            servers.map { it.guid }
        }
        val resetGuids = effectiveTargetGuids.toHashSet()
        val shouldClear = clearPreviousDelays || !targetGuids.isNullOrEmpty()
        if (shouldClear) {
            mutableServerGroupState(groupId).update { current ->
                current.copy(
                    servers = current.servers.map { server ->
                        if (server.guid !in resetGuids || server.testDelayMillis == 0L) server
                        else server.copy(testDelayMillis = 0L)
                    },
                    rows = current.rows.map { row ->
                        if (row.guid !in resetGuids || row.testDelayMillis == 0L) row
                        else row.copy(testDelayMillis = 0L)
                    }
                )
            }
        }
        val request = testRequests.beginBulk(groupId)
        val message = TestServiceMessage(
            key = AppConfig.MSG_MEASURE_CONFIG_START,
            subscriptionId = groupId,
            serverGuids = if (!targetGuids.isNullOrEmpty() || keywordFilter.isNotEmpty()) effectiveTargetGuids else emptyList(),
            onlyTcp = onlyTcp
        )
        _uiState.update {
            it.copy(
                isTesting = true,
                isAutoPinging = targetGuids.isNullOrEmpty() && pendingSmartConnectRequestId == null,
                status = MainStatus.Testing
            )
        }
        bulkTestJob = viewModelScope.launch {
            if (shouldClear) {
                withContext(ioDispatcher) {
                    dataSource.clearAllTestDelayResults(effectiveTargetGuids)
                    cacheMutex.withLock {
                        groupDataCache[groupId]?.let { cached ->
                            groupDataCache[groupId] = cached.map { server ->
                                if (server.guid !in resetGuids || server.testDelayMillis == 0L) server
                                else server.copy(testDelayMillis = 0L)
                            }
                        }
                    }
                }
            }
            dataSource.sendMsg2TestService(message, request.id)
        }
        return request.id
    }

    private fun smartConnect() {
        smartConnectAttemptedGuids.clear()
        val groupId = activeGroupId()
        val servers = mutableServerGroupState(groupId).value.servers
        if (servers.isEmpty()) {
            smartConnectFallbackActive = false
            _uiState.update { it.copy(isSmartConnecting = false) }
            notifyStartFailure()
            toastError(R.string.virax_no_servers_found)
            return
        }

        // Reuse an in-flight background startup ping for this group rather than cancelling and restarting it.
        val activeBulk = testRequests.bulk
        if (!uiState.value.isRunning &&
            activeBulk != null &&
            activeBulk.groupId == groupId &&
            activeBulk.id !in singleServerPingRequestIds
        ) {
            pendingSmartConnectRequestId = activeBulk.id
            smartConnectFallbackActive = true
            _uiState.update { it.copy(isSmartConnecting = true, isAutoPinging = false) }
            return
        }

        // Connect immediately if this group already has fresh healthy ping measurements while disconnected.
        if (!uiState.value.isRunning && isGroupPingFresh(groupId)) {
            val fastestGuid = ViraxConnectionManager.findFastestServerGuid(servers)
            if (fastestGuid != null) {
                smartConnectFallbackActive = true
                smartConnectAttemptedGuids.add(fastestGuid)
                updateSelectedGuid(fastestGuid)
                _uiState.update { it.copy(isSmartConnecting = false) }
                _uiEffects.trySend(MainUiEffect.ConnectOrRestart)
                return
            }
        }

        cancelAllPing()
        smartConnectFallbackActive = true
        _uiState.update { it.copy(isSmartConnecting = true, isAutoPinging = false) }
        smartConnectPreStopJob = viewModelScope.launch {
            if (uiState.value.isRunning) {
                _uiEffects.send(MainUiEffect.StopForSmartConnect)
                withTimeoutOrNull(2500L) {
                    uiState.first { !it.isRunning }
                }
            }
            val requestId = startBulkRealPing(onlyTcp = false, explicitGroupId = groupId)
            if (requestId != null) {
                pendingSmartConnectRequestId = requestId
            } else {
                smartConnectFallbackActive = false
                _uiState.update { it.copy(isSmartConnecting = false) }
                notifyStartFailure()
            }
        }
    }

    private fun completeSmartConnect(testedServers: List<ServersCache>) {
        val bestGuid = ViraxConnectionManager.findFastestServerGuidExcluding(
            testedServers,
            smartConnectAttemptedGuids
        )
        if (bestGuid != null) {
            smartConnectFallbackActive = true
            smartConnectAttemptedGuids.add(bestGuid)
            updateSelectedGuid(bestGuid)
            _uiState.update { it.copy(isSmartConnecting = false) }
            _uiEffects.trySend(MainUiEffect.ConnectOrRestart)
        } else {
            smartConnectFallbackActive = false
            smartConnectAttemptedGuids.clear()
            _uiState.update { it.copy(isSmartConnecting = false) }
            notifyStartFailure()
            toastError(R.string.virax_no_healthy_server)
        }
    }

    private fun cancelPendingTestResults() {
        testResultFlushJob?.cancel()
        testResultFlushJob = null
        pendingTestResults.clear()
    }

    fun testCurrentServerRealPing() {
        if (!uiState.value.isRunning) return
        val requestId = testRequests.beginCurrent()
        _uiState.update { it.copy(isTesting = true, status = MainStatus.Testing) }
        dataSource.testCurrentServerRealPing(requestId)
    }

    private fun onTestsFinished(requestId: String) {
        if (testRequests.completeBulk(requestId) == null) return
        resetTestStatus()
        viewModelScope.launch(ioDispatcher) {
            cacheMutex.withLock { groupDataCache.clear() }
            reloadAllGroups(_uiState.value.groups.map { it.id })
        }
    }

    fun triggerLocateSelectedServer() {
        val selected = dataSource.getSelectServer() ?: return
        val profile = dataSource.decodeServerConfig(selected) ?: return
        val groupId = profile.subscriptionId
        if (_uiState.value.groups.none { it.id == groupId }) return
        viewModelScope.launch(ioDispatcher) {
            updateGroupUi(groupId, loadGroup(groupId))
            if (_uiState.value.selectedGroupId != groupId) {
                dataSource.setSelectedSubscriptionId(groupId)
            }
            val target = LocateTarget(groupId, selected)
            _uiState.update {
                it.copy(selectedGroupId = groupId, locateTarget = target)
            }
        }
    }

    private fun consumeLocateTarget() {
        _uiState.update { it.copy(locateTarget = null) }
    }

    // ---------- Running state ----------
    private fun updateRunningState(running: Boolean, clearTestingText: Boolean = true) {
        if (!running || clearTestingText) testRequests.invalidateCurrent()
        if (running) {
            startSessionMetricsTracking()
        } else {
            stopSessionMetricsTracking()
        }
        _uiState.update { state ->
            state.copy(
                isRunning = running,
                isTesting = testRequests.isTesting,
                sessionDurationText = if (running) state.sessionDurationText else "00:00:00",
                sessionDownloadText = if (running) state.sessionDownloadText else "0 B",
                sessionUploadText = if (running) state.sessionUploadText else "0 B",
                status = if (!clearTestingText && state.isRunning == running) state.status
                else if (running) MainStatus.Connected else MainStatus.Disconnected
            )
        }
    }

    private fun startSessionMetricsTracking() {
        if (sessionMetricsJob?.isActive == true) return
        fallbackConnectionSession = null
        sessionMetricsJob = viewModelScope.launch {
            while (isActive) {
                val persistedSession = withContext(ioDispatcher) { dataSource.getConnectionSession() }
                val currentElapsedMs = dataSource.getElapsedRealtimeMs()
                val currentRx = dataSource.getAppRxBytes()
                val currentTx = dataSource.getAppTxBytes()
                val effectiveSession = if (persistedSession != null) {
                    persistedSession
                } else {
                    fallbackConnectionSession ?: ViraxConnectionSession(
                        connectedElapsedMs = currentElapsedMs,
                        baseRxBytes = currentRx,
                        baseTxBytes = currentTx,
                    ).also { fallbackConnectionSession = it }
                }
                val snapshot = ViraxConnectionManager.computeSessionTrafficSnapshot(
                    isRunning = true,
                    session = effectiveSession,
                    currentElapsedMs = currentElapsedMs,
                    currentRxBytes = currentRx,
                    currentTxBytes = currentTx,
                )
                _uiState.update {
                    it.copy(
                        sessionDurationText = snapshot.durationText,
                        sessionDownloadText = snapshot.downloadText,
                        sessionUploadText = snapshot.uploadText,
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun stopSessionMetricsTracking() {
        sessionMetricsJob?.cancel()
        sessionMetricsJob = null
        fallbackConnectionSession = null
    }

    override fun onCleared() {
        setupGroupJob?.cancel()
        preloadJob?.cancel()
        selectedGroupLoadJob?.cancel()
        reloadJob?.cancel()
        filterJob?.cancel()
        stopSessionMetricsTracking()
        cancelAllPing()
        _uiEffects.close()
        dataSource.close()
        super.onCleared()
    }

    // ---------- Factory ----------
    class Factory(private val application: Application, private val dataSource: MainDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(application, dataSource) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private companion object {
        const val TEST_RESULT_FLUSH_INTERVAL_MS = 500L
        const val AUTO_PING_FRESHNESS_MS = 180_000L
        const val MAX_SMART_CONNECT_ATTEMPTS = 3
    }
}
