// Файл: ui/screens/DevicesReactivityTest.kt
package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.devices.DevicesScreen
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ТЕГ: UI/Devices/ReactivityTest
 */
@RunWith(AndroidJUnit4::class)
class DevicesReactivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun devicesScreen_updatesWhenBluetoothStateChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockViewModel = mockk<SharedViewModel>(relaxed = true)
        
        // Используем реальные StateFlow, так как MockK возвращает прокси, которые падают в Compose
        val connectionStateFlow = MutableStateFlow(ConnectionState.UNDEFINED)
        val isInitializedFlow = MutableStateFlow(true)
        val selectedDeviceFlow = MutableStateFlow(BluetoothDeviceData.empty())
        val appSettingsFlow = MutableStateFlow(AppSettings())
        
        val pairedDevices = listOf(BluetoothDeviceData("Car", "00:11:22:33:44:55"))
        val navController = TestNavHostController(context)
        
        every { mockViewModel.connectionState } returns connectionStateFlow
        every { mockViewModel.isInitialized } returns isInitializedFlow
        every { mockViewModel.selectedDevice } returns selectedDeviceFlow
        every { mockViewModel.appSettings } returns appSettingsFlow
        every { mockViewModel.getPairedDevices() } returns pairedDevices
        every { mockViewModel.isBluetoothEnabled() } returns true

        composeTestRule.setContent {
            BluetoothCarTheme {
                DevicesScreen(
                    navController = navController,
                    sharedViewModel = mockViewModel
                )
            }
        }

        // Проверяем начальное состояние
        composeTestRule.onNodeWithText("Car", substring = true).assertIsDisplayed()

        // Эмитируем изменение состояния
        connectionStateFlow.value = ConnectionState.DISCONNECTED
        
        // Проверка
        composeTestRule.onNodeWithText("00:11:22:33:44:55", substring = true).assertIsDisplayed()
    }
}
