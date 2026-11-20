package com.v2ray.ang.ui.virax

import com.v2ray.ang.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ViraxSpeedtestManagerTest {

    @Test
    fun speedtestResult_serializationAndDeserialization_isPreserved() {
        val result = ViraxSpeedtestResult(
            id = UUID.randomUUID().toString(),
            timestamp = 1732123456789L,
            pingMs = 45L,
            jitterMs = 4L,
            downloadMbps = 35.8,
            uploadMbps = 12.4
        )

        val json = JsonUtil.toJson(listOf(result))
        assertNotNull(json)

        val parsed = JsonUtil.fromJsonSafe(json, Array<ViraxSpeedtestResult>::class.java)?.toList()
        assertNotNull(parsed)
        assertEquals(1, parsed!!.size)
        assertEquals(result.id, parsed[0].id)
        assertEquals(45L, parsed[0].pingMs)
        assertEquals(4L, parsed[0].jitterMs)
        assertEquals(35.8, parsed[0].downloadMbps, 0.01)
        assertEquals(12.4, parsed[0].uploadMbps, 0.01)
    }

    @Test
    fun speedtestHistory_capsAtFiveResults() {
        val results = (1..10).map { i ->
            ViraxSpeedtestResult(
                id = "test-$i",
                timestamp = System.currentTimeMillis() + i * 1000,
                pingMs = (20 + i).toLong(),
                jitterMs = 2L,
                downloadMbps = 10.0 + i,
                uploadMbps = 5.0 + i
            )
        }

        var history = emptyList<ViraxSpeedtestResult>()
        results.forEach { r ->
            history = (listOf(r) + history.filter { it.id != r.id }).take(5)
        }

        assertEquals(5, history.size)
        assertEquals("test-10", history.first().id)
        assertEquals("test-6", history.last().id)
    }
}
