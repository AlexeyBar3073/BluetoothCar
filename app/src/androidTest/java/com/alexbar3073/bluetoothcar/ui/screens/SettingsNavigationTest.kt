package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.settings.SettingsScreen
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
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
        // 1. Подготовка моков и контроллера
        val mockViewModel = mockk<SharedViewModel>(relaxed = true)
        val appSettingsFlow = MutableStateFlow(AppSettings())
        val selectedDeviceFlow = MutableStateFlow(BluetoothDeviceData.empty())
        
        every { mockViewModel.appSettings } returns appSettingsFlow
        every { mockViewModel.selectedDevice } returns selectedDeviceFlow

        lateinit var navController: TestNavHostController

        // 2. Установка контента с реальным NavHost
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            // Настраиваем минимальный граф навигации
            NavHost(navController = navController, startDestination = "settings") {
                composable("settings") {
                    SettingsScreen(
                        navController = navController,
                        viewModel = mockViewModel
                    )
                }
                composable("devices") {
                    // Пустой экран назначения
                }
            }
        }

        // 3. Действие: клик по пункту "Устройство данных"
        composeTestRule.onNodeWithText("Устройство данных").performClick()

        // 4. Проверка: изменился ли текущий маршрут
        composeTestRule.waitForIdle()
        assertEquals("devices", navController.currentBackStackEntry?.destination?.route)
    }
}
