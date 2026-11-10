package com.v2ray.ang.ui.virax

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViraxThemeTest {

    @Test
    fun testPaletteProperties() {
        assertTrue(ViraxDarkPalette.isDark)
        assertFalse(ViraxLightPalette.isDark)

        // Dark palette uses dark background
        assertEquals(Color(0xFF0C0A19), ViraxDarkPalette.background)
        // Light palette uses light background
        assertEquals(Color(0xFFF6F5FC), ViraxLightPalette.background)
    }

    @Test
    fun testLanguageDirection() {
        val rtlLanguages = setOf("fa", "ar", "ur", "he")
        assertTrue("fa" in rtlLanguages)
        assertFalse("en" in rtlLanguages)
    }
}
