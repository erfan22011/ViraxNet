package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.main.MainAction
import com.v2ray.ang.ui.main.MainDestination
import com.v2ray.ang.ui.main.MainDrawerContent
import com.v2ray.ang.ui.main.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ViraxMainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val isTesting = uiState.isTesting
    val selectedGuid = uiState.selectedGuid

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ViraxTab.HOME) }
    var isConnectingModalVisible by remember { mutableStateOf(false) }

    // Theme mode: "0" = System, "1" = Light, "2" = Dark
    val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()

    // Language code: "auto", "fa", "en"
    var languageCode by remember {
        mutableStateOf(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE, "auto") ?: "auto"
        )
    }

    // Determine layout direction
    val layoutDirection = when (languageCode) {
        "fa" -> LayoutDirection.Rtl
        "en" -> LayoutDirection.Ltr
        else -> {
            val defaultLang = Locale.getDefault().language
            if (defaultLang in setOf("fa", "ar", "ur", "he")) LayoutDirection.Rtl else LayoutDirection.Ltr
        }
    }

    // Core Settings direct bindings
    var localDns by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false))
    }
    var fakeDns by remember {
        mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED, false))
    }
    var pingUrl by remember {
        mutableStateOf(
            MmkvManager.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL, AppConfig.DELAY_TEST_URL)
                ?: AppConfig.DELAY_TEST_URL
        )
    }

    ViraxThemeProvider(themeMode = themeMode) {
        val colors = ViraxTheme.colors

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    MainDrawerContent(
                        drawerState = drawerState,
                        onNavigate = { route ->
                            scope.launch { drawerState.close() }
                            onNavigate(route)
                        }
                    )
                }
            ) {
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
                                    isConnecting = isTesting,
                                    serverName = "Germany - Frankfurt",
                                    serverPing = 12L,
                                    ipAddress = if (isRunning) "10.0.0.2" else null,
                                    connectionDurationText = if (isRunning) "00:12:34" else "00:00:00",
                                    downloadSpeedText = if (isRunning) "12.5 MB" else "0 B/s",
                                    uploadSpeedText = if (isRunning) "3.8 MB" else "0 B/s",
                                    onPowerClick = {
                                        if (!isRunning) {
                                            isConnectingModalVisible = true
                                        }
                                        onAction(MainAction.ToggleService)
                                    },
                                    onMenuClick = {
                                        scope.launch { drawerState.open() }
                                    },
                                    onCrownClick = { selectedTab = ViraxTab.STATS },
                                    onSmartSelectClick = { selectedTab = ViraxTab.SERVERS }
                                )
                            }

                            ViraxTab.SERVERS -> {
                                ViraxServersScreen(
                                    servers = listOf(
                                        ViraxServerItem("s1", "Germany - Frankfurt", 12L, selectedGuid == "s1"),
                                        ViraxServerItem("s2", "Netherlands - Amsterdam", 18L, selectedGuid == "s2"),
                                        ViraxServerItem("s3", "Singapore - SG", 22L, selectedGuid == "s3"),
                                        ViraxServerItem("s4", "Turkey - Istanbul", 27L, selectedGuid == "s4"),
                                        ViraxServerItem("s5", "United States - NY", 31L, selectedGuid == "s5"),
                                        ViraxServerItem("s6", "Japan - Tokyo", 34L, selectedGuid == "s6")
                                    ),
                                    selectedGuid = selectedGuid ?: "s1",
                                    onServerSelect = { guid ->
                                        onAction(MainAction.SelectServer(guid))
                                    },
                                    onAutoConnectClick = {
                                        selectedTab = ViraxTab.HOME
                                        onAction(MainAction.ToggleService)
                                    },
                                    onConnectSelectedClick = {
                                        selectedTab = ViraxTab.HOME
                                        onAction(MainAction.ToggleService)
                                    },
                                    onBackClick = { selectedTab = ViraxTab.HOME }
                                )
                            }

                            ViraxTab.STATS -> {
                                ViraxSubscriptionScreen(
                                    data = ViraxSubscriptionUiData(
                                        hasUserInfo = true,
                                        uploadBytes = 1_500_000_000L,
                                        downloadBytes = 3_200_000_000L,
                                        totalBytes = 50_000_000_000L,
                                        expireTimestamp = 1792195200L // 1405/07/25
                                    ),
                                    onManageSubscriptionClick = {
                                        onNavigate(MainDestination.Subscriptions)
                                    },
                                    onBackClick = { selectedTab = ViraxTab.HOME }
                                )
                            }

                            ViraxTab.SETTINGS -> {
                                ViraxSettingsScreen(
                                    localDnsEnabled = localDns,
                                    onLocalDnsToggled = { enabled ->
                                        localDns = enabled
                                        MmkvManager.encodeSettings(AppConfig.PREF_LOCAL_DNS_ENABLED, enabled)
                                    },
                                    fakeDnsEnabled = fakeDns,
                                    onFakeDnsToggled = { enabled ->
                                        fakeDns = enabled
                                        MmkvManager.encodeSettings(AppConfig.PREF_FAKE_DNS_ENABLED, enabled)
                                    },
                                    pingTestUrl = pingUrl,
                                    onPingTestUrlChanged = { newUrl ->
                                        pingUrl = newUrl
                                        MmkvManager.encodeSettings(AppConfig.PREF_DELAY_TEST_URL, newUrl)
                                    },
                                    currentThemeMode = themeMode,
                                    onThemeModeChanged = { mode ->
                                        ThemeManager.setThemeMode(mode)
                                    },
                                    currentLanguageCode = languageCode,
                                    onLanguageChanged = { code ->
                                        languageCode = code
                                        AppLocaleManager.setApplicationLanguage(code)
                                    },
                                    onRoutingSettingsClick = {
                                        onNavigate(MainDestination.Routing)
                                    },
                                    onBackClick = { selectedTab = ViraxTab.HOME }
                                )
                            }
                        }

                        if (isConnectingModalVisible && !isRunning) {
                            ViraxConnectingDialog(
                                onCancel = {
                                    isConnectingModalVisible = false
                                    if (isTesting) onAction(MainAction.CancelTesting)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
