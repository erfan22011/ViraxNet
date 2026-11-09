package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

enum class ViraxTab(val labelRes: Int, val iconRes: Int) {
    HOME(R.string.virax_tab_home, R.drawable.ic_virax_tab_home),
    SERVERS(R.string.virax_tab_servers, R.drawable.ic_virax_tab_servers),
    STATS(R.string.virax_tab_stats, R.drawable.ic_virax_tab_stats),
    SETTINGS(R.string.virax_tab_settings, R.drawable.ic_virax_tab_settings)
}

@Composable
fun ViraxBottomNavigation(
    selectedTab: ViraxTab,
    onTabSelected: (ViraxTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ViraxTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bottomBarBackground)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViraxTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = stringResource(tab.labelRes)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .semantics {
                            role = Role.Tab
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp, 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.darkPurple else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = null,
                            tint = if (isSelected) colors.primaryPurple else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) colors.textPrimary else colors.textMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
