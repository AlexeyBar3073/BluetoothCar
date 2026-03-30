package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.settings.SettingsScreen
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Тест навигации экрана настроек.
 * Проверяет, что при клике на "Устройство данных" происходит переход на экран выбора устройств.
 */
class SettingsNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingDeviceSetting_navigatesToDevices() {
        // 1. Подготовка моков
        val mockNavController = mockk<NavController>(relaxed = true)
        val mockViewModel = mockk<SharedViewModel>(relaxed = true)
        
        // Настраиваем состояния Flow
        val appSettingsFlow = MutableStateFlow(AppSettings())
        val selectedDeviceFlow = MutableStateFlow(BluetoothDeviceData.empty())
        
        every { mockViewModel.appSettings } returns appSettingsFlow
        every { mockViewModel.selectedDevice } returns selectedDeviceFlow

        // 2. Установка контента
        composeTestRule.setContent {
            SettingsScreen(
                navController = mockNavController,
                viewModel = mockViewModel
            )
        }

        // 3. Действие: клик по пункту "Устройство данных"
        // В SimpleSettingItem заголовок "Устройство данных"
        composeTestRule.onNodeWithText("Устройство данных").performClick()

        // 4. Проверка: был ли вызван переход
        verify { mockNavController.navigate("devices") }
    }
}
