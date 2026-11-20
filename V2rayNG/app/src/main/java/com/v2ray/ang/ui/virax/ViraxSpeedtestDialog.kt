package com.v2ray.ang.ui.virax

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViraxSpeedtestDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors
    val scope = rememberCoroutineScope()

    var testJob by remember { mutableStateOf<Job?>(null) }
    var currentPhase by remember { mutableStateOf<SpeedtestPhase>(SpeedtestPhase.Idle) }
    var history by remember { mutableStateOf(ViraxSpeedtestManager.getHistory()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun startTest() {
        testJob?.cancel()
        currentPhase = SpeedtestPhase.Latency(0, 0, 1)
        testJob = scope.launch {
            val result = ViraxSpeedtestManager.runSpeedtest { phase ->
                currentPhase = phase
            }
            history = ViraxSpeedtestManager.getHistory()
        }
    }

    LaunchedEffect(Unit) {
        startTest()
    }

    val isTesting = currentPhase is SpeedtestPhase.Latency ||
            currentPhase is SpeedtestPhase.Download ||
            currentPhase is SpeedtestPhase.Upload

    AlertDialog(
        onDismissRequest = {
            testJob?.cancel()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(24.dp)),
        containerColor = colors.cardBackgroundElevated,
        titleContentColor = colors.textPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Official Speedtest Tag
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.primaryPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isTesting) colors.statusConnected else colors.primaryPurple)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SPEEDTEST®",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = {
                        testJob?.cancel()
                        onDismiss()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Live Gauge / Value Display Card
                val (displayValue, displayUnit, statusText) = when (val phase = currentPhase) {
                    is SpeedtestPhase.Latency -> Triple(
                        "${phase.pingMs}",
                        "ms",
                        stringResource(R.string.virax_speedtest_phase_ping)
                    )
                    is SpeedtestPhase.Download -> Triple(
                        String.format(Locale.US, "%.1f", phase.mbps),
                        "Mbps",
                        stringResource(R.string.virax_speedtest_phase_download)
                    )
                    is SpeedtestPhase.Upload -> Triple(
                        String.format(Locale.US, "%.1f", phase.mbps),
                        "Mbps",
                        stringResource(R.string.virax_speedtest_phase_upload)
                    )
                    is SpeedtestPhase.Finished -> Triple(
                        String.format(Locale.US, "%.1f", phase.result.downloadMbps),
                        "Mbps",
                        stringResource(R.string.virax_speedtest_phase_completed)
                    )
                    is SpeedtestPhase.Error -> Triple(
                        "-",
                        "",
                        stringResource(R.string.virax_speedtest_phase_failed)
                    )
                    else -> Triple("0.0", "Mbps", stringResource(R.string.virax_speedtest_phase_ready))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = statusText,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayValue,
                                color = colors.primaryPurple,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (displayUnit.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayUnit,
                                    color = colors.textSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        if (isTesting) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val progressValue = when (val p = currentPhase) {
                                is SpeedtestPhase.Download -> p.progress
                                is SpeedtestPhase.Upload -> p.progress
                                else -> 0f
                            }
                            if (progressValue > 0f) {
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = colors.primaryPurple,
                                    trackColor = colors.cardBorder
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = colors.primaryPurple,
                                    trackColor = colors.cardBorder
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Three Metrics Overview (Ping, Download, Upload)
                val pingVal = when (val p = currentPhase) {
                    is SpeedtestPhase.Latency -> "${p.pingMs} ms"
                    is SpeedtestPhase.Download -> "${p.pingMs} ms"
                    is SpeedtestPhase.Upload -> "${p.pingMs} ms"
                    is SpeedtestPhase.Finished -> "${p.result.pingMs} ms"
                    else -> "- ms"
                }

                val downVal = when (val p = currentPhase) {
                    is SpeedtestPhase.Download -> "${String.format(Locale.US, "%.1f", p.mbps)} Mbps"
                    is SpeedtestPhase.Upload -> "${String.format(Locale.US, "%.1f", p.downloadMbps)} Mbps"
                    is SpeedtestPhase.Finished -> "${String.format(Locale.US, "%.1f", p.result.downloadMbps)} Mbps"
                    else -> "- Mbps"
                }

                val upVal = when (val p = currentPhase) {
                    is SpeedtestPhase.Upload -> "${String.format(Locale.US, "%.1f", p.mbps)} Mbps"
                    is SpeedtestPhase.Finished -> "${String.format(Locale.US, "%.1f", p.result.uploadMbps)} Mbps"
                    else -> "- Mbps"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedtestMetricCard(
                        title = stringResource(R.string.virax_speedtest_metric_ping),
                        value = pingVal,
                        iconRes = R.drawable.ic_virax_clock,
                        modifier = Modifier.weight(1f)
                    )
                    SpeedtestMetricCard(
                        title = stringResource(R.string.virax_speedtest_metric_download),
                        value = downVal,
                        iconRes = R.drawable.ic_virax_download,
                        modifier = Modifier.weight(1f)
                    )
                    SpeedtestMetricCard(
                        title = stringResource(R.string.virax_speedtest_metric_upload),
                        value = upVal,
                        iconRes = R.drawable.ic_virax_upload,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // History Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.virax_speedtest_history_title),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete_24dp),
                                contentDescription = stringResource(R.string.virax_speedtest_clear_history),
                                tint = colors.statusDisconnected,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.virax_speedtest_no_history),
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
                        history.forEach { item ->
                            val dateStr = dateFormat.format(Date(item.timestamp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateStr,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "${item.pingMs}ms",
                                            color = colors.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "↓${item.downloadMbps}M",
                                            color = colors.primaryPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "↑${item.uploadMbps}M",
                                            color = colors.statusConnected,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isTesting) {
                Button(
                    onClick = {
                        testJob?.cancel()
                        currentPhase = SpeedtestPhase.Idle
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.statusDisconnected,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.virax_speedtest_stop),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { startTest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.virax_speedtest_run_again),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    testJob?.cancel()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.virax_speedtest_close),
                    color = colors.textSecondary
                )
            }
        }
    )

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.virax_speedtest_clear_history),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.virax_speedtest_clear_confirm),
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ViraxSpeedtestManager.clearHistory()
                        history = emptyList()
                        showClearConfirm = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = colors.statusDisconnected,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = colors.textSecondary
                    )
                }
            },
            containerColor = colors.cardBackgroundElevated
        )
    }
}

@Composable
private fun SpeedtestMetricCard(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = colors.primaryPurple,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = colors.textSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
