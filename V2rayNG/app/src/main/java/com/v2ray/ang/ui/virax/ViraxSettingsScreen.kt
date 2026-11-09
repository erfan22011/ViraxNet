package com.v2ray.ang.ui.virax

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
fun ViraxSettingsScreen(
    localDnsEnabled: Boolean,
    onLocalDnsToggled: (Boolean) -> Unit,
    fakeDnsEnabled: Boolean,
    onFakeDnsToggled: (Boolean) -> Unit,
    pingTestUrl: String,
    onPingTestUrlChanged: (String) -> Unit,
    onRoutingSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showPingDialog by remember { mutableStateOf(false) }
    var tempPingUrl by remember { mutableStateOf(pingTestUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ViraxColors.Background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                text = stringResource(R.string.virax_tab_settings),
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

        // 1. Routing Settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ViraxColors.CardBackground)
                .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onRoutingSettingsClick)
                .semantics { role = Role.Button }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_routing),
                        contentDescription = null,
                        tint = ViraxColors.PurpleLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.virax_routing_settings),
                            color = ViraxColors.TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.virax_routing_subtitle),
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

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Local DNS Toggle
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_globe),
                        contentDescription = null,
                        tint = ViraxColors.PurpleLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.virax_local_dns),
                            color = ViraxColors.TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.virax_local_dns_subtitle),
                            color = ViraxColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = localDnsEnabled,
                    onCheckedChange = onLocalDnsToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ViraxColors.TextWhite,
                        checkedTrackColor = ViraxColors.PurplePrimary,
                        uncheckedThumbColor = ViraxColors.TextMuted,
                        uncheckedTrackColor = ViraxColors.CardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Fake DNS Toggle
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_tab_servers),
                        contentDescription = null,
                        tint = ViraxColors.PurpleLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.virax_fake_dns),
                            color = ViraxColors.TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.virax_fake_dns_subtitle),
                            color = ViraxColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = fakeDnsEnabled,
                    onCheckedChange = onFakeDnsToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ViraxColors.TextWhite,
                        checkedTrackColor = ViraxColors.PurplePrimary,
                        uncheckedThumbColor = ViraxColors.TextMuted,
                        uncheckedTrackColor = ViraxColors.CardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Ping Test URL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ViraxColors.CardBackground)
                .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                .clickable {
                    tempPingUrl = pingTestUrl
                    showPingDialog = true
                }
                .semantics { role = Role.Button }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_virax_link),
                    contentDescription = null,
                    tint = ViraxColors.PurpleLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.virax_ping_test_url),
                        color = ViraxColors.TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pingTestUrl,
                        color = ViraxColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Footer Note
        Text(
            text = "ⓘ " + stringResource(R.string.virax_ping_type_note),
            color = ViraxColors.TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showPingDialog) {
        AlertDialog(
            onDismissRequest = { showPingDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.virax_ping_test_url),
                    color = ViraxColors.TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempPingUrl,
                    onValueChange = { tempPingUrl = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPingTestUrlChanged(tempPingUrl)
                        showPingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ViraxColors.PurpleSecondary)
                ) {
                    Text(text = stringResource(R.string.action_ok), color = ViraxColors.TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPingDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = ViraxColors.TextSecondary)
                }
            },
            containerColor = ViraxColors.CardBackgroundElevated
        )
    }
}
