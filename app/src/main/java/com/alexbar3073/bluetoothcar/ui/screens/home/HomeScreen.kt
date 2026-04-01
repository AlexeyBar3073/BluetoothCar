// Файл: ui/screens/home/HomeScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.components.TopBarButton
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.StatusCircleButton
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4.DashboardType4
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.ColorPickerDialog
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Главный/Домашний/Экран
 * 
 * ФАЙЛ: ui/screens/home/HomeScreen.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Основной экран приложения. 
 * Предоставляет интерфейс бортового компьютера с визуализацией данных в реальном времени.
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение панели приборов, индикация статуса подключения, 
 * переход к настройкам и управление сбросом параметров поездки.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: MVVM.
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Центральный хаб приложения для мониторинга состояния автомобиля.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: Вызывается из NavHost. Взаимодействует с SharedViewModel, 
 * отображает DashboardType4. Использует CompactTopBar для заголовка.
 */

/**
 * Входная точка домашнего экрана. Связывает ViewModel с UI-контентом.
 */
@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    navigateToSettings: () -> Unit
) {
    // Подписка на ключевые StateFlow из ViewModel
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val carData by viewModel.carData.collectAsStateWithLifecycle()
    val connectionStatusInfo by viewModel.connectionStatusInfo.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()

    HomeScreenContent(
        selectedDevice = selectedDevice,
        carData = carData,
        connectionStatusInfo = connectionStatusInfo,
        appSettings = appSettings,
        onRetryConnection = { viewModel.retryConnection() },
        onTripReset = { command -> viewModel.sendJsonCommand(command) },
        onSettingsUpdate = { viewModel.updateSettings(it) },
        navigateToSettings = navigateToSettings
    )
}

/**
 * Основной UI-контент домашнего экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    selectedDevice: BluetoothDeviceData,
    carData: CarData,
    connectionStatusInfo: ConnectionStatusInfo,
    appSettings: AppSettings,
    onRetryConnection: () -> Unit,
    onTripReset: (String) -> Unit = {},
    onSettingsUpdate: (AppSettings) -> Unit = {},
    navigateToSettings: () -> Unit
) {
    // Состояние отображения диалога выбора цвета
    var showColorPicker by remember { mutableStateOf(false) }

    // Инициализация контекста темы
    BluetoothCarTheme(themeMode = appSettings.selectedTheme) {
        Scaffold(
            topBar = {
                // Использование унифицированного компактного Топбара (40 DP)
                CompactTopBar(
                    title = "БОРТОВОЙ КОМПЬЮТЕР",
                    titleIcon = Icons.Filled.DirectionsCar,
                    // Использование BluetoothDeviceConnected (Blue80) вместо PrimaryBlue 
                    // обеспечивает яркую и заметную индикацию даже в светлой теме.
                    titleIconTint = if (selectedDevice.isValidDevice()) AppColors.BluetoothDeviceConnected else AppColors.TextTertiary,
                    leftContent = {
                        // Кнопка статуса унифицирована через TopBarButton (32/28 dp)
                        StatusCircleButton(
                            connectionStatusInfo = connectionStatusInfo,
                            onClick = onRetryConnection
                        )
                    },
                    rightContent = {
                        // Кнопка настроек унифицирована через TopBarButton (32/28 dp)
                        TopBarButton(
                            icon = Icons.Default.Settings,
                            onClick = navigateToSettings,
                            contentDescription = "Настройки",
                            tint = AppColors.TextSecondary
                        )
                    }
                )
            }
        ) { paddingValues ->
            // Основная рабочая область экрана
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        // Вызов настройки цвета по длинному нажатию на фон
                        detectTapGestures(onLongPress = { showColorPicker = true })
                    }
            ) {
                // Область отрисовки панели приборов
                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardType4(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings,
                        onTripReset = onTripReset,
                        onLongPress = { showColorPicker = true }
                    )
                }
            }

            // Диалог выбора акцентного цвета приборов
            if (showColorPicker) {
                ColorPickerDialog(
                    appSettings = appSettings,
                    onDismiss = { showColorPicker = false },
                    onColorSelected = { color ->
                        val updatedSettings = appSettings.copy(currentDashboardColor = color.toArgb().toLong())
                        onSettingsUpdate(updatedSettings)
                    }
                )
            }
        }
    }
}

@Preview(name = "Dark Theme", device = "spec:width=642dp,height=360dp,dpi=480", showBackground = true)
@Composable
fun PreviewDark() {
    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = CarData(speed = 60f),
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(selectedTheme = "dark"),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}

@Preview(name = "Light Theme", device = "spec:width=642dp,height=360dp,dpi=480", showBackground = true)
@Composable
fun PreviewLight() {
    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = CarData(speed = 45f),
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(selectedTheme = "light"),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}
