package com.v2ray.ang.ui.virax

import androidx.annotation.DrawableRes
import com.v2ray.ang.R
import java.util.Locale

object ViraxFlagUtils {

    @DrawableRes
    fun getFlagDrawable(name: String?): Int {
        if (name.isNullOrBlank()) return R.drawable.ic_virax_flag_default

        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("germany") || lower.contains("frankfurt") || lower.contains("berlin") || Regex("(?:^|[^a-z])de(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_de

            lower.contains("netherlands") || lower.contains("amsterdam") || Regex("(?:^|[^a-z])nl(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_nl

            lower.contains("singapore") || Regex("(?:^|[^a-z])sg(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_sg

            lower.contains("turkey") || lower.contains("istanbul") || Regex("(?:^|[^a-z])tr(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_tr

            lower.contains("united states") || lower.contains("usa") || lower.contains("new york") || Regex("(?:^|[^a-z])us(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_us

            lower.contains("japan") || lower.contains("tokyo") || Regex("(?:^|[^a-z])jp(?:$|[^a-z])").containsMatchIn(lower) ->
                R.drawable.ic_virax_flag_jp

            else -> R.drawable.ic_virax_flag_default
        }
    }
}
