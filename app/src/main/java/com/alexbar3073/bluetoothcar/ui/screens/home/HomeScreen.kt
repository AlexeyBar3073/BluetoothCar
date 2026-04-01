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
import androidx.compose.ui.graphics.Color
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
 */

@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    navigateToSettings: () -> Unit
) {
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
    var showColorPicker by remember { mutableStateOf(false) }

    // Принудительно используем темную тему, согласно задаче "Dark Only"
    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            // Устанавливаем прозрачный фон для контейнера, чтобы видеть наш градиент
            containerColor = Color.Transparent,
            topBar = {
                CompactTopBar(
                    title = "БОРТОВОЙ КОМПЬЮТЕР",
                    titleIcon = Icons.Filled.DirectionsCar,
                    titleIconTint = if (selectedDevice.isValidDevice()) AppColors.BluetoothDeviceConnected else AppColors.TextTertiary,
                    leftContent = {
                        StatusCircleButton(
                            connectionStatusInfo = connectionStatusInfo,
                            onClick = onRetryConnection
                        )
                    },
                    rightContent = {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Наш эталонный градиент из DASHBOARD_4_SPEC
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { showColorPicker = true })
                    }
            ) {
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

// ================== PREVIEWS ==================

/**
 * Данные для превью в рабочем режиме. 
 * Используются для проверки корректности масштабирования элементов при разной плотности пикселей.
 */
private val previewData = CarData(
    speed = 85f,
    voltage = 14.2f,
    fuel = 42f, 
    coolantTemp = 90f,
    transmissionTemp = 82f,
    tripA = 125.4f,
    odometer = 54320f,
    isFuelLow = false,
    tirePressureLow = false,
    washerFluidLow = false
)

/**
 * Превью домашнего экрана для формата Full HD (1080P).
 * Параметры устройства (642dp x 360dp, 480dpi) соответствуют расчетным пропорциям для 1080P панелей.
 */
@Preview(
    name = "Home - 1080P",
    device = "spec:width=642dp,height=360dp,dpi=480",
    showBackground = true
)
@Composable
fun Preview1080() {
    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = previewData,
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(selectedTheme = "dark", fuelTankCapacity = 60f),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}

/**
 * Превью домашнего экрана для формата HD (720P).
 * Параметры устройства (624dp x 360dp, 320dpi) соответствуют расчетным пропорциям для 720P панелей.
 */
@Preview(
    name = "Home - 720P",
    device = "spec:width=624dp,height=360dp,dpi=320",
    showBackground = true
)
@Composable
fun Preview720() {
    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = previewData,
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(selectedTheme = "dark", fuelTankCapacity = 60f),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}
