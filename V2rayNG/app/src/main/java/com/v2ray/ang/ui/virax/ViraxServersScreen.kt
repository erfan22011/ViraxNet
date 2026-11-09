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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R

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
    val isSelected: Boolean
)

@Composable
fun ViraxServersScreen(
    servers: List<ViraxServerItem>,
    selectedGuid: String?,
    onServerSelect: (String) -> Unit,
    onAutoConnectClick: () -> Unit,
    onConnectSelectedClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(ViraxServerFilter.ALL) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredServers = remember(servers, activeFilter, searchQuery) {
        var list = servers
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.remarks.contains(searchQuery, ignoreCase = true) }
        }
        when (activeFilter) {
            ViraxServerFilter.ALL -> list
            ViraxServerFilter.HIGH_SPEED -> list.filter { it.pingMillis in 1..150 }
                .sortedBy { it.pingMillis }
            ViraxServerFilter.STABLE -> list.filter { it.pingMillis > 0 }
                .sortedBy { it.pingMillis }
            ViraxServerFilter.FAVORITES -> list // can be extended with favorite flag
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViraxColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ViraxColors.TextWhite
                    )
                }

                Text(
                    text = stringResource(R.string.virax_tab_servers),
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

            // Search Bar (if opened)
            if (isSearchOpen) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = "جستجوی سرور...", color = ViraxColors.TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ViraxColors.CardBackground,
                        unfocusedContainerColor = ViraxColors.CardBackground,
                        focusedTextColor = ViraxColors.TextWhite,
                        unfocusedTextColor = ViraxColors.TextWhite
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
                            .background(if (isSelected) ViraxColors.PurpleDark else ViraxColors.CardBackground)
                            .border(
                                1.dp,
                                if (isSelected) ViraxColors.PurplePrimary else ViraxColors.CardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(filter.labelRes),
                            color = if (isSelected) ViraxColors.TextWhite else ViraxColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auto Connect Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ViraxColors.CardBackground)
                    .border(1.dp, ViraxColors.CardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onAutoConnectClick)
                    .semantics { role = Role.Button }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ViraxColors.PurpleDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_virax_lightning),
                                contentDescription = null,
                                tint = ViraxColors.PurpleLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.virax_auto_connect),
                                color = ViraxColors.TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.virax_select_fastest_server),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Server List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredServers, key = { it.guid }) { server ->
                    val isChecked = server.guid == selectedGuid
                    val flag = ViraxFlagUtils.getFlagEmoji(server.remarks)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ViraxColors.CardBackground)
                            .border(
                                1.dp,
                                if (isChecked) ViraxColors.PurplePrimary else ViraxColors.CardBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onServerSelect(server.guid) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
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
                                Text(text = flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = server.remarks,
                                    color = ViraxColors.TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (server.pingMillis > 0) {
                                    Text(
                                        text = "${server.pingMillis} ms",
                                        color = if (server.pingMillis > 300) ViraxColors.DisconnectRed else ViraxColors.GreenNeon,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ViraxSignalBars(ping = server.pingMillis)
                                } else {
                                    Text(
                                        text = "- ms",
                                        color = ViraxColors.TextMuted,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                RadioButton(
                                    selected = isChecked,
                                    onClick = { onServerSelect(server.guid) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ViraxColors.PurpleLight,
                                        unselectedColor = ViraxColors.TextMuted
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
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
                colors = ButtonDefaults.buttonColors(containerColor = ViraxColors.PurpleSecondary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "⚡ " + stringResource(R.string.virax_connect_selected_server),
                    color = ViraxColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
