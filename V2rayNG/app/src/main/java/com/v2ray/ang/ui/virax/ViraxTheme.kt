package com.v2ray.ang.ui.virax

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ViraxPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundDarker: Color,
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    val cardBorder: Color,
    val primaryPurple: Color,
    val secondaryPurple: Color,
    val darkPurple: Color,
    val lightPurple: Color,
    val gold: Color,
    val goldDark: Color,
    val goldLight: Color,
    val greenNeon: Color,
    val greenGlow: Color,
    val greenDark: Color,
    val disconnectRed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val buttonGradient: Brush,
    val cardGradient: Brush,
    val powerButtonGradient: Brush,
    val powerButtonOuterGlow: Color,
    val powerButtonMiddleGlow: Color,
    val powerButtonRingBorder: Color,
    val connectedShieldGradient: Brush,
    val connectedShieldOuterGlow: Color,
    val bottomBarBackground: Color
)

val ViraxDarkPalette = ViraxPalette(
    isDark = true,
    background = Color(0xFF0C0A19),
    backgroundDarker = Color(0xFF070510),
    cardBackground = Color(0xFF16132A),
    cardBackgroundElevated = Color(0xFF1E1A38),
    cardBorder = Color(0xFF2B2353),
    primaryPurple = Color(0xFF8B5CF6),
    secondaryPurple = Color(0xFF6D28D9),
    darkPurple = Color(0xFF3B1874),
    lightPurple = Color(0xFFA78BFA),
    gold = Color(0xFFFBBF24),
    goldDark = Color(0xFFD97706),
    goldLight = Color(0xFFFDE68A),
    greenNeon = Color(0xFF10B981),
    greenGlow = Color(0xFF34D399),
    greenDark = Color(0xFF064E3B),
    disconnectRed = Color(0xFFEF4444),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280),
    buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
    ),
    cardGradient = Brush.verticalGradient(
        listOf(Color(0xFF1B1735), Color(0xFF120F24))
    ),
    powerButtonGradient = Brush.radialGradient(
        listOf(Color(0xFF6D28D9), Color(0xFF4C1D95), Color(0xFF1E0E45))
    ),
    powerButtonOuterGlow = Color(0xFF281454),
    powerButtonMiddleGlow = Color(0xFF140C2E),
    powerButtonRingBorder = Color(0xFF381F6D),
    connectedShieldGradient = Brush.radialGradient(
        listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
    ),
    connectedShieldOuterGlow = Color(0xFF064E3B),
    bottomBarBackground = Color(0xFF070510)
)

val ViraxLightPalette = ViraxPalette(
    isDark = false,
    background = Color(0xFFF6F5FC),
    backgroundDarker = Color(0xFFEDEAF8),
    cardBackground = Color(0xFFFFFFFF),
    cardBackgroundElevated = Color(0xFFF9F8FF),
    cardBorder = Color(0xFFE2DCF5),
    primaryPurple = Color(0xFF7C3AED),
    secondaryPurple = Color(0xFF6D28D9),
    darkPurple = Color(0xFFEDE9FE),
    lightPurple = Color(0xFF7C3AED),
    gold = Color(0xFFD97706),
    goldDark = Color(0xFFB45309),
    goldLight = Color(0xFFFBBF24),
    greenNeon = Color(0xFF059669),
    greenGlow = Color(0xFF10B981),
    greenDark = Color(0xFFD1FAE5),
    disconnectRed = Color(0xFFDC2626),
    textPrimary = Color(0xFF1E1B2E),
    textSecondary = Color(0xFF6B7280),
    textMuted = Color(0xFF9CA3AF),
    buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
    ),
    cardGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF9F8FF))
    ),
    powerButtonGradient = Brush.radialGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D28D9))
    ),
    powerButtonOuterGlow = Color(0xFFEDE9FE),
    powerButtonMiddleGlow = Color(0xFFDDD6FE),
    powerButtonRingBorder = Color(0xFFC4B5FD),
    connectedShieldGradient = Brush.radialGradient(
        listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857))
    ),
    connectedShieldOuterGlow = Color(0xFFD1FAE5),
    bottomBarBackground = Color(0xFFFFFFFF)
)

val LocalViraxColors = staticCompositionLocalOf { ViraxDarkPalette }

object ViraxTheme {
    val colors: ViraxPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalViraxColors.current
}

/**
 * Virax Theme provider supporting:
 * - "0": System Default
 * - "1": Light
 * - "2": Dark
 */
@Composable
fun ViraxThemeProvider(
    themeMode: String = "2", // default dark
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "1" -> false
        "2" -> true
        else -> systemIsDark
    }

    val palette = if (isDark) ViraxDarkPalette else ViraxLightPalette

    CompositionLocalProvider(
        LocalViraxColors provides palette,
        content = content
    )
}
