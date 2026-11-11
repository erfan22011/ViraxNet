package com.v2ray.ang.ui.settings

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.v2ray.ang.dto.SniSpoofConfig
import com.v2ray.ang.dto.SniSpoofScope
import com.v2ray.ang.dto.ViraxSniServerOption
import com.v2ray.ang.dto.ViraxSniSubscriptionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SettingsViewModelTest {
    @Test
    fun vpnSettingsVisibilityFollowsActivityAvailability() = runBlocking(Dispatchers.IO) {
        val packageManager = mock<PackageManager>()
        val application = mock<Application>()
        whenever(application.packageManager).thenReturn(packageManager)
        var activity: ComponentName? = null

        mockConstruction(Intent::class.java) { intent, context ->
            assertEquals(listOf(Settings.ACTION_VPN_SETTINGS), context.arguments())
            whenever(intent.resolveActivity(packageManager)).thenAnswer { activity }
        }.use {
            val viewModel = SettingsViewModel(application)
            assertFalse(viewModel.systemVpnSettingsAvailable.value)
            viewModel.refreshSystemVpnSettingsAvailability()
            assertFalse(viewModel.systemVpnSettingsAvailable.value)

            activity = mock<ComponentName>()
            viewModel.refreshSystemVpnSettingsAvailability()
            assertTrue(viewModel.systemVpnSettingsAvailable.value)

            activity = null
            viewModel.refreshSystemVpnSettingsAvailability()
            assertFalse(viewModel.systemVpnSettingsAvailable.value)
        }
    }

    @Test
    fun sniSpoofingStateLoadsAndPersistsUpdates() = runBlocking(Dispatchers.IO) {
        val application = mock<Application>()
        var persisted = SniSpoofConfig(
            enabled = false,
            fakeSni = "chatgpt.com",
            connectIp = "104.17.100.50",
            scope = SniSpoofScope.SINGLE_SERVER,
            targetServerGuids = setOf("srv-1")
        )
        val servers = listOf(
            ViraxSniServerOption(guid = "srv-1", remarks = "DE Node", subscriptionId = "sub-1")
        )
        val subs = listOf(
            ViraxSniSubscriptionOption(id = "sub-1", remarks = "VIP", isProtected = true)
        )

        val viewModel = SettingsViewModel(
            application = application,
            decodeSniConfig = { persisted },
            encodeSniConfig = { persisted = it },
            loadSniServers = { servers },
            loadSniSubscriptions = { subs },
            ioDispatcher = Dispatchers.Unconfined
        )

        assertEquals(SniSpoofConfig(), viewModel.sniSpoofConfig.value)
        viewModel.refreshSniSpoofingState()

        assertEquals(persisted, viewModel.sniSpoofConfig.value)
        assertEquals(servers, viewModel.sniServerOptions.value)
        assertEquals(subs, viewModel.sniSubscriptionOptions.value)

        val updated = persisted.copy(
            enabled = true,
            fakeSni = "custom.example.com",
            connectIp = "172.64.10.2",
            scope = SniSpoofScope.SUBSCRIPTION,
            targetSubscriptionIds = setOf("sub-1")
        )
        viewModel.applyAndSaveSniSpoofConfig(updated)
        assertEquals(updated, viewModel.sniSpoofConfig.value)
        assertEquals(updated, persisted)
    }
}
