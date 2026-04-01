// Файл: ui/screens/SettingsNavigationTest.kt
package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.settings.SettingsScreen
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ТЕГ: UI/Settings/NavigationTest
 *
 * ФАЙЛ: ui/screens/SettingsNavigationTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: /src/androidTest/java/com/alexbar3073/bluetoothcar/ui/screens/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Тестирование переходов (навигации) с экрана настроек.
 */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Тест: Проверка перехода на экран выбора устройств при клике на соответствующий пункт.
     */
    @Test
    fun clickingDeviceSetting_navigatesToDevices() {
        // 1. Подготовка
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val navController = TestNavHostController(context)
        val mockViewModel = mockk<SharedViewModel>(relaxed = true)
        
        // Создаем реальные MutableStateFlow для избежания ClassCastException
        val appSettingsFlow = MutableStateFlow(AppSettings())
        val selectedDeviceFlow = MutableStateFlow(BluetoothDeviceData.empty())
        val isInitializedFlow = MutableStateFlow(true)
        
        every { mockViewModel.appSettings } returns appSettingsFlow
        every { mockViewModel.selectedDevice } returns selectedDeviceFlow
        every { mockViewModel.isInitialized } returns isInitializedFlow

        // 2. Установка NavHost для теста
        composeTestRule.setContent {
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            BluetoothCarTheme {
                NavHost(navController = navController, startDestination = "settings") {
                    composable("settings") {
                        SettingsScreen(navController = navController, viewModel = mockViewModel)
                    }
                    composable("devices") {
                        // Заглушка целевого экрана
                    }
                }
            }
        }

        // 3. Выполнение клика
        // ИСПРАВЛЕНО: Текст в SettingsSection.kt — "Устройство данных"
        composeTestRule.onNodeWithText("Устройство данных", substring = true).performClick()

        // 4. Проверка результата навигации
        assertEquals("devices", navController.currentBackStackEntry?.destination?.route)
    }
}
