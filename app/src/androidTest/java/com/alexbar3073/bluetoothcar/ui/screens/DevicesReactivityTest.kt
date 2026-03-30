package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.devices.DevicesScreen
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Тест реактивности экрана выбора устройств.
 * Проверяет, что UI обновляется при изменении состояния Bluetooth через поток connectionStatusInfo.
 */
class DevicesReactivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun devicesScreen_updatesWhenBluetoothStateChanges() {
        // 1. Подготовка моков
        val mockNavController = mockk<NavController>(relaxed = true)
        val mockViewModel = mockk<SharedViewModel>(relaxed = true)
        
        // Потоки состояний
        val isInitializedFlow = MutableStateFlow(true)
        val selectedDeviceFlow = MutableStateFlow(BluetoothDeviceData.empty())
        // Используем метод toStatusInfo() объекта ConnectionState
        val connectionStatusFlow = MutableStateFlow(ConnectionState.CONNECTED.toStatusInfo())
        
        every { mockViewModel.isInitialized } returns isInitializedFlow
        every { mockViewModel.selectedDevice } returns selectedDeviceFlow
        every { mockViewModel.connectionStatusInfo } returns connectionStatusFlow
        
        // Настраиваем функции-геттеры
        every { mockViewModel.isBluetoothEnabled() } answers { 
            connectionStatusFlow.value.state != ConnectionState.ERROR 
        }
        every { mockViewModel.getPairedDevices() } returns emptyList()

        // 2. Установка контента (Bluetooth включен)
        composeTestRule.setContent {
            DevicesScreen(
                navController = mockNavController,
                sharedViewModel = mockViewModel
            )
        }

        // Проверяем, что заголовок отображается корректно при включенном BT
        composeTestRule.onNodeWithText("УСТРОЙСТВА BLUETOOTH").assertIsDisplayed()

        // 3. Действие: имитируем выключение Bluetooth (состояние ERROR)
        connectionStatusFlow.value = ConnectionState.ERROR.toStatusInfo()

        // 4. Проверка: UI должен показать сообщение о выключенном Bluetooth
        // Это доказывает, что подписка на connectionStatusInfo работает и вызывает рекомпозицию
        composeTestRule.onNodeWithText("Bluetooth выключен").assertIsDisplayed()
        composeTestRule.onNodeWithText("Включите Bluetooth в настройках системы для отображения устройств").assertIsDisplayed()
    }
}
