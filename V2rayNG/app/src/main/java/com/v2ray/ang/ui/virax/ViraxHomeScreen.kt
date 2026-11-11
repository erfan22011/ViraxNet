package com.v2ray.ang.ui.virax

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.handler.ViraxPlanBadge

@Composable
fun ViraxHomeScreen(
    isConnected: Boolean,
    isConnecting: Boolean,
    planBadge: ViraxPlanBadge = ViraxPlanBadge.NONE,
    isFetchingIp: Boolean = false,
    serverName: String?,
    serverPing: Long,
    ipAddress: String?,
    connectionDurationText: String,
    downloadVolumeText: String,
    uploadVolumeText: String,
    onPowerClick: () -> Unit,
    onShieldClick: () -> Unit,
    onIpCardClick: () -> Unit,
    onCrownClick: () -> Unit,
    onSmartSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header (symmetry spacer on start, ViraxNet + badge centered, crown on end)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(48.dp))

            // Logo & Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_rocket),
                    contentDescription = null,
                    tint = colors.primaryPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.virax_brand_name),
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                when (planBadge) {
                    ViraxPlanBadge.PREMIUM -> {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(colors.gold, colors.goldLight)
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.virax_premium),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    ViraxPlanBadge.CUSTOM -> {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(colors.primaryPurple, colors.secondaryPurple)
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.virax_plan_custom),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    ViraxPlanBadge.NONE -> Unit
                }
            }

            IconButton(onClick = onCrownClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_crown),
                    contentDescription = stringResource(R.string.virax_user_subscription),
                    tint = colors.gold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isConnected) {
            // Connected View State
            ConnectedShieldView(onShieldClick = onShieldClick)

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_shield_check),
                    contentDescription = null,
                    tint = colors.greenNeon,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.virax_connected),
                    color = colors.greenNeon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connected Server Info Card
            ConnectedServerCard(serverName = serverName, serverPing = serverPing)

            Spacer(modifier = Modifier.height(14.dp))

            // IP Address Card (auto-fetched on connect, also tappable to refresh)
            ViraxIpCard(
                ip = ipAddress,
                isConnected = true,
                isFetchingIp = isFetchingIp,
                onClick = onIpCardClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Session Stats Card
            ViraxStatsCard(
                timeText = connectionDurationText,
                downloadText = downloadVolumeText,
                uploadText = uploadVolumeText
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Disconnect Button
            Button(
                onClick = onPowerClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_disconnect),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.virax_disconnect),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Disconnected View State
            DisconnectedPowerButton(onPowerClick = onPowerClick)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.virax_auto_connect),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.virax_subtitle_disconnected),
                color = colors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Card (00:00:00, 0 B, 0 B)
            ViraxStatsCard(
                timeText = "00:00:00",
                downloadText = "0 B",
                uploadText = "0 B"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // IP Address Card
            ViraxIpCard(
                ip = ipAddress,
                isConnected = false,
                onClick = null
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Smart Best Server Selection Card
            ViraxSmartServerCard(
                serverName = serverName,
                serverPing = serverPing,
                onClick = onSmartSelectClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DisconnectedPowerButton(onPowerClick: () -> Unit) {
    val colors = ViraxTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "power_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(190.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.powerButtonOuterGlow,
                        colors.powerButtonMiddleGlow,
                        Color.Transparent
                    )
                )
            )
            .border(2.dp, colors.powerButtonRingBorder, CircleShape)
            .clickable(onClick = onPowerClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(CircleShape)
                .background(colors.powerButtonGradient)
                .border(2.dp, colors.lightPurple.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_virax_power),
                contentDescription = stringResource(R.string.virax_auto_connect),
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun ConnectedShieldView(onShieldClick: () -> Unit) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.connectedShieldOuterGlow,
                        Color.Transparent
                    )
                )
            )
            .border(2.dp, colors.greenNeon.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onShieldClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .clip(CircleShape)
                .background(colors.connectedShieldGradient)
                .border(2.dp, colors.greenGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_virax_shield_check),
                contentDescription = stringResource(R.string.virax_disconnect),
                tint = Color.White,
                modifier = Modifier.size(62.dp)
            )
        }
    }
}

@Composable
private fun ConnectedServerCard(serverName: String?, serverPing: Long) {
    val colors = ViraxTheme.colors
    val flagRes = ViraxFlagUtils.getFlagDrawable(serverName)
    val displayName = serverName ?: stringResource(R.string.virax_not_selected)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = flagRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = displayName,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (serverPing > 0) {
                    Text(
                        text = stringResource(R.string.virax_ms_unit, serverPing),
                        color = colors.greenNeon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ViraxSignalBars(ping = serverPing)
                } else {
                    Text(
                        text = "- ms",
                        color = colors.textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ViraxSignalBars(ping: Long, modifier: Modifier = Modifier) {
    val colors = ViraxTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val activeBars = when {
            ping in 1..80 -> 4
            ping in 81..160 -> 3
            ping in 161..300 -> 2
            else -> 1
        }
        val barColor = if (ping > 300) colors.disconnectRed else colors.greenNeon

        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((i * 3 + 4).dp)
                    .background(if (i <= activeBars) barColor else colors.textMuted)
            )
        }
    }
}

@Composable
private fun ViraxStatsCard(
    timeText: String,
    downloadText: String,
    uploadText: String
) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_clock),
                    contentDescription = null,
                    tint = colors.primaryPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = timeText,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_connection_time),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colors.cardBorder)
            )

            // Download
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_download),
                    contentDescription = null,
                    tint = colors.primaryPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = downloadText,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_download),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(colors.cardBorder)
            )

            // Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_upload),
                    contentDescription = null,
                    tint = colors.greenNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = uploadText,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_upload),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ViraxIpCard(
    ip: String?,
    isConnected: Boolean,
    isFetchingIp: Boolean = false,
    onClick: (() -> Unit)?
) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_virax_globe),
                contentDescription = null,
                tint = colors.primaryPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.virax_your_ip),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = ip ?: when {
                        isConnected && isFetchingIp -> stringResource(R.string.virax_ip_fetching)
                        isConnected -> stringResource(R.string.virax_tap_to_test_ip)
                        else -> "-"
                    },
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ViraxSmartServerCard(
    serverName: String?,
    serverPing: Long,
    onClick: () -> Unit
) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_lightning),
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.virax_smart_server_selection),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pingPart = stringResource(
                        R.string.virax_ping_prefix,
                        if (serverPing > 0) stringResource(R.string.virax_ms_unit, serverPing) else "- ms"
                    )
                    Text(
                        text = if (!serverName.isNullOrBlank()) "$serverName • $pingPart" else pingPart,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_virax_arrow_forward),
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
