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
import androidx.compose.ui.text.style.TextAlign
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
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ViraxColors.Background)
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
                    tint = ViraxColors.TextWhite
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
                    tint = ViraxColors.PurpleLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.virax_brand_name),
                    color = ViraxColors.TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ViraxColors.GoldGradient)
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
                    tint = ViraxColors.Gold,
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
                color = ViraxColors.GreenNeon,
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
                colors = ButtonDefaults.buttonColors(containerColor = ViraxColors.PurpleSecondary),
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
                        tint = ViraxColors.TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.virax_disconnect),
                        color = ViraxColors.TextWhite,
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
                color = ViraxColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.virax_subtitle_disconnected),
                color = ViraxColors.TextSecondary,
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
                        Color(0xFF281454),
                        Color(0xFF140C2E),
                        Color.Transparent
                    )
                )
            )
            .border(2.dp, Color(0xFF381F6D), CircleShape)
            .clickable(onClick = onPowerClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6D28D9),
                            Color(0xFF4C1D95),
                            Color(0xFF1E0E45)
                        )
                    )
                )
                .border(2.dp, ViraxColors.PurpleLight.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_virax_power),
                contentDescription = stringResource(R.string.virax_auto_connect),
                tint = ViraxColors.TextWhite,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun ConnectedShieldView() {
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF022C22),
                        Color.Transparent
                    )
                )
            )
            .border(2.dp, ViraxColors.GreenNeon.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF059669),
                            Color(0xFF047857),
                            Color(0xFF064E3B)
                        )
                    )
                )
                .border(2.dp, ViraxColors.GreenGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_virax_shield_check),
                contentDescription = null,
                tint = ViraxColors.TextWhite,
                modifier = Modifier.size(62.dp)
            )
        }
    }
}

@Composable
private fun ConnectedServerCard(serverName: String?, serverPing: Long) {
    val flag = ViraxFlagUtils.getFlagEmoji(serverName)
    val displayName = serverName ?: "Germany - Frankfurt"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ViraxColors.CardBackground)
            .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
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
                    color = ViraxColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (serverPing > 0) "${serverPing} ms" else "12 ms",
                    color = ViraxColors.GreenNeon,
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
        val barColor = if (ping > 300) ViraxColors.DisconnectRed else ViraxColors.GreenNeon

        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((i * 3 + 4).dp)
                    .background(if (i <= activeBars) barColor else ViraxColors.TextMuted)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ViraxColors.CardBackground)
            .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(18.dp))
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
                    color = ViraxColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_connection_time),
                    color = ViraxColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(ViraxColors.CardBorder)
            )

            // Download
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "↓", fontSize = 16.sp, color = ViraxColors.PurpleLight, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = downloadText,
                    color = ViraxColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_download),
                    color = ViraxColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(ViraxColors.CardBorder)
            )

            // Upload
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "↑", fontSize = 16.sp, color = ViraxColors.GreenNeon, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uploadText,
                    color = ViraxColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.virax_upload),
                    color = ViraxColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ViraxIpCard(ip: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ViraxColors.CardBackground)
            .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
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
                tint = ViraxColors.PurpleLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.virax_your_ip),
                    color = ViraxColors.TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = ip ?: "-",
                    color = ViraxColors.TextWhite,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ViraxColors.CardBackground)
            .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
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
                    tint = ViraxColors.Gold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.virax_smart_server_selection),
                        color = ViraxColors.TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.virax_ping_prefix,
                            if (serverPing > 0) "${serverPing} ms" else "... ms"
                        ),
                        color = ViraxColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ViraxColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
