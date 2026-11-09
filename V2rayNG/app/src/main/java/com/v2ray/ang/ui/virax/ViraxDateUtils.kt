package com.v2ray.ang.ui.virax

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ViraxDateUtils {

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        fun format(): String = String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
    }

    /**
     * Converts a Unix timestamp (in seconds or milliseconds) to a formatted Jalali date (e.g., 1405/07/25).
     */
    fun timestampToJalali(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val millis = if (timestamp < 100_000_000_000L) timestamp * 1000 else timestamp
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gYear, gMonth, gDay).format()
    }

    /**
     * Calculates remaining days from now until expireTimestamp.
     * Returns 0 if already expired, or negative if invalid.
     */
    fun remainingDays(expireTimestamp: Long, currentTimeMillis: Long = System.currentTimeMillis()): Long {
        if (expireTimestamp <= 0) return -1
        val expireMillis = if (expireTimestamp < 100_000_000_000L) expireTimestamp * 1000 else expireTimestamp
        val diff = expireMillis - currentTimeMillis
        if (diff <= 0) return 0
        return diff / (1000L * 60 * 60 * 24)
    }

    /**
     * Converts Gregorian Year, Month, Day into Jalali Date.
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i + 1]
        }
        val isLeapGregorian = (gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)
        if (gm > 1 && isLeapGregorian) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0 until 11) {
            if (jDayNo < jDaysInMonth[i + 1]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i + 1]
        }
        if (jm == 0) {
            jm = 12
        }
        val jd = jDayNo + 1
        return JalaliDate(jy, jm, jd)
    }
}
