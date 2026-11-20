package com.v2ray.ang.ui.virax

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class ViraxSpeedtestResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val pingMs: Long,
    val jitterMs: Long,
    val downloadMbps: Double,
    val uploadMbps: Double
)

sealed class SpeedtestPhase {
    object Idle : SpeedtestPhase()
    data class Latency(val pingMs: Long, val jitterMs: Long, val sample: Int) : SpeedtestPhase()
    data class Download(val mbps: Double, val progress: Float, val pingMs: Long, val jitterMs: Long) : SpeedtestPhase()
    data class Upload(val mbps: Double, val progress: Float, val pingMs: Long, val jitterMs: Long, val downloadMbps: Double) : SpeedtestPhase()
    data class Finished(val result: ViraxSpeedtestResult) : SpeedtestPhase()
    data class Error(val message: String) : SpeedtestPhase()
}

object ViraxSpeedtestManager {

    private const val CLOUDFLARE_PING_URL = "https://speed.cloudflare.com/__down?bytes=0"
    private const val FALLBACK_PING_URL = "https://api.ip.sb/geoip"
    private const val CLOUDFLARE_DOWN_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
    private const val CLOUDFLARE_UP_URL = "https://speed.cloudflare.com/__up"

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getHistory(): List<ViraxSpeedtestResult> {
        return try {
            val json = MmkvManager.decodeSettingsString(AppConfig.PREF_SPEEDTEST_HISTORY)
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                JsonUtil.fromJsonSafe(json, Array<ViraxSpeedtestResult>::class.java)?.toList() ?: emptyList()
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to read speedtest history", e)
            emptyList()
        }
    }

    fun saveResult(result: ViraxSpeedtestResult) {
        try {
            val existing = getHistory()
            val updated = (listOf(result) + existing.filter { it.id != result.id }).take(5)
            MmkvManager.encodeSettings(AppConfig.PREF_SPEEDTEST_HISTORY, JsonUtil.toJson(updated))
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to save speedtest result", e)
        }
    }

    fun clearHistory() {
        try {
            MmkvManager.encodeSettings(AppConfig.PREF_SPEEDTEST_HISTORY, "[]")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to clear speedtest history", e)
        }
    }

