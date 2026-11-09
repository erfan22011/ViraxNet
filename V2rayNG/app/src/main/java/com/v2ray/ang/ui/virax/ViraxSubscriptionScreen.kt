package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.extension.toTrafficString

data class ViraxSubscriptionUiData(
    val hasUserInfo: Boolean,
    val uploadBytes: Long,
    val downloadBytes: Long,
    val totalBytes: Long,
    val expireTimestamp: Long
) {
    val usedBytes: Long get() = uploadBytes + downloadBytes

    val isExpired: Boolean get() = expireTimestamp > 0 &&
            (expireTimestamp * 1000L < System.currentTimeMillis() ||
                    (expireTimestamp < 100_000_000_000L && expireTimestamp * 1000L < System.currentTimeMillis()))

    val remainingDaysText: String get() {
        if (!hasUserInfo || expireTimestamp <= 0) return ""
        val days = ViraxDateUtils.remainingDays(expireTimestamp)
        return if (days >= 0) "$days روز" else ""
    }

    val jalaliExpiryDate: String get() {
        if (!hasUserInfo || expireTimestamp <= 0) return ""
        return ViraxDateUtils.timestampToJalali(expireTimestamp)
    }

    val progressFraction: Float get() {
        if (!hasUserInfo || totalBytes <= 0) return 0f
        return (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    val trafficText: String get() {
        if (!hasUserInfo) return ""
        return "${usedBytes.toTrafficString()} / ${if (totalBytes > 0) totalBytes.toTrafficString() else "∞"}"
    }
}

@Composable
fun ViraxSubscriptionScreen(
    data: ViraxSubscriptionUiData,
    onManageSubscriptionClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val unavailableText = stringResource(R.string.virax_sub_info_unavailable)

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
            Box(modifier = Modifier.size(48.dp)) // Spacer to center title

            Text(
                text = stringResource(R.string.virax_user_subscription),
                color = ViraxColors.TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ViraxColors.TextWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Glowing Golden Crown Badge
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF381F6D),
                            Color(0xFF1B0E38),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, ViraxColors.PurpleLight.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6D28D9),
                                Color(0xFF4C1D95)
                            )
                        )
                    )
                    .border(2.dp, ViraxColors.Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_crown),
                    contentDescription = null,
                    tint = ViraxColors.Gold,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ViraxNet Premium Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.virax_brand_name),
                color = ViraxColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.virax_premium),
                color = ViraxColors.Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subscription Status
        val statusText = when {
            !data.hasUserInfo -> unavailableText
            data.isExpired -> stringResource(R.string.virax_sub_expired)
            else -> stringResource(R.string.virax_sub_active_valid)
        }
        Text(
            text = statusText,
            color = if (data.isExpired) ViraxColors.DisconnectRed else ViraxColors.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2-Column Info Cards (Remaining Days & Expiry Date)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Remaining Time Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ViraxColors.CardBackground)
                    .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⏱", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.virax_remaining_time),
                        color = ViraxColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (data.hasUserInfo && data.remainingDaysText.isNotBlank()) data.remainingDaysText else unavailableText,
                        color = ViraxColors.TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expiry Date Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ViraxColors.CardBackground)
                    .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📅", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.virax_expiry_date),
                        color = ViraxColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (data.hasUserInfo && data.jalaliExpiryDate.isNotBlank()) data.jalaliExpiryDate else unavailableText,
                        color = ViraxColors.TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Traffic Consumed Progress Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ViraxColors.CardBackground)
                .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.virax_traffic_consumed),
                        color = ViraxColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (data.hasUserInfo && data.trafficText.isNotBlank()) data.trafficText else unavailableText,
                        color = ViraxColors.TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { if (data.hasUserInfo) data.progressFraction else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = ViraxColors.PurpleLight,
                    trackColor = ViraxColors.CardBorder
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manage Subscription Button
        Button(
            onClick = onManageSubscriptionClick,
            colors = ButtonDefaults.buttonColors(containerColor = ViraxColors.PurpleSecondary),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(R.string.virax_manage_subscription),
                color = ViraxColors.TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Info Note Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ViraxColors.CardBackground)
                .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ⓘ", color = ViraxColors.PurpleLight, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.virax_sub_received_from_link),
                    color = ViraxColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
