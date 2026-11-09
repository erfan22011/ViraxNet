package com.v2ray.ang.ui.virax

import com.v2ray.ang.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViraxFlagUtilsTest {

    @Test
    fun testCountryFlagDetection() {
        assertEquals(R.drawable.ic_virax_flag_de, ViraxFlagUtils.getFlagDrawable("Germany - Frankfurt"))
        assertEquals(R.drawable.ic_virax_flag_de, ViraxFlagUtils.getFlagDrawable("DE Fast Server"))
        assertEquals(R.drawable.ic_virax_flag_nl, ViraxFlagUtils.getFlagDrawable("Netherlands - Amsterdam"))
        assertEquals(R.drawable.ic_virax_flag_sg, ViraxFlagUtils.getFlagDrawable("Singapore - SG"))
        assertEquals(R.drawable.ic_virax_flag_tr, ViraxFlagUtils.getFlagDrawable("Turkey - Istanbul"))
        assertEquals(R.drawable.ic_virax_flag_us, ViraxFlagUtils.getFlagDrawable("United States - NY"))
        assertEquals(R.drawable.ic_virax_flag_jp, ViraxFlagUtils.getFlagDrawable("Japan - Tokyo"))
        assertEquals(R.drawable.ic_virax_flag_default, ViraxFlagUtils.getFlagDrawable("Unknown Node 123"))
        assertEquals(R.drawable.ic_virax_flag_default, ViraxFlagUtils.getFlagDrawable(null))
        assertEquals(R.drawable.ic_virax_flag_default, ViraxFlagUtils.getFlagDrawable(""))
    }
}
