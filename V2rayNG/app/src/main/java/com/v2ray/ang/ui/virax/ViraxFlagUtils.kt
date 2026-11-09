package com.v2ray.ang.ui.virax

import java.util.Locale

object ViraxFlagUtils {

    private val countryMap = mapOf(
        "germany" to "🇩🇪", "frankfurt" to "🇩🇪", "berlin" to "🇩🇪", "de" to "🇩🇪",
        "netherlands" to "🇳🇱", "amsterdam" to "🇳🇱", "nl" to "🇳🇱",
        "singapore" to "🇸🇬", "sg" to "🇸🇬",
        "turkey" to "🇹🇷", "istanbul" to "🇹🇷", "tr" to "🇹🇷",
        "united states" to "🇺🇸", "usa" to "🇺🇸", "us" to "🇺🇸", "new york" to "🇺🇸", "los angeles" to "🇺🇸",
        "japan" to "🇯🇵", "tokyo" to "🇯🇵", "jp" to "🇯🇵",
        "united kingdom" to "🇬🇧", "uk" to "🇬🇧", "london" to "🇬🇧", "gb" to "🇬🇧",
        "france" to "🇫🇷", "paris" to "🇫🇷", "fr" to "🇫🇷",
        "canada" to "🇨🇦", "ca" to "🇨🇦", "toronto" to "🇨🇦",
        "finland" to "🇫🇮", "helsinki" to "🇫🇮", "fi" to "🇫🇮",
        "sweden" to "🇸🇪", "stockholm" to "🇸🇪", "se" to "🇸🇪",
        "switzerland" to "🇨🇭", "zurich" to "🇨🇭", "ch" to "🇨🇭",
        "iran" to "🇮🇷", "tehran" to "🇮🇷", "ir" to "🇮🇷",
        "uae" to "🇦🇪", "dubai" to "🇦🇪", "ae" to "🇦🇪",
        "india" to "🇮🇳", "mumbai" to "🇮🇳", "in" to "🇮🇳",
        "korea" to "🇰🇷", "seoul" to "🇰🇷", "kr" to "🇰🇷"
    )

    fun getFlagEmoji(name: String?): String {
        if (name.isNullOrBlank()) return "🌐"

        // If the name already contains a flag emoji, preserve it
        for (char in name) {
            val type = Character.getType(char)
            if (type == Character.OTHER_SYMBOL.toInt() || type == Character.SURROGATE.toInt()) {
                // Potential emoji
            }
        }

        val lower = name.lowercase(Locale.ROOT)
        for ((keyword, flag) in countryMap) {
            val regex = Regex("(?:^|[^a-z])$keyword(?:$|[^a-z])")
            if (regex.containsMatchIn(lower) || lower.contains(keyword)) {
                return flag
            }
        }
        return "🌐"
    }
}
