package com.v2ray.ang.ui.virax

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreSniSpoofManager
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.SniSpoofScope
import com.v2ray.ang.dto.ViraxSniServerOption
import com.v2ray.ang.dto.ViraxSniSubscriptionOption

/**
 * Localized summary describing current SNI Spoofing configuration state.
 */
@Composable
fun sniSpoofSummaryText(config: SniSpoofConfig): String {
    val scopeLabel = when (config.scope) {
        SniSpoofScope.SINGLE_SERVER -> stringResource(R.string.virax_sni_scope_single_server)
        SniSpoofScope.MULTI_SERVER -> stringResource(R.string.virax_sni_scope_multi_server)
        SniSpoofScope.SUBSCRIPTION -> stringResource(R.string.virax_sni_scope_subscription)
        SniSpoofScope.GLOBAL -> stringResource(R.string.virax_sni_scope_global)
    }
    return if (CoreSniSpoofManager.isValidActiveConfig(config)) {
        stringResource(
            R.string.virax_sni_status_enabled_fmt,
            config.fakeSni.trim(),
            config.connectIp.trim(),
            scopeLabel
        )
    } else {
        stringResource(R.string.virax_sni_spoof_subtitle)
    }
}

/**
 * Interactive SNI Spoofing (Experimental) configuration dialog supporting editable
 * Fake SNI, Connect IP, and target scope selection (single server, multi-server,
 * subscription link, or entire app) without exposing any protected server IPs or URLs.
 */
