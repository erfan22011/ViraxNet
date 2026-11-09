package com.v2ray.ang.ui.virax

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViraxDateUtilsTest {

    @Test
    fun testGregorianToJalaliKnownDates() {
        // Test Gregorian 2026-03-21 -> Jalali 1405-01-01 (Nowruz)
        val nowruz = ViraxDateUtils.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, nowruz.year)
        assertEquals(1, nowruz.month)
        assertEquals(1, nowruz.day)
        assertEquals("1405/01/01", nowruz.format())

        // Test Gregorian 2026-10-17 -> Jalali 1405-07-25 (Matches image example!)
        val exampleDate = ViraxDateUtils.gregorianToJalali(2026, 10, 17)
        assertEquals(1405, exampleDate.year)
        assertEquals(7, exampleDate.month)
        assertEquals(25, exampleDate.day)
        assertEquals("1405/07/25", exampleDate.format())
    }

    @Test
    fun testRemainingDaysCalculation() {
        val now = 1773000000000L // arbitrary fixed time
        val futureDays = 29L
        val futureTime = now + (futureDays * 24 * 60 * 60 * 1000L) + 5000L
        assertEquals(29L, ViraxDateUtils.remainingDays(futureTime, now))

        // Expired date
        val pastTime = now - 10000L
        assertEquals(0L, ViraxDateUtils.remainingDays(pastTime, now))

        // Invalid
        assertEquals(-1L, ViraxDateUtils.remainingDays(0L, now))
    }

    @Test
    fun testTimestampToJalali() {
        // Zero or negative
        assertEquals("", ViraxDateUtils.timestampToJalali(0L))
        assertEquals("", ViraxDateUtils.timestampToJalali(-100L))

        // Fixed UTC timestamp for 2026-10-17 12:00:00 UTC -> Jalali 1405/07/25
        val epochSeconds = 1792238400L // 2026-10-17
        val formattedFromSeconds = ViraxDateUtils.timestampToJalali(epochSeconds)
        assertEquals("1405/07/25", formattedFromSeconds)

        val formattedFromMillis = ViraxDateUtils.timestampToJalali(epochSeconds * 1000L)
        assertEquals("1405/07/25", formattedFromMillis)
    }
}