    suspend fun runSpeedtest(onPhaseUpdate: (SpeedtestPhase) -> Unit): ViraxSpeedtestResult? = withContext(Dispatchers.IO) {
        try {
            // 1. Latency & Jitter Test (4 samples)
            val samples = mutableListOf<Long>()
            val pingUrl = CLOUDFLARE_PING_URL

            for (i in 1..4) {
                if (!isActive) return@withContext null
                val start = System.currentTimeMillis()
                try {
                    val req = Request.Builder()
                        .url(pingUrl)
                        .get()
                        .build()
                    httpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val elapsed = System.currentTimeMillis() - start
                            samples.add(elapsed)
                        } else {
                            val fallbackStart = System.currentTimeMillis()
                            val fallbackReq = Request.Builder().url(FALLBACK_PING_URL).get().build()
                            httpClient.newCall(fallbackReq).execute().use { fbResp ->
                                if (fbResp.isSuccessful) {
                                    samples.add(System.currentTimeMillis() - fallbackStart)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    try {
                        val fallbackStart = System.currentTimeMillis()
                        val fallbackReq = Request.Builder().url(FALLBACK_PING_URL).get().build()
                        httpClient.newCall(fallbackReq).execute().use { fbResp ->
                            if (fbResp.isSuccessful) {
                                samples.add(System.currentTimeMillis() - fallbackStart)
                            }
                        }
                    } catch (_: Exception) {}
                }

                val currentPing = if (samples.isNotEmpty()) samples.average().toLong() else 0L
                val currentJitter = if (samples.size > 1) {
                    samples.zipWithNext { a, b -> abs(a - b) }.average().toLong()
                } else 0L

                onPhaseUpdate(SpeedtestPhase.Latency(currentPing, currentJitter, i))
            }

            val finalPing = if (samples.isNotEmpty()) samples.average().toLong() else 99L
            val finalJitter = if (samples.size > 1) {
                samples.zipWithNext { a, b -> abs(a - b) }.average().toLong()
            } else 5L

            // 2. Download Speed Test (up to 6.5 seconds)
            var finalDownloadMbps = 0.0
            if (isActive) {
                val downReq = Request.Builder()
                    .url(CLOUDFLARE_DOWN_URL)
                    .get()
                    .build()

                try {
                    httpClient.newCall(downReq).execute().use { response ->
                        val body = response.body
                        if (response.isSuccessful && body != null) {
                            val stream: InputStream = body.byteStream()
                            val buffer = ByteArray(16 * 1024)
                            var totalBytesRead = 0L
                            val testStartTime = System.currentTimeMillis()
                            var lastWindowTime = testStartTime
                            var lastWindowBytes = 0L
                            val maxDurationMs = 6500L
                            val speedSamples = mutableListOf<Double>()

                            while (isActive) {
                                val read = stream.read(buffer)
                                if (read == -1) break
                                totalBytesRead += read
                                val now = System.currentTimeMillis()
                                val elapsedTotal = now - testStartTime

                                if (now - lastWindowTime >= 150) {
                                    val windowBytes = totalBytesRead - lastWindowBytes
                                    val windowSec = (now - lastWindowTime) / 1000.0
                                    val instantaneousMbps = (windowBytes * 8.0) / (windowSec * 1_000_000.0)
                                    speedSamples.add(instantaneousMbps)

                                    val progress = (elapsedTotal.toFloat() / maxDurationMs).coerceIn(0f, 1f)
                                    val displayMbps = (speedSamples.takeLast(4).average())
                                    onPhaseUpdate(SpeedtestPhase.Download(displayMbps, progress, finalPing, finalJitter))

                                    lastWindowTime = now
                                    lastWindowBytes = totalBytesRead
                                }

                                if (elapsedTotal >= maxDurationMs) {
                                    break
                                }
                            }

                            finalDownloadMbps = if (speedSamples.isNotEmpty()) {
                                speedSamples.sorted().let { sorted ->
                                    val idx = (sorted.size * 0.85).toInt().coerceAtMost(sorted.lastIndex)
                                    sorted[idx]
                                }
                            } else {
                                val totalSec = (System.currentTimeMillis() - testStartTime) / 1000.0
                                if (totalSec > 0) (totalBytesRead * 8.0) / (totalSec * 1_000_000.0) else 0.0
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.w(AppConfig.TAG, "Download speed test partial: ${e.message}")
                }
            }

            if (!isActive) return@withContext null

            // 3. Upload Speed Test (up to 5.5 seconds)
            var finalUploadMbps = 0.0
            if (isActive) {
                val dummyChunk = ByteArray(32 * 1024)
                val uploadStartTime = System.currentTimeMillis()
                val maxUploadDurationMs = 5500L
                val uploadSamples = mutableListOf<Double>()
                var totalBytesUploaded = 0L
                var lastWindowTime = uploadStartTime
                var lastWindowBytes = 0L

                val requestBody = object : RequestBody() {
                    override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

                    override fun writeTo(sink: BufferedSink) {
                        while (isActive && (System.currentTimeMillis() - uploadStartTime) < maxUploadDurationMs) {
                            sink.write(dummyChunk)
                            sink.flush()
                            totalBytesUploaded += dummyChunk.size
                            val now = System.currentTimeMillis()
                            val elapsedTotal = now - uploadStartTime

                            if (now - lastWindowTime >= 150) {
                                val windowBytes = totalBytesUploaded - lastWindowBytes
                                val windowSec = (now - lastWindowTime) / 1000.0
                                val instantaneousMbps = (windowBytes * 8.0) / (windowSec * 1_000_000.0)
                                uploadSamples.add(instantaneousMbps)

                                val progress = (elapsedTotal.toFloat() / maxUploadDurationMs).coerceIn(0f, 1f)
                                val displayMbps = uploadSamples.takeLast(4).average()
                                onPhaseUpdate(
                                    SpeedtestPhase.Upload(
                                        mbps = displayMbps,
                                        progress = progress,
                                        pingMs = finalPing,
                                        jitterMs = finalJitter,
                                        downloadMbps = finalDownloadMbps
                                    )
                                )

                                lastWindowTime = now
                                lastWindowBytes = totalBytesUploaded
                            }
                        }
                    }
                }

                try {
                    val upReq = Request.Builder()
                        .url(CLOUDFLARE_UP_URL)
                        .post(requestBody)
                        .build()
                    httpClient.newCall(upReq).execute().use { /* upload executed */ }
                } catch (e: Exception) {
                    LogUtil.w(AppConfig.TAG, "Upload speed test partial: ${e.message}")
                }

                finalUploadMbps = if (uploadSamples.isNotEmpty()) {
                    uploadSamples.sorted().let { sorted ->
                        val idx = (sorted.size * 0.85).toInt().coerceAtMost(sorted.lastIndex)
                        sorted[idx]
                    }
                } else {
                    val totalSec = (System.currentTimeMillis() - uploadStartTime) / 1000.0
                    if (totalSec > 0) (totalBytesUploaded * 8.0) / (totalSec * 1_000_000.0) else 0.0
                }
            }

            if (!isActive) return@withContext null

            // 4. Completed
            val result = ViraxSpeedtestResult(
                pingMs = finalPing,
                jitterMs = finalJitter,
                downloadMbps = (finalDownloadMbps * 10).toLong() / 10.0,
                uploadMbps = (finalUploadMbps * 10).toLong() / 10.0
            )
            saveResult(result)
            onPhaseUpdate(SpeedtestPhase.Finished(result))
            return@withContext result
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Speedtest failed", e)
            onPhaseUpdate(SpeedtestPhase.Error(e.localizedMessage ?: "Test failed"))
            return@withContext null
        }
    }
}
