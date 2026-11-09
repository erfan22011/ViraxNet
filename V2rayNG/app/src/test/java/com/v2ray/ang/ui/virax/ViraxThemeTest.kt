package com.v2ray.ang.ui.virax

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
        assertEquals(0xFF0C0A19.toInt(), ViraxDarkPalette.background.value.toInt())
        // Light palette uses light background
        assertEquals(0xFFF6F5FC.toInt(), ViraxLightPalette.background.value.toInt())
    }

    @Test
    fun testLanguageDirection() {
        val rtlLanguages = setOf("fa", "ar", "ur", "he")
        assertTrue("fa" in rtlLanguages)
        assertFalse("en" in rtlLanguages)
    }
}
