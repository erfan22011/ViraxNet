package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.handler.SubscriptionInfoParser
import com.v2ray.ang.handler.ViraxAccessManager
import com.v2ray.ang.ui.compose.ConfirmDialog
import com.v2ray.ang.ui.compose.QRCodeDialog
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainDestination
import com.v2ray.ang.ui.main.MainStatus
import com.v2ray.ang.ui.main.MainViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private enum class ViraxImportMode {
    SUBSCRIPTION_OR_LINK,
    DEDICATED_CODE
}

@Composable
fun ViraxMainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val isTesting = uiState.isTesting
    val selectedGuid = uiState.selectedGuid

    val activeSubscriptions = uiState.activeSubscriptions
    val planBadge = uiState.planBadge

    val activeGroupId = remember(uiState.selectedGroupId, uiState.groups) {
        uiState.selectedGroupId.ifEmpty { uiState.groups.firstOrNull()?.id.orEmpty() }
    }
    val groupState by mainViewModel.serverGroupState(activeGroupId).collectAsStateWithLifecycle()

    val viraxServers = remember(groupState.rows, selectedGuid) {
        groupState.rows.map { row ->
            ViraxServerItem(
                guid = row.guid,
                remarks = row.remarks,
                pingMillis = row.testDelayMillis,
                isSelected = row.guid == selectedGuid,
                profile = row.profile,
                typeDescription = row.typeDescription,
                isProtected = row.isProtected || ViraxAccessManager.isProtectedServer(row.guid, row.profile)
            )
        }
    }

    val allServersLabel = stringResource(R.string.virax_all_servers)
    val selectorGroups = remember(activeSubscriptions, uiState.groups, uiState.selectedGroupId, allServersLabel) {
        val list = mutableListOf<GroupMapItem>()
        val hasAllGroup = uiState.groups.any { it.id.isEmpty() }
        if (activeSubscriptions.size > 1 || hasAllGroup || (activeSubscriptions.isNotEmpty() && uiState.selectedGroupId.isEmpty())) {
            val allLabel = uiState.groups.firstOrNull { it.id.isEmpty() }?.remarks?.takeIf { it.isNotBlank() }
                ?: allServersLabel
            list += GroupMapItem(id = "", remarks = allLabel)
        }
        activeSubscriptions.forEach { sub ->
            val isProtectedSub = ViraxAccessManager.isProtectedSubscription(sub.subscription)
            list += GroupMapItem(
                id = sub.guid,
                remarks = sub.subscription.remarks,
                usageInfo = sub.subscription.usageInfo?.let { info ->
                    if (isProtectedSub) info.copy(webPageUrl = null)
                    else SubscriptionInfoParser.withWebPageFallback(info, sub.subscription.url)
                }
            )
        }
        list
    }

    val currentServerRow = groupState.rows.firstOrNull { it.guid == selectedGuid }
    val currentServerName = currentServerRow?.remarks
    val currentServerPing = currentServerRow?.testDelayMillis ?: 0L

    var selectedTab by rememberSaveable { mutableStateOf(ViraxTab.HOME) }
    var isConnectingModalVisible by remember { mutableStateOf(false) }
    var isFetchingIp by remember { mutableStateOf(false) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var hasDismissedOnboarding by rememberSaveable { mutableStateOf(false) }
    var showManualSubInputDialog by remember { mutableStateOf(false) }
    var manualImportMode by rememberSaveable { mutableStateOf(ViraxImportMode.SUBSCRIPTION_OR_LINK) }

    // Ensure Connecting dialog closes immediately when VPN becomes active or fails/cancels
    LaunchedEffect(isRunning) {
        if (isRunning) {
            isConnectingModalVisible = false
        }
    }
    LaunchedEffect(uiState.startFailureCounter) {
        if (uiState.startFailureCounter > 0L) {
            isConnectingModalVisible = false
        }
    }
    LaunchedEffect(isConnectingModalVisible, uiState.isSmartConnecting) {
        if (isConnectingModalVisible && !uiState.isSmartConnecting) {
            delay(20_000L)
            isConnectingModalVisible = false
        }
    }

    val liveIpAddress = remember(isRunning, uiState.status) {
        if (!isRunning) {
            null
        } else {
            val result = (uiState.status as? MainStatus.ConnectionTest)?.result
            if (result != null && !result.ipAddress.isNullOrBlank()) {
                val country = result.country?.takeIf { it.isNotBlank() }
                if (country != null) "($country) ${result.ipAddress}" else result.ipAddress
            } else {
                null
            }
        }
    }

    val latestIsRunning by rememberUpdatedState(isRunning)
    val latestLiveIp by rememberUpdatedState(liveIpAddress)

    LaunchedEffect(liveIpAddress) {
        if (liveIpAddress != null) {
            isFetchingIp = false
        }
    }

    // Automatically test IP and latency when connected or switching servers while connected, with retries
    LaunchedEffect(isRunning, selectedGuid) {
        if (!isRunning) {
            isFetchingIp = false
            return@LaunchedEffect
        }
        isFetchingIp = true
        val retryDelays = listOf(800L, 2500L, 4000L)
        for (waitMs in retryDelays) {
            delay(waitMs)
            if (!latestIsRunning || latestLiveIp != null) break
            onAction(MainAction.TestCurrentServer)
        }
        delay(1500L)
        isFetchingIp = false
    }

    val liveServerPing = remember(currentServerPing, uiState.status, isRunning) {
        val testedDelay = if (isRunning) (uiState.status as? MainStatus.ConnectionTest)?.result?.delayMillis else null
        if (testedDelay != null && testedDelay > 0) testedDelay else currentServerPing
    }

    // Theme mode: "0" = System, "1" = Light, "2" = Dark
    val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
    val languageCode = uiState.languageCode

    val layoutDirection = when (languageCode) {
        "fa" -> LayoutDirection.Rtl
        "en" -> LayoutDirection.Ltr
        else -> {
            val defaultLang = Locale.getDefault().language
            if (defaultLang in setOf("fa", "ar", "ur", "he")) LayoutDirection.Rtl else LayoutDirection.Ltr
        }
    }

    val effectiveServersSelectedGuid = remember(viraxServers, selectedGuid) {
        ViraxConnectionManager.resolveExplicitOrFirstServerGuid(viraxServers, selectedGuid)
    }

    val handleConnectOrDisconnect: () -> Unit = {
        if (isRunning) {
            // Ensure disconnect never triggers Connecting modal
            isConnectingModalVisible = false
            onAction(MainAction.ToggleService)
        } else if (viraxServers.isEmpty()) {
            isConnectingModalVisible = false
            manualImportMode = ViraxImportMode.DEDICATED_CODE
            showManualSubInputDialog = true
        } else {
            isConnectingModalVisible = true
            onAction(MainAction.SmartConnect)
        }
    }

    val handleSmartConnect: () -> Unit = {
        if (viraxServers.isEmpty()) {
            isConnectingModalVisible = false
            manualImportMode = ViraxImportMode.DEDICATED_CODE
            showManualSubInputDialog = true
        } else {
            selectedTab = ViraxTab.HOME
            isConnectingModalVisible = true
            onAction(MainAction.SmartConnect)
        }
    }

    val handleConnectSelected: () -> Unit = {
        val targetGuid = effectiveServersSelectedGuid
        if (targetGuid.isNullOrBlank()) {
            isConnectingModalVisible = false
            manualImportMode = ViraxImportMode.DEDICATED_CODE
            showManualSubInputDialog = true
        } else {
            selectedTab = ViraxTab.HOME
            isConnectingModalVisible = true
            onAction(MainAction.ConnectServer(targetGuid))
        }
    }

    val showOnboardingPrompt = uiState.groupsLoaded &&
        !uiState.hasActiveSubscription &&
        !hasDismissedOnboarding &&
        !showManualSubInputDialog

    ViraxThemeProvider(themeMode = themeMode) {
        val colors = ViraxTheme.colors

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            // Disconnect Confirmation Dialog (when green shield circle is tapped while connected)
            if (showDisconnectConfirm) {
                ConfirmDialog(
                    title = stringResource(R.string.virax_disconnect_confirm_title),
                    message = stringResource(R.string.virax_disconnect_confirm_message),
                    confirmText = stringResource(R.string.virax_disconnect),
                    dismissText = stringResource(R.string.virax_cancel),
                    onConfirm = {
                        showDisconnectConfirm = false
                        isConnectingModalVisible = false
                        if (isRunning) {
                            onAction(MainAction.ToggleService)
                        }
                    },
                    onDismiss = { showDisconnectConfirm = false }
                )
            }

            // QR Code Share Dialog
            uiState.shareQRCodeBitmap?.let { bitmap ->
                QRCodeDialog(
                    bitmap = bitmap,
                    onDismiss = { onAction(MainAction.DismissQRCodeDialog) }
                )
            }

            // Subscription / Server Link / Dedicated Code Input Modal
            if (showOnboardingPrompt || showManualSubInputDialog) {
                val isOnboarding = showOnboardingPrompt && !showManualSubInputDialog
                ViraxLinkInputDialog(
                    title = stringResource(
                        if (isOnboarding) R.string.virax_onboarding_sub_title else R.string.virax_import_link_title
                    ),
                    message = if (isOnboarding) stringResource(R.string.virax_onboarding_sub_message) else null,
                    dismissLabel = stringResource(if (isOnboarding) R.string.virax_later else R.string.virax_cancel),
                    initialMode = if (isOnboarding) ViraxImportMode.DEDICATED_CODE else manualImportMode,
                    onImportClipboard = {
                        hasDismissedOnboarding = true
                        showManualSubInputDialog = false
                        onAction(MainAction.ImportClipboard)
                    },
                    onConfirmLink = { link ->
                        hasDismissedOnboarding = true
                        showManualSubInputDialog = false
                        onAction(MainAction.ImportBatchConfig(link))
                    },
                    onConfirmDedicatedCode = { code ->
                        hasDismissedOnboarding = true
                        showManualSubInputDialog = false
                        onAction(MainAction.ImportDedicatedCode(code))
                    },
                    onDismiss = {
                        hasDismissedOnboarding = true
                        showManualSubInputDialog = false
                    }
                )
            }

            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.background),
                bottomBar = {
                    ViraxBottomNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        ViraxTab.HOME -> {
                            ViraxHomeScreen(
                                isConnected = isRunning,
                                isConnecting = isConnectingModalVisible,
                                planBadge = planBadge,
                                isFetchingIp = isFetchingIp,
                                serverName = currentServerName,
                                serverPing = liveServerPing,
                                ipAddress = liveIpAddress,
                                connectionDurationText = uiState.sessionDurationText,
                                downloadVolumeText = uiState.sessionDownloadText,
                                uploadVolumeText = uiState.sessionUploadText,
                                onPowerClick = handleConnectOrDisconnect,
                                onShieldClick = {
                                    if (isRunning) {
                                        showDisconnectConfirm = true
                                    }
                                },
                                onIpCardClick = {
                                    if (isRunning) {
                                        isFetchingIp = true
                                        onAction(MainAction.TestCurrentServer)
                                    }
                                },
                                onCrownClick = { selectedTab = ViraxTab.STATS },
                                onSmartSelectClick = handleSmartConnect
                            )
                        }

                        ViraxTab.SERVERS -> {
                            ViraxServersScreen(
                                servers = viraxServers,
                                groups = selectorGroups,
                                selectedGroupId = activeGroupId,
                                selectedGuid = effectiveServersSelectedGuid,
                                isTesting = isTesting,
                                isLoading = isLoading,
                                onSelectGroup = { groupId ->
                                    onAction(MainAction.SelectGroup(groupId))
                                },
                                onServerSelect = { guid ->
                                    onAction(MainAction.SelectServerOnly(guid))
                                },
                                onAutoConnectClick = handleSmartConnect,
                                onConnectSelectedClick = handleConnectSelected,
                                onPingAllClick = { onAction(MainAction.TestRealAllServers) },
                                onCancelPingClick = { onAction(MainAction.CancelTesting) },
                                onImportClipboardClick = { onAction(MainAction.ImportClipboard) },
                                onImportQrClick = { onAction(MainAction.ImportQRcode) },
                                onImportLocalClick = { onAction(MainAction.ImportConfigLocal) },
                                onOpenManualLinkImport = {
                                    manualImportMode = ViraxImportMode.SUBSCRIPTION_OR_LINK
                                    showManualSubInputDialog = true
                                },
                                onOpenDedicatedCodeImport = {
                                    manualImportMode = ViraxImportMode.DEDICATED_CODE
                                    showManualSubInputDialog = true
                                },
                                onPingServer = { guid ->
                                    onAction(MainAction.TestSingleServer(guid))
                                },
                                onEditServer = { guid, profile ->
                                    onAction(MainAction.EditServer(guid, profile))
                                },
                                onDeleteServer = { guid ->
                                    onAction(MainAction.RemoveServer(guid))
                                },
                                onShareServerQr = { guid ->
                                    onAction(MainAction.ShareQRCode(guid))
                                },
                                onShareServerClipboard = { guid ->
                                    onAction(MainAction.ShareClipboard(guid))
                                },
                                onBackClick = { selectedTab = ViraxTab.HOME }
                            )
                        }

                        ViraxTab.STATS -> {
                            ViraxSubscriptionScreen(
                                subscriptions = activeSubscriptions,
                                selectedSubId = activeGroupId,
                                isLoading = isLoading,
                                planBadge = planBadge,
                                onSelectSubscription = { subId ->
                                    onAction(MainAction.SelectGroup(subId))
                                },
                                onAddSubscriptionClick = {
                                    manualImportMode = ViraxImportMode.DEDICATED_CODE
                                    showManualSubInputDialog = true
                                },
                                onImportClipboardClick = { onAction(MainAction.ImportClipboard) },
                                onRefreshSubscription = { subId ->
                                    if (subId.isBlank()) {
                                        onAction(MainAction.UpdateSubscriptions)
                                    } else {
                                        onAction(MainAction.UpdateGroupSubscription(subId))
                                    }
                                },
                                onToggleAutoUpdate = { subId, autoUpdate ->
                                    onAction(MainAction.ToggleSubscriptionAutoUpdate(subId, autoUpdate))
                                },
                                onDeleteSubscription = { subId ->
                                    onAction(MainAction.DeleteSubscription(subId))
                                },
                                onManageSubscriptionClick = {
                                    onNavigate(MainDestination.Subscriptions)
                                },
                                onBackClick = { selectedTab = ViraxTab.HOME }
                            )
                        }

                        ViraxTab.SETTINGS -> {
                            ViraxSettingsScreen(
                                localDnsEnabled = uiState.localDnsEnabled,
                                onLocalDnsToggled = { enabled ->
                                    onAction(MainAction.SetLocalDnsEnabled(enabled))
                                },
                                fakeDnsEnabled = uiState.fakeDnsEnabled,
                                onFakeDnsToggled = { enabled ->
                                    onAction(MainAction.SetFakeDnsEnabled(enabled))
                                },
                                bypassIranianSites = uiState.bypassIranianSites,
                                onBypassIranianSitesToggled = { enabled ->
                                    onAction(MainAction.SetBypassIranianSites(enabled))
                                },
                                pingTestUrl = uiState.delayTestUrl,
                                onPingTestUrlChanged = { newUrl ->
                                    onAction(MainAction.SetDelayTestUrl(newUrl))
                                },
                                currentThemeMode = themeMode,
                                onThemeModeChanged = { mode ->
                                    ThemeManager.setThemeMode(mode)
                                },
                                currentLanguageCode = languageCode,
                                onLanguageChanged = { code ->
                                    onAction(MainAction.SetLanguageCode(code))
                                },
                                onNavigate = onNavigate,
                                onBackClick = { selectedTab = ViraxTab.HOME }
                            )
                        }
                    }

                    if (uiState.isSmartConnecting || (isConnectingModalVisible && !isRunning)) {
                        ViraxConnectingDialog(
                            isSmartPinging = uiState.isSmartConnecting,
                            onCancel = {
                                isConnectingModalVisible = false
                                if (uiState.isSmartConnecting || isTesting) {
                                    onAction(MainAction.CancelTesting)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViraxLinkInputDialog(
    title: String,
    message: String?,
    dismissLabel: String,
    initialMode: ViraxImportMode,
    onImportClipboard: () -> Unit,
    onConfirmLink: (String) -> Unit,
    onConfirmDedicatedCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ViraxTheme.colors
    var mode by rememberSaveable(initialMode) { mutableStateOf(initialMode) }
    var linkText by rememberSaveable { mutableStateOf("") }
    var codeText by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackgroundElevated,
        titleContentColor = colors.textPrimary,
        title = {
            Text(
                text = if (mode == ViraxImportMode.DEDICATED_CODE && initialMode == ViraxImportMode.DEDICATED_CODE && message == null) {
                    stringResource(R.string.virax_dedicated_code_title)
                } else {
                    title
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Segmented Mode Toggle: Dedicated Code | Subscription / Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val codeSelected = mode == ViraxImportMode.DEDICATED_CODE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (codeSelected) colors.secondaryPurple else Color.Transparent)
                            .clickable { mode = ViraxImportMode.DEDICATED_CODE }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.virax_sub_add_mode_code),
                            color = if (codeSelected) Color.White else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!codeSelected) colors.secondaryPurple else Color.Transparent)
                            .clickable { mode = ViraxImportMode.SUBSCRIPTION_OR_LINK }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.virax_sub_add_mode_link),
                            color = if (!codeSelected) Color.White else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (mode == ViraxImportMode.DEDICATED_CODE) {
                    Text(
                        text = stringResource(R.string.virax_dedicated_code_desc),
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.virax_dedicated_code_hint),
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primaryPurple,
                            unfocusedBorderColor = colors.cardBorder,
                            cursorColor = colors.primaryPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onConfirmDedicatedCode(codeText) },
                        enabled = codeText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.virax_dedicated_code_import_btn),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.virax_import_link_hint),
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primaryPurple,
                            unfocusedBorderColor = colors.cardBorder,
                            cursorColor = colors.primaryPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onImportClipboard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.virax_paste_clipboard_activate),
                            color = colors.primaryPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onConfirmLink(linkText) },
                        enabled = linkText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.virax_activate),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = colors.textSecondary)
            }
        }
    )
}
