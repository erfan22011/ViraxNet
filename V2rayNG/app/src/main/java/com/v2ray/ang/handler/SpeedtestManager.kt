package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.IPAPIInfo
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

object SpeedtestManager {

    data class RemoteEndpointInfo(
        val country: String?,
        val ipAddress: String?,
    )

    /**
     * Measures the time taken to establish a TCP connection to a given URL and port.
     *
     * @param url The URL to connect to.
     * @param port The port to connect to.
     * @return The connection time in milliseconds, or -1 if the connection failed.
     */
    fun socketConnectTime(url: String, port: Int, timeoutMs: Int = 1500): Long {
        var socket: Socket? = null
        val start = System.currentTimeMillis()

        try {
            socket = Socket()
            socket.connect(InetSocketAddress(url, port), timeoutMs)

            return System.currentTimeMillis() - start
        } catch (e: UnknownHostException) {
            LogUtil.e(AppConfig.TAG, "Unknown host: $url", e)
        } catch (e: IOException) {
            LogUtil.e(AppConfig.TAG, "socketConnectTime IOException: ${e.message}")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to establish socket connection to $url:$port", e)
        } finally {
            socket?.let { s ->
                try {
                    if (!s.isClosed) {
                        s.close()
                    }
                } catch (closeEx: IOException) {
                }
            }
        }
        return -1
    }

    private val FALLBACK_IP_API_URLS = listOf(
        "https://ipwho.is/",
        "https://api.ip.sb/geoip",
        "https://ipinfo.io/json",
        "https://api.myip.com",
    )

    internal fun parseRemoteEndpointInfo(content: String?): RemoteEndpointInfo? {
        if (content.isNullOrBlank()) return null
        val ipInfo = JsonUtil.fromJsonSafe(content, IPAPIInfo::class.java) ?: return null

        val ip = listOf(
            ipInfo.ip,
            ipInfo.clientIp,
            ipInfo.ip_addr,
            ipInfo.query
        ).firstOrNull { !it.isNullOrBlank() }

        val country = listOf(
            ipInfo.country_code,
            ipInfo.countryCode,
            ipInfo.country,
            ipInfo.location?.country_code
        ).firstOrNull { !it.isNullOrBlank() }

        if (ip.isNullOrBlank() && country.isNullOrBlank()) return null
        return RemoteEndpointInfo(
            country = country,
            ipAddress = ip,
        )
    }

    fun getRemoteIPInfo(): RemoteEndpointInfo? {
        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()
        val httpPort = SettingsManager.getHttpPort()
        if (httpPort == 0) return null

        val configuredUrl = MmkvManager.decodeSettingsString(AppConfig.PREF_IP_API_URL)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val candidateUrls = buildList {
            if (configuredUrl != null) add(configuredUrl)
            add(AppConfig.IP_API_URL)
            addAll(FALLBACK_IP_API_URLS)
        }.distinct()

        for (candidateUrl in candidateUrls) {
            val content = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = candidateUrl,
                    timeout = 4500,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                )
            )
            val parsed = parseRemoteEndpointInfo(content)
            if (parsed != null && !parsed.ipAddress.isNullOrBlank()) {
                return parsed
            }
        }
        return null
    }
}
