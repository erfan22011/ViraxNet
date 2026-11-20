package com.v2ray.ang.ui.checkupdate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.handler.UpdateDownloadState
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.Utils

class CheckUpdateActivity : BaseComponentActivity() {

    private val viewModel: CheckUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.checkForUpdates()
        }
    }

    @Composable
    override fun ScreenContent() {
        CheckUpdateScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun CheckUpdateScreen(
    viewModel: CheckUpdateViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val checkPreRelease by viewModel.checkPreRelease.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.update_check_for_update),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSwitchItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.update_check_pre_release),
                checked = checkPreRelease,
                onCheckedChange = { viewModel.toggleCheckPreRelease(it) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_check_update_24dp),
                title = stringResource(R.string.update_check_for_update),
                onClick = { viewModel.checkForUpdates() }
            )
            VersionInfoBlock(versionText = versionText)
            NavigationBarsSpacer()
        }
    }

    if (showUpdateDialog && updateResult != null) {
        val result = updateResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Text(
                    when (downloadState) {
                        is UpdateDownloadState.Downloading -> stringResource(R.string.virax_update_downloading)
                        is UpdateDownloadState.ReadyToInstall -> stringResource(R.string.virax_update_ready_title)
                        is UpdateDownloadState.Error -> stringResource(R.string.virax_update_error_title)
                        else -> stringResource(R.string.update_new_version_found, result.latestVersion ?: "")
                    }
                )
            },
            text = {
                when (val state = downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.percent >= 0) {
                                LinearProgressIndicator(
                                    progress = { state.percent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val downloadedMb = String.format(java.util.Locale.US, "%.1f", state.downloadedBytes / (1024.0 * 1024.0))
                                val totalMb = if (state.totalBytes > 0) {
                                    String.format(java.util.Locale.US, "%.1f", state.totalBytes / (1024.0 * 1024.0))
                                } else "?"
                                Text(
                                    text = "${state.percent}% ($downloadedMb MB / $totalMb MB)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                val downloadedMb = String.format(java.util.Locale.US, "%.1f", state.downloadedBytes / (1024.0 * 1024.0))
                                Text(
                                    text = "$downloadedMb MB",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Text(
                            text = stringResource(R.string.virax_update_ready_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = stringResource(R.string.virax_update_failed_desc, state.message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        val scrollState = rememberScrollState()
                        Text(
                            text = result.releaseNotes.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .verticalScrollbar(scrollState)
                        )
                    }
                }
            },
            confirmButton = {
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        // In-progress
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        TextButton(onClick = { viewModel.installDownloadedApk(context) }) {
                            Text(stringResource(R.string.virax_update_install_now))
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        TextButton(onClick = {
                            result.downloadUrl?.let {
                                viewModel.startInAppDownload(context, it, result.latestVersion.orEmpty())
                            }
                        }) {
                            Text(stringResource(R.string.virax_update_retry))
                        }
                    }
                    else -> {
                        TextButton(onClick = {
                            val url = result.downloadUrl
                            if (!url.isNullOrEmpty()) {
                                viewModel.startInAppDownload(context, url, result.latestVersion.orEmpty())
                            }
                        }) {
                            Text(stringResource(R.string.update_now))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
