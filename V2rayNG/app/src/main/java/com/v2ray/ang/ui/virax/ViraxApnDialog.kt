package com.v2ray.ang.ui.virax

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R

data class ViraxApnItem(
    val id: String,
    val operatorNameFa: String,
    val operatorNameEn: String,
    val apnName: String,
    val apnValue: String,
    val protocol: String = "IPv4/IPv6",
    val apnType: String = "default,supl"
)

val IRANIAN_OPERATORS_APN = listOf(
    ViraxApnItem(
        id = "mci",
        operatorNameFa = "همراه اول",
        operatorNameEn = "MCI",
        apnName = "MCINET",
        apnValue = "mcinet"
    ),
    ViraxApnItem(
        id = "irancell",
        operatorNameFa = "ایرانسل",
        operatorNameEn = "MTN Irancell",
        apnName = "Irancell-Internet",
        apnValue = "mtnirancell"
    ),
    ViraxApnItem(
        id = "rightel",
        operatorNameFa = "رایتل",
        operatorNameEn = "Rightel",
        apnName = "RighTel",
        apnValue = "rightel"
    ),
    ViraxApnItem(
        id = "samantel",
        operatorNameFa = "سامانتل",
        operatorNameEn = "Samantel",
        apnName = "Samantel",
        apnValue = "samantel"
    ),
    ViraxApnItem(
        id = "shatel",
        operatorNameFa = "شاتل موبایل",
        operatorNameEn = "Shatel Mobile",
        apnName = "Shatel Mobile",
        apnValue = "shatelmobile"
    ),
    ViraxApnItem(
        id = "aptel",
        operatorNameFa = "آپتل",
        operatorNameEn = "Aptel",
        apnName = "Aptel",
        apnValue = "aptel"
    )
)

@Composable
fun ViraxApnDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = ViraxTheme.colors
    var selectedOperator by remember { mutableStateOf(IRANIAN_OPERATORS_APN.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackgroundElevated,
        titleContentColor = colors.textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_apn),
                    contentDescription = null,
                    tint = colors.primaryPurple,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.virax_apn_dialog_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.virax_apn_dialog_desc),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Operator Selection Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IRANIAN_OPERATORS_APN) { op ->
                        val isSelected = op.id == selectedOperator.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.darkPurple else colors.cardBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primaryPurple else colors.cardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedOperator = op }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${op.operatorNameFa} (${op.operatorNameEn})",
                                color = if (isSelected) (if (colors.isDark) Color.White else colors.primaryPurple) else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // APN Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApnDetailRow(
                        label = stringResource(R.string.virax_apn_name_label),
                        value = selectedOperator.apnName,
                        onCopy = {
                            copySingleField(context, context.getString(R.string.virax_apn_name_label), selectedOperator.apnName)
                        }
                    )
                    ApnDetailRow(
                        label = stringResource(R.string.virax_apn_field_label),
                        value = selectedOperator.apnValue,
                        isHighlight = true,
                        onCopy = {
                            copySingleField(context, context.getString(R.string.virax_apn_field_label), selectedOperator.apnValue)
                        }
                    )
                    ApnDetailRow(
                        label = stringResource(R.string.virax_apn_protocol_label),
                        value = selectedOperator.protocol
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guidance hint for blank APN screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.virax_apn_empty_hint),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    copyApnAndOpenSettings(context, selectedOperator.apnValue)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.virax_apn_open_settings),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = colors.textSecondary
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ApnDetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    val colors = ViraxTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                color = if (isHighlight) colors.primaryPurple else colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold
            )
            if (onCopy != null) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = "Copy $label",
                    tint = colors.primaryPurple,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCopy() }
                )
            }
        }
    }
}

private fun copySingleField(context: Context, label: String, value: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label: $value", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
    }
}

private fun copyApnAndOpenSettings(context: Context, apnValue: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("APN", apnValue)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.virax_apn_copied_toast), Toast.LENGTH_LONG).show()

        val apnIntent = Intent(Settings.ACTION_APN_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    putExtra("sub_id", subId)
                    putExtra("subscription_id", subId)
                    putExtra("android.provider.extra.SUB_ID", subId)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                @Suppress("DEPRECATION")
                val subId = SubscriptionManager.getDefaultSubscriptionId()
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    putExtra("sub_id", subId)
                    putExtra("subscription_id", subId)
                    putExtra("android.provider.extra.SUB_ID", subId)
                }
            }
        }
        context.startActivity(apnIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            val netIntent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(netIntent)
        } catch (_: Exception) {
            try {
                val wirelessIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(wirelessIntent)
            } catch (_: Exception) {
                val mainSettings = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mainSettings)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Failed to open settings", Toast.LENGTH_SHORT).show()
    }
}
