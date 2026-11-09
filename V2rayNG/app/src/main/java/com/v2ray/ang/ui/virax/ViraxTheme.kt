package com.v2ray.ang.ui.virax

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ViraxColors {
    val Background = Color(0xFF0C0A19)
    val BackgroundDarker = Color(0xFF070510)
    val CardBackground = Color(0xFF16132A)
    val CardBackgroundElevated = Color(0xFF1E1A38)
    val CardBorder = Color(0xFF2B2353)

    val PurplePrimary = Color(0xFF8B5CF6)
    val PurpleGlow = Color(0xFF7C3AED)
    val PurpleDark = Color(0xFF3B1874)
    val PurpleLight = Color(0xFFA78BFA)
    val PurpleSecondary = Color(0xFF6D28D9)

    val Gold = Color(0xFFFBBF24)
    val GoldDark = Color(0xFFD97706)
    val GoldLight = Color(0xFFFDE68A)

    val GreenNeon = Color(0xFF10B981)
    val GreenGlow = Color(0xFF34D399)
    val GreenDark = Color(0xFF064E3B)

    val DisconnectRed = Color(0xFFEF4444)
    val TextWhite = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9CA3AF)
    val TextMuted = Color(0xFF6B7280)

    val PurpleButtonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
    )

    val GoldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
    )

    val CardGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1B1735), Color(0xFF120F24))
    )

    val PowerButtonGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6), Color(0xFF2E1065))
    )

    val ConnectedShieldGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B))
    )
}
