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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R

@Composable
fun ViraxHomeScreen(
    isConnected: Boolean,
    isConnecting: Boolean,
    serverName: String?,
    serverPing: Long,
    ipAddress: String?,
    connectionDurationText: String,
    downloadSpeedText: String,
    uploadSpeedText: String,
    onPowerClick: () -> Unit,
    onMenuClick: () -> Unit,
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
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.navigation_drawer_open),
                    tint = colors.textPrimary
                )
            }

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
            ConnectedShieldView()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.virax_connected) + " ✓",
                color = colors.greenNeon,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Connected Server Info Card
            ConnectedServerCard(serverName = serverName, serverPing = serverPing)

            Spacer(modifier = Modifier.height(16.dp))

            // Session Stats Card
            ViraxStatsCard(
                timeText = connectionDurationText,
                downloadText = downloadSpeedText,
                uploadText = uploadSpeedText
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            // Stats Card (00:00:00, 0 B/s, 0 B/s)
            ViraxStatsCard(
                timeText = "00:00:00",
                downloadText = "0 B/s",
                uploadText = "0 B/s"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // IP Address Card
            ViraxIpCard(ip = ipAddress)

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
private fun ConnectedShieldView() {
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
            .border(2.dp, colors.greenNeon.copy(alpha = 0.5f), CircleShape),
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
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(62.dp)
            )
        }
    }
}

@Composable
private fun ConnectedServerCard(serverName: String?, serverPing: Long) {
    val colors = ViraxTheme.colors
    val flag = ViraxFlagUtils.getFlagEmoji(serverName)
    val displayName = serverName ?: "Germany - Frankfurt"

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = displayName,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (serverPing > 0) "${serverPing} ms" else "12 ms",
                    color = colors.greenNeon,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                ViraxSignalBars(ping = if (serverPing > 0) serverPing else 12)
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
                Text(text = "⏱", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
                Text(text = "↓", fontSize = 16.sp, color = colors.primaryPurple, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = downloadText,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
                Text(text = "↑", fontSize = 16.sp, color = colors.greenNeon, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uploadText,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
private fun ViraxIpCard(ip: String?) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
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
                    text = ip ?: "-",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.virax_ping_prefix,
                            if (serverPing > 0) "${serverPing} ms" else "... ms"
                        ),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
