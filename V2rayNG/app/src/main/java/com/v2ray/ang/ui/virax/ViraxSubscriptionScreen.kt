package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.DeleteConfirmDialog
import com.v2ray.ang.util.Utils

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
        return if (days >= 0) "$days" else ""
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
    subscriptions: List<SubscriptionCache>,
    selectedSubId: String,
    isLoading: Boolean,
    showPremiumBadge: Boolean,
    onSelectSubscription: (String) -> Unit,
    onAddSubscriptionClick: () -> Unit,
    onImportClipboardClick: () -> Unit,
    onRefreshSubscription: (String) -> Unit,
    onToggleAutoUpdate: (String, Boolean) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onManageSubscriptionClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors
    val scrollState = rememberScrollState()
    val unavailableText = stringResource(R.string.virax_sub_info_unavailable)

    val activeSub = remember(subscriptions, selectedSubId) {
        subscriptions.firstOrNull { it.guid == selectedSubId } ?: subscriptions.firstOrNull()
    }
    val data = remember(activeSub) {
        ViraxConnectionManager.createUiDataFromSubscription(activeSub?.subscription)
    }

    var pendingDeleteSub by remember { mutableStateOf<SubscriptionCache?>(null) }

    pendingDeleteSub?.let { target ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_subscription_group),
            itemName = target.subscription.remarks,
            onConfirm = {
                onDeleteSubscription(target.guid)
                pendingDeleteSub = null
            },
            onDismiss = { pendingDeleteSub = null }
        )
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddSubscriptionClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.acc_add_subscription),
                        tint = colors.textPrimary
                    )
                }
                if (subscriptions.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                onRefreshSubscription(activeSub?.guid ?: "")
                            }
                        }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = colors.primaryPurple,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh_24dp),
                                contentDescription = stringResource(R.string.acc_update_subscriptions),
                                tint = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.virax_user_subscription),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = stringResource(R.string.action_close),
                    tint = colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Golden Crown Hero Badge
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.darkPurple, Color.Transparent)
                    )
                )
                .border(2.dp, colors.primaryPurple.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.secondaryPurple, colors.darkPurple)
                        )
                    )
                    .border(2.dp, colors.gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_crown),
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ViraxNet Title (+ Premium ONLY when active subscription link exists)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.virax_brand_name),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (showPremiumBadge) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.virax_premium),
                    color = colors.gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (subscriptions.isEmpty()) {
            // Empty State: No active subscription link imported
            Text(
                text = stringResource(R.string.virax_no_subscriptions),
                color = colors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_link),
                        contentDescription = null,
                        tint = colors.primaryPurple,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.virax_onboarding_sub_message),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAddSubscriptionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.virax_onboarding_sub_title),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onImportClipboardClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.virax_paste_clipboard_activate),
                            color = colors.primaryPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // Subscription Status Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (data.hasUserInfo) {
                    Icon(
                        painter = painterResource(
                            id = if (data.isExpired) R.drawable.ic_virax_warning else R.drawable.ic_virax_sparkle
                        ),
                        contentDescription = null,
                        tint = if (data.isExpired) colors.disconnectRed else colors.gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                val statusText = when {
                    !data.hasUserInfo -> unavailableText
                    data.isExpired -> stringResource(R.string.virax_sub_expired)
                    else -> stringResource(R.string.virax_sub_active_valid)
                }
                Text(
                    text = statusText,
                    color = if (data.isExpired) colors.disconnectRed else colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Subscription Selector Pills (if more than 1 subscription exists)
            if (subscriptions.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(subscriptions, key = { it.guid }) { sub ->
                        val isSelected = sub.guid == activeSub?.guid
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.darkPurple else colors.cardBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primaryPurple else colors.cardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectSubscription(sub.guid) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = sub.subscription.remarks.ifBlank { unavailableText },
                                color = if (isSelected) (if (colors.isDark) Color.White else colors.primaryPurple) else colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2-Column Info Cards (Remaining Days & Expiry Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_virax_clock),
                            contentDescription = null,
                            tint = colors.primaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.virax_remaining_time),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val daysVal = data.remainingDaysText.toLongOrNull()
                        val daysDisplay = if (data.hasUserInfo && daysVal != null) {
                            stringResource(R.string.virax_days_unit, daysVal)
                        } else {
                            unavailableText
                        }
                        Text(
                            text = daysDisplay,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_virax_calendar),
                            contentDescription = null,
                            tint = colors.primaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.virax_expiry_date),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (data.hasUserInfo && data.jalaliExpiryDate.isNotBlank()) data.jalaliExpiryDate else unavailableText,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Traffic Consumed Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
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
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (data.hasUserInfo && data.trafficText.isNotBlank()) data.trafficText else unavailableText,
                            color = colors.textPrimary,
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
                        color = colors.primaryPurple,
                        trackColor = colors.cardBorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Subscription Controls Card (List of real subscriptions, 12h auto-update, refresh, delete)
            subscriptions.forEach { subCache ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .border(
                            1.dp,
                            if (subCache.guid == activeSub?.guid) colors.primaryPurple.copy(alpha = 0.6f) else colors.cardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectSubscription(subCache.guid) }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subCache.subscription.remarks.ifBlank { stringResource(R.string.virax_user_subscription) },
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(
                                        R.string.virax_last_updated,
                                        Utils.formatTimestamp(subCache.subscription.lastUpdated)
                                    ),
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onRefreshSubscription(subCache.guid) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_refresh_24dp),
                                        contentDescription = stringResource(R.string.title_sub_update),
                                        tint = colors.primaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDeleteSub = subCache },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete_24dp),
                                        contentDescription = stringResource(R.string.acc_delete),
                                        tint = colors.disconnectRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_virax_clock),
                                    contentDescription = null,
                                    tint = colors.primaryPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.virax_auto_update_12h),
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = subCache.subscription.autoUpdate,
                                onCheckedChange = { checked ->
                                    onToggleAutoUpdate(subCache.guid, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primaryPurple,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.cardBorder
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Add Subscription + Advanced Settings Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddSubscriptionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.virax_onboarding_sub_title),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onManageSubscriptionClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.virax_advanced_manage),
                        color = colors.primaryPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Info Note Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_info),
                    contentDescription = null,
                    tint = colors.primaryPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.virax_sub_received_from_link),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
