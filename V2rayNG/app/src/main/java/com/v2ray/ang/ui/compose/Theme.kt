package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.virax.LocalViraxColors
import com.v2ray.ang.ui.virax.ViraxDarkPalette
import com.v2ray.ang.ui.virax.ViraxLightPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF7C3AED), // Virax Primary Purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF1E1B2E),
    secondary = Color(0xFF6D28D9), // Virax Secondary Purple
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF3B1874),
    tertiary = Color(0xFF059669), // Virax Green Neon
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF6F5FC), // Virax Light Background
    onBackground = Color(0xFF1E1B2E),
    surface = Color(0xFFF6F5FC),
    onSurface = Color(0xFF1E1B2E),
    surfaceVariant = Color(0xFFEDEAF8),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFC4B5FD),
    outlineVariant = Color(0xFFE2DCF5),
    inverseSurface = Color(0xFF1E1A38),
    inverseOnSurface = Color(0xFFF6F5FC),
    inversePrimary = Color(0xFFA78BFA),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF7C3AED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F8FF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEDEAF8),
    surfaceContainerHighest = Color(0xFFE2DCF5),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF8B5CF6), // Virax Dark Primary Purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3B1874),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFA78BFA), // Virax Light Purple Accent
    onSecondary = Color(0xFF16132A),
    secondaryContainer = Color(0xFF2B2353),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF10B981), // Virax Green Neon
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF450A0A),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF0C0A19), // Virax Dark Background
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0C0A19),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1A38),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF3B2E6B),
    outlineVariant = Color(0xFF2B2353),
    inverseSurface = Color(0xFFF6F5FC),
    inverseOnSurface = Color(0xFF16132A),
    inversePrimary = Color(0xFF6D28D9),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF8B5CF6),
    surfaceContainerLowest = Color(0xFF070510),
    surfaceContainerLow = Color(0xFF120F24),
    surfaceContainer = Color(0xFF16132A),
    surfaceContainerHigh = Color(0xFF1E1A38),
    surfaceContainerHighest = Color(0xFF28224A),
)

// Semantic Colors
val colorPing = Color(0xFF10B981) // Virax Green
val colorPingRed = Color(0xFFEF4444) // Virax Red
val colorConfigType = Color(0xFF8B5CF6) // Virax Purple
val colorFabActive = Color(0xFF7C3AED) // Virax Purple
val colorFabInactiveLight = Color(0xFF9CA3AF)
val colorFabInactiveDark = Color(0xFF6B7280)
val dividerColorLight = Color(0xFFE2DCF5)
val dividerColorDark = Color(0xFF2B2353)

// Toast Colors 85%
val toastNormalBgLight = Color(0xD916132A)
val toastNormalBgDark = Color(0xD91E1A38)
val toastSuccessBg = Color(0xD9059669)
val toastErrorBg = Color(0xD9DC2626)
val toastInfoBg = Color(0xD96D28D9)
val toastIconCircleBg = Color(0x33FFFFFF)
val toastTextColor = Color.White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val viraxPalette = if (darkTheme) ViraxDarkPalette else ViraxLightPalette

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalViraxColors provides viraxPalette,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppSnackbarBridge(controller = snackbarController)
                    content()
                    AppSnackbarHost(hostState = snackbarController.hostState)
                }
            }
        }
    }
}