@Composable
fun ViraxSniSpoofingDialog(
    initialConfig: SniSpoofConfig,
    initialEnabled: Boolean,
    serverOptions: List<ViraxSniServerOption>,
    subscriptionOptions: List<ViraxSniSubscriptionOption>,
    onSave: (SniSpoofConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ViraxTheme.colors
    var enabled by remember(initialConfig, initialEnabled) { mutableStateOf(initialEnabled) }
    var fakeSni by remember(initialConfig) { mutableStateOf(initialConfig.fakeSni) }
    var connectIp by remember(initialConfig) { mutableStateOf(initialConfig.connectIp) }
    var scope by remember(initialConfig) { mutableStateOf(initialConfig.scope) }
    var selectedServerGuids by remember(initialConfig) {
        mutableStateOf(initialConfig.targetServerGuids)
    }
    var selectedSubscriptionIds by remember(initialConfig) {
        mutableStateOf(initialConfig.targetSubscriptionIds)
    }
    var serverQuery by remember { mutableStateOf("") }
    var validationErrorRes by remember { mutableStateOf<Int?>(null) }

    val filteredServers = remember(serverOptions, serverQuery) {
        val cleanQuery = serverQuery.trim()
        if (cleanQuery.isEmpty()) serverOptions
        else serverOptions.filter { it.remarks.contains(cleanQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.cardBackgroundElevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .navigationBarsPadding()
                .imePadding()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                val toggleLabel = stringResource(R.string.virax_sni_spoof_title)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = enabled,
                            role = Role.Switch,
                            onValueChange = { next ->
                                enabled = next
                                validationErrorRes = null
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = toggleLabel,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.primaryPurple,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.cardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.virax_sni_fake_label),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fakeSni,
                    onValueChange = {
                        fakeSni = it
                        validationErrorRes = null
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.virax_sni_fake_hint),
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryPurple,
                        unfocusedBorderColor = colors.cardBorder,
                        cursorColor = colors.primaryPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.virax_sni_connect_ip_label),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = connectIp,
                    onValueChange = {
                        connectIp = it
                        validationErrorRes = null
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.virax_sni_connect_ip_hint),
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primaryPurple,
                        unfocusedBorderColor = colors.cardBorder,
                        cursorColor = colors.primaryPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.virax_sni_spoof_explanation),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.cardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.virax_sni_scope_question),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val scopeOptions = listOf(
                    SniSpoofScope.SINGLE_SERVER to R.string.virax_sni_scope_single_server,
                    SniSpoofScope.MULTI_SERVER to R.string.virax_sni_scope_multi_server,
                    SniSpoofScope.SUBSCRIPTION to R.string.virax_sni_scope_subscription,
                    SniSpoofScope.GLOBAL to R.string.virax_sni_scope_global,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scopeOptions.forEach { (itemScope, labelRes) ->
                        val isSelected = scope == itemScope
                        val labelText = stringResource(labelRes)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) colors.darkPurple.copy(alpha = 0.55f)
                                    else colors.cardBackground
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primaryPurple else colors.cardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        scope = itemScope
                                        validationErrorRes = null
                                        if (itemScope == SniSpoofScope.SINGLE_SERVER && selectedServerGuids.size > 1) {
                                            selectedServerGuids = setOfNotNull(selectedServerGuids.firstOrNull())
                                        }
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = colors.primaryPurple)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = labelText,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                when (scope) {
                    SniSpoofScope.SUBSCRIPTION -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.virax_sni_select_subs_hint),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (subscriptionOptions.isEmpty()) {
                            EmptyTargetBanner(text = stringResource(R.string.virax_no_subscriptions))
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 190.dp)
                                ) {
                                    items(
                                        items = subscriptionOptions,
                                        key = { it.id }
                                    ) { sub ->
                                        val isChecked = sub.id in selectedSubscriptionIds
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .toggleable(
                                                    value = isChecked,
                                                    role = Role.Checkbox,
                                                    onValueChange = { checked ->
                                                        validationErrorRes = null
                                                        selectedSubscriptionIds = if (checked) {
                                                            selectedSubscriptionIds + sub.id
                                                        } else {
                                                            selectedSubscriptionIds - sub.id
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = colors.primaryPurple,
                                                    checkmarkColor = Color.White,
                                                    uncheckedColor = colors.textMuted
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = sub.remarks,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SniSpoofScope.SINGLE_SERVER,
                    SniSpoofScope.MULTI_SERVER -> {
                        val singleOnly = scope == SniSpoofScope.SINGLE_SERVER
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.virax_sni_select_servers_hint),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (serverOptions.isEmpty()) {
                            EmptyTargetBanner(text = stringResource(R.string.virax_no_servers_found))
                        } else {
                            if (serverOptions.size > 5) {
                                OutlinedTextField(
                                    value = serverQuery,
                                    onValueChange = { serverQuery = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.virax_sni_search_servers),
                                            color = colors.textMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary,
                                        focusedBorderColor = colors.primaryPurple,
                                        unfocusedBorderColor = colors.cardBorder,
                                        cursorColor = colors.primaryPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 210.dp)
                                ) {
                                    items(
                                        items = filteredServers,
                                        key = { it.guid }
                                    ) { server ->
                                        val isChosen = server.guid in selectedServerGuids
                                        val toggleSelection: () -> Unit = {
                                            validationErrorRes = null
                                            selectedServerGuids = if (singleOnly) {
                                                setOf(server.guid)
                                            } else if (isChosen) {
                                                selectedServerGuids - server.guid
                                            } else {
                                                selectedServerGuids + server.guid
                                            }
                                        }
                                        val rowModifier = if (singleOnly) {
                                            Modifier.selectable(
                                                selected = isChosen,
                                                role = Role.RadioButton,
                                                onClick = toggleSelection
                                            )
                                        } else {
                                            Modifier.toggleable(
                                                value = isChosen,
                                                role = Role.Checkbox,
                                                onValueChange = { toggleSelection() }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(rowModifier)
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (singleOnly) {
                                                RadioButton(
                                                    selected = isChosen,
                                                    onClick = null,
                                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primaryPurple)
                                                )
                                            } else {
                                                Checkbox(
                                                    checked = isChosen,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = colors.primaryPurple,
                                                        checkmarkColor = Color.White,
                                                        uncheckedColor = colors.textMuted
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                painter = painterResource(
                                                    id = ViraxFlagUtils.getFlagDrawable(server.remarks)
                                                ),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clearAndSetSemantics {}
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = server.remarks,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SniSpoofScope.GLOBAL -> Unit
                }

                validationErrorRes?.let { errorRes ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(errorRes),
                        color = colors.disconnectRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.virax_cancel),
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val normalizedGuids = if (scope == SniSpoofScope.SINGLE_SERVER) {
                                setOfNotNull(selectedServerGuids.firstOrNull())
                            } else {
                                selectedServerGuids
                            }
                            val candidate = SniSpoofConfig(
                                enabled = enabled,
                                fakeSni = fakeSni.trim(),
                                connectIp = connectIp.trim(),
                                scope = scope,
                                targetServerGuids = normalizedGuids,
                                targetSubscriptionIds = selectedSubscriptionIds,
                            )
                            if (candidate.enabled) {
                                if (!CoreSniSpoofManager.hasRequiredFields(candidate)) {
                                    validationErrorRes = R.string.virax_sni_validation_fields
                                    return@Button
                                }
                                if (!CoreSniSpoofManager.hasValidTargetSelection(candidate)) {
                                    validationErrorRes = R.string.virax_sni_validation_target
                                    return@Button
                                }
                            }
                            onSave(candidate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.virax_save),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTargetBanner(text: String) {
    val colors = ViraxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}
