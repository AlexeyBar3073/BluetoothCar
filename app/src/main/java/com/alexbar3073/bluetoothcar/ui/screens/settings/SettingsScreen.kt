// Файл: ui/screens/settings/SettingsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditDialogData
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditValueDialog
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Настройки/Конфигурация/Screen
 * 
 * ФАЙЛ: ui/screens/settings/SettingsScreen.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Главный экран настроек приложения. 
 * Предоставляет интерфейс для изменения параметров автомобиля и внешнего вида приложения.
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение разделов настроек, управление диалогами редактирования 
 * значений.
 * 
 * ИЗМЕНЕНИЕ: Удалена логика выбора темы оформления (теперь только Dark).
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: MVVM (использует SharedViewModel).
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Централизованное управление конфигурацией пользователя.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: Вызывается из HomeScreen. Взаимодействует с SettingsContent, 
 * диалогом EditValueDialog. Использует CompactTopBar для заголовка.
 */

/**
 * Входная точка экрана настроек. Связывает состояния ViewModel с контентом.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Подписка на настройки и выбранное устройство
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()

    // Делегирование отрисовки контентной функции
    SettingsScreenContent(
        appSettings = appSettings,
        selectedDevice = selectedDevice,
        navController = navController,
        onUpdateSettings = { viewModel.updateSettings(it) },
        onClearSelectedDevice = { viewModel.clearSelectedDevice() }
    )
}

/**
 * Основной UI-контент экрана настроек с поддержкой Scaffold и TopBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    appSettings: AppSettings,
    selectedDevice: BluetoothDeviceData?,
    navController: NavController,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearSelectedDevice: () -> Unit
) {
    // Состояния для управления диалогами редактирования
    var showEditDialog by remember { mutableStateOf(false) }
    var editDialogData by remember { mutableStateOf(EditDialogData()) }

    // Обертка темы приложения (принудительно dark согласно задаче)
    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            topBar = {
                // Использование унифицированного компактного Топбара (40 DP)
                CompactTopBar(
                    title = "НАСТРОЙКИ",
                    titleIcon = Icons.Default.Settings,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = { navController.popBackStack() }
                )
            }
        ) { paddingValues ->
            // Область контента с градиентным фоном
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                // Вызов композиции со списком настроек
                SettingsContent(
                    appSettings = appSettings,
                    selectedDevice = selectedDevice,
                    navController = navController,
                    onEditDialogShow = { data ->
                        // Логика подготовки данных для диалога редактирования
                        editDialogData = data.copy(onSave = { newValue ->
                            val updatedSettings = when (data.title) {
                                "Объем топливного бака" -> appSettings.copy(fuelTankCapacity = newValue)
                                "Мин. остаток топлива" -> appSettings.copy(minFuelLevel = newValue)
                                "Производительность форсунки" -> appSettings.copy(injectorPerformance = newValue)
                                "Количество форсунок" -> appSettings.copy(injectorCount = newValue.toInt())
                                "Сигналы датчика скорости" -> appSettings.copy(speedSensorSignalsPerMeter = newValue.toInt())
                                else -> appSettings
                            }
                            onUpdateSettings(updatedSettings)
                        })
                        showEditDialog = true
                    },
                    onDeviceClear = onClearSelectedDevice,
                    onUpdateSetting = onUpdateSettings
                )
            }
        }

        // Диалог редактирования числовых значений
        if (showEditDialog) {
            EditValueDialog(
                data = editDialogData,
                onDismiss = { showEditDialog = false },
                onConfirm = { newValue ->
                    editDialogData.onSave(newValue)
                    showEditDialog = false
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 642, heightDp = 360)
@Composable
fun PreviewSettingsDark() {
    SettingsScreenContent(
        appSettings = AppSettings(selectedTheme = "dark"),
        selectedDevice = null,
        navController = rememberNavController(),
        onUpdateSettings = {},
        onClearSelectedDevice = {}
    )
}
