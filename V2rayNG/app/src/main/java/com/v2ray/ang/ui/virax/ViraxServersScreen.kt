package com.v2ray.ang.ui.virax

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.ui.compose.DeleteConfirmDialog

enum class ViraxServerFilter(val labelRes: Int) {
    ALL(R.string.virax_filter_all),
    HIGH_SPEED(R.string.virax_filter_high_speed),
    STABLE(R.string.virax_filter_stable),
    FAVORITES(R.string.virax_filter_favorites)
}

data class ViraxServerItem(
    val guid: String,
    val remarks: String,
    val pingMillis: Long,
    val isSelected: Boolean,
    val profile: ProfileItem? = null,
    val typeDescription: String = "",
    val isProtected: Boolean = false
)

fun filterServers(
    servers: List<ViraxServerItem>,
    filter: ViraxServerFilter,
    searchQuery: String = ""
): List<ViraxServerItem> {
    var list = servers
    if (searchQuery.isNotBlank()) {
        list = list.filter { it.remarks.contains(searchQuery, ignoreCase = true) }
    }
    return when (filter) {
        ViraxServerFilter.ALL -> list
        ViraxServerFilter.HIGH_SPEED -> list.filter { it.pingMillis in 1..150 }.sortedBy { it.pingMillis }
        ViraxServerFilter.STABLE -> list.filter { it.pingMillis > 0 }.sortedBy { it.pingMillis }
        ViraxServerFilter.FAVORITES -> list
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViraxServersScreen(
    servers: List<ViraxServerItem>,
    groups: List<GroupMapItem>,
    selectedGroupId: String,
    selectedGuid: String?,
    isTesting: Boolean,
    isLoading: Boolean,
    onSelectGroup: (String) -> Unit,
    onServerSelect: (String) -> Unit,
    onAutoConnectClick: () -> Unit,
    onConnectSelectedClick: () -> Unit,
    onPingAllClick: () -> Unit,
    onCancelPingClick: () -> Unit,
    onImportClipboardClick: () -> Unit,
    onImportQrClick: () -> Unit,
    onImportLocalClick: () -> Unit,
    onOpenManualLinkImport: () -> Unit,
    onOpenDedicatedCodeImport: () -> Unit,
    onPingServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onDeleteServer: (String) -> Unit,
    onShareServerQr: (String) -> Unit,
    onShareServerClipboard: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors
    val haptics = LocalHapticFeedback.current
    var activeFilter by remember { mutableStateOf(ViraxServerFilter.ALL) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSubscriptionPicker by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var contextTarget by remember { mutableStateOf<ViraxServerItem?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<ViraxServerItem?>(null) }

    val filteredServers = remember(servers, activeFilter, searchQuery) {
        filterServers(servers, activeFilter, searchQuery)
    }

    val currentGroupTitle = remember(groups, selectedGroupId) {
        groups.firstOrNull { it.id == selectedGroupId }?.remarks
    } ?: stringResource(R.string.virax_all_servers)

    // 1. Subscription Selector Modal
    if (showSubscriptionPicker) {
        AlertDialog(
            onDismissRequest = { showSubscriptionPicker = false },
            containerColor = colors.cardBackgroundElevated,
            titleContentColor = colors.textPrimary,
            title = {
                Text(
                    text = stringResource(R.string.virax_select_subscription),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                if (groups.isEmpty()) {
                    Text(
                        text = stringResource(R.string.virax_no_subscriptions),
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groups, key = { it.id.ifEmpty { "__all__" } }) { group ->
                            val isSelected = group.id == selectedGroupId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.darkPurple else colors.cardBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primaryPurple else colors.cardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectGroup(group.id)
                                        showSubscriptionPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = group.remarks.ifBlank { stringResource(R.string.virax_all_servers) },
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectGroup(group.id)
                                        showSubscriptionPicker = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primaryPurple,
                                        unselectedColor = colors.textMuted
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubscriptionPicker = false }) {
                    Text(stringResource(R.string.action_close), color = colors.primaryPurple)
                }
            }
        )
    }

    // 2. "+" Import Menu Dialog (matching official v2rayNG import options)
    if (showImportMenu) {
        AlertDialog(
            onDismissRequest = { showImportMenu = false },
            containerColor = colors.cardBackgroundElevated,
            titleContentColor = colors.textPrimary,
            title = {
                Text(
                    text = stringResource(R.string.virax_add_server_or_sub),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_lock_24dp,
                        label = stringResource(R.string.virax_dedicated_code_title),
                        onClick = {
                            showImportMenu = false
                            onOpenDedicatedCodeImport()
                        }
                    )
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_copy,
                        label = stringResource(R.string.menu_item_import_config_clipboard),
                        onClick = {
                            showImportMenu = false
                            onImportClipboardClick()
                        }
                    )
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_scan_24dp,
                        label = stringResource(R.string.menu_item_import_config_qrcode),
                        onClick = {
                            showImportMenu = false
                            onImportQrClick()
                        }
                    )
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_virax_link,
                        label = stringResource(R.string.virax_import_link_title),
                        onClick = {
                            showImportMenu = false
                            onOpenManualLinkImport()
                        }
                    )
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_file_24dp,
                        label = stringResource(R.string.menu_item_import_config_local),
                        onClick = {
                            showImportMenu = false
                            onImportLocalClick()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportMenu = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        )
    }

    // 3. Long-press Server Context Menu (Ping + unlocked Edit / Delete / Share QR / Copy Link)
    contextTarget?.let { server ->
        val isComplex = server.profile?.configType?.isComplexType() == true
        val subtitleText = if (server.isProtected) {
            stringResource(R.string.virax_protected_server_label)
        } else {
            server.typeDescription
        }
        AlertDialog(
            onDismissRequest = { contextTarget = null },
            containerColor = colors.cardBackgroundElevated,
            titleContentColor = colors.textPrimary,
            title = {
                Column {
                    Text(
                        text = server.remarks,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitleText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            color = if (server.isProtected) colors.gold else colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServerDialogActionRow(
                        iconRes = R.drawable.ic_virax_lightning,
                        label = stringResource(R.string.virax_ping_action),
                        onClick = {
                            contextTarget = null
                            onPingServer(server.guid)
                        }
                    )
                    if (!server.isProtected) {
                        server.profile?.let { profile ->
                            ServerDialogActionRow(
                                iconRes = R.drawable.ic_edit_24dp,
                                label = stringResource(R.string.action_edit),
                                onClick = {
                                    contextTarget = null
                                    onEditServer(server.guid, profile)
                                }
                            )
                        }
                        if (!isComplex) {
                            ServerDialogActionRow(
                                iconRes = R.drawable.ic_scan_24dp,
                                label = stringResource(R.string.virax_share_qr),
                                onClick = {
                                    contextTarget = null
                                    onShareServerQr(server.guid)
                                }
                            )
                            ServerDialogActionRow(
                                iconRes = R.drawable.ic_copy,
                                label = stringResource(R.string.virax_copy_link),
                                onClick = {
                                    contextTarget = null
                                    onShareServerClipboard(server.guid)
                                }
                            )
                        }
                        ServerDialogActionRow(
                            iconRes = R.drawable.ic_delete_24dp,
                            label = stringResource(R.string.action_delete),
                            tint = colors.disconnectRed,
                            onClick = {
                                contextTarget = null
                                deleteConfirmTarget = server
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextTarget = null }) {
                    Text(stringResource(R.string.action_close), color = colors.textSecondary)
                }
            }
        )
    }

    // 4. Delete Confirmation Dialog
    deleteConfirmTarget?.let { target ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_profile),
            itemName = target.remarks,
            onConfirm = {
                onDeleteServer(target.guid)
                deleteConfirmTarget = null
            },
            onDismiss = { deleteConfirmTarget = null }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header with Search, Title, Ping Test, + Add Button, Back
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24dp),
                            contentDescription = stringResource(R.string.virax_search_hint),
                            tint = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = { if (isTesting) onCancelPingClick() else onPingAllClick() }
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                color = colors.primaryPurple,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_virax_lightning),
                                contentDescription = stringResource(R.string.virax_ping_all),
                                tint = colors.gold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.virax_tab_servers),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showImportMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = stringResource(R.string.virax_add_server_or_sub),
                            tint = colors.primaryPurple
                        )
                    }
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.action_close),
                            tint = colors.textPrimary
                        )
                    }
                }
            }

            // Top Subscription Selector Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .clickable { showSubscriptionPicker = true }
                    .semantics { role = Role.DropdownList }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
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
                            painter = painterResource(R.drawable.ic_subscriptions_24dp),
                            contentDescription = null,
                            tint = colors.primaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentGroupTitle,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.virax_server_count, servers.size),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_expand_more_24dp),
                            contentDescription = stringResource(R.string.virax_select_subscription),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar (if opened)
            if (isSearchOpen) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = stringResource(R.string.virax_search_hint), color = colors.textSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBackground,
                        unfocusedContainerColor = colors.cardBackground,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    singleLine = true
                )
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ViraxServerFilter.entries) { filter ->
                    val isSelected = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.darkPurple else colors.cardBackground)
                            .border(
                                1.dp,
                                if (isSelected) colors.primaryPurple else colors.cardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(filter.labelRes),
                            color = if (isSelected) (if (colors.isDark) Color.White else colors.primaryPurple) else colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Auto Connect Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = servers.isNotEmpty(), onClick = onAutoConnectClick)
                    .semantics { role = Role.Button }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.darkPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_virax_lightning),
                                contentDescription = null,
                                tint = colors.primaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.virax_auto_connect),
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.virax_select_fastest_server),
                                color = colors.textSecondary,
                                fontSize = 12.sp
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

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading && filteredServers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primaryPurple)
                }
            } else if (filteredServers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_virax_tab_servers),
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.virax_no_servers_found),
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showImportMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.virax_add_server_or_sub),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Server List (long-press opens Edit / Delete / Share QR / Copy Link)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredServers, key = { it.guid }) { server ->
                        val isChecked = server.guid == selectedGuid
                        val flagRes = ViraxFlagUtils.getFlagDrawable(server.remarks)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBackground)
                                .border(
                                    1.dp,
                                    if (isChecked) colors.primaryPurple else colors.cardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .combinedClickable(
                                    onClick = { onServerSelect(server.guid) },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        contextTarget = server
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = server.remarks,
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (server.isProtected) {
                                            Text(
                                                text = stringResource(R.string.virax_protected_server_label),
                                                color = colors.primaryPurple,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else if (server.typeDescription.isNotBlank()) {
                                            Text(
                                                text = server.typeDescription,
                                                color = colors.textMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (server.pingMillis > 0) {
                                        Text(
                                            text = "${server.pingMillis} ms",
                                            color = if (server.pingMillis > 300) colors.disconnectRed else colors.greenNeon,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ViraxSignalBars(ping = server.pingMillis)
                                    } else {
                                        Text(
                                            text = "- ms",
                                            color = colors.textMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { contextTarget = server },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert_24dp),
                                            contentDescription = stringResource(R.string.virax_server_actions),
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    RadioButton(
                                        selected = isChecked,
                                        onClick = { onServerSelect(server.guid) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = colors.primaryPurple,
                                            unselectedColor = colors.textMuted
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = onConnectSelectedClick,
                enabled = servers.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_virax_lightning),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.virax_connect_selected_server),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerDialogActionRow(
    iconRes: Int,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = ViraxTheme.colors
    val iconColor = tint ?: colors.primaryPurple
    val textColor = tint ?: colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
