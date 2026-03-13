// Файл: navigation/SetupNavigation.kt
package com.alexbar3073.bluetoothcar.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alexbar3073.bluetoothcar.ui.screens.devices.DevicesScreen
import com.alexbar3073.bluetoothcar.ui.screens.home.HomeScreen
import com.alexbar3073.bluetoothcar.ui.screens.settings.SettingsScreen
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ФАЙЛ: navigation/SetupNavigation.kt
 * МЕСТОНАХОЖДЕНИЕ: navigation/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Основной файл навигации приложения BluetoothCar. Определяет структуру навигации
 * между всеми экранами приложения.
 *
 * АРХИТЕКТУРНАЯ РОЛЬ:
 * Навигационный компонент, реализующий паттерн Single Activity с несколькими Compose-экранами.
 * Является точкой входа для всей навигации в приложении.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Использует экраны:
 *    - HomeScreen.kt (главный экран с виджетами)
 *    - SettingsScreen.kt (экран настроек приложения)
 *    - DevicesScreen.kt (экран выбора Bluetooth устройств)
 *
 * 2. Работает с: SharedViewModel (центральный ViewModel для всего приложения)
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.05 17:30: ВОССТАНОВЛЕН экран DevicesScreen
 *   Убрана временная заглушка, добавлен реальный экран DevicesScreen
 */

@Composable
fun SetupNavigation(
    sharedViewModel: SharedViewModel,
    context: Context
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Главный экран с виджетами
        composable("home") {
            HomeScreen(
                viewModel = sharedViewModel,
                navigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // Экран настроек приложения
        composable("settings") {
            SettingsScreen(
                navController = navController,
                viewModel = sharedViewModel
            )
        }

        // Экран выбора Bluetooth устройств
        composable("devices") {
            DevicesScreen(
                navController = navController,
                sharedViewModel = sharedViewModel
            )
        }
    }
}