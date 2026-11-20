package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

object ViraxInAppUpdater {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (UpdateDownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "ViraxNet-$versionName.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "ViraxNet-App/${versionName}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP error: ${response.code}"
                onProgress(UpdateDownloadState.Error(errorMsg))
                return@withContext null
            }

            val body = response.body ?: run {
                onProgress(UpdateDownloadState.Error("Empty response body"))
                return@withContext null
            }

            val totalBytes = body.contentLength()
            inputStream = body.byteStream()
            outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastReportPercent = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val percent = if (totalBytes > 0) {
                    ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                    -1
                }

                if (percent != lastReportPercent) {
                    lastReportPercent = percent
                    onProgress(UpdateDownloadState.Downloading(percent, totalRead, totalBytes))
                }
            }

            outputStream.flush()
            onProgress(UpdateDownloadState.ReadyToInstall(apkFile))
            return@withContext apkFile
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to download update APK", e)
            onProgress(UpdateDownloadState.Error(e.localizedMessage ?: "Download failed"))
            return@withContext null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to open install permission settings", e)
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, R.string.toast_failure, Toast.LENGTH_SHORT).show()
            return
        }

        if (!canInstallPackages(context)) {
            Toast.makeText(context, R.string.virax_update_grant_install_permission, Toast.LENGTH_LONG).show()
            requestInstallPermission(context)
            return
        }

        try {
            val authority = "${context.packageName}.cache"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to launch package installer", e)
            Toast.makeText(context, e.localizedMessage ?: "Installation failed", Toast.LENGTH_SHORT).show()
        }
    }
}
