// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/screens/home/HomeScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.StatusCircleButton
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4.DashboardType4
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.ColorPickerDialog
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import androidx.compose.runtime.Composable

/**
 * ТЕГ: Домашний экран
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Главный экран приложения. Отвечает за визуализацию панели приборов (Dashboard) 
 * и предоставление доступа к основным функциям управления (статус связи, настройки).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Получает данные из: SharedViewModel.kt (реактивные StateFlow).
 * 2. Вызывает: DashboardType4.kt.
 * 3. Навигация: Инициирует переход в SettingsScreen.kt.
 */

/**
 * Основная точка входа для Домашнего экрана.
 * Подписывается на потоки данных и делегирует отрисовку функции контента.
 */
@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    navigateToSettings: () -> Unit
) {
    // Получение текущего состояния приложения (гарантированно не null)
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
 * Контентная часть Домашнего экрана.
 * Реализована без прямой привязки к ViewModel для возможности использования в Preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    selectedDevice: BluetoothDeviceData?,
    carData: CarData,
    connectionStatusInfo: ConnectionStatusInfo,
    appSettings: AppSettings,
    onRetryConnection: () -> Unit,
    onTripReset: (String) -> Unit = {},
    onSettingsUpdate: (AppSettings) -> Unit = {},
    navigateToSettings: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    BluetoothCarTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Иконка авто, активна при наличии выбранного устройства
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = "Бортовой компьютер",
                                tint = if (selectedDevice != null) AppColors.PrimaryBlue else AppColors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "БОРТОВОЙ КОМПЬЮТЕР",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        // Кнопка статуса. Разрешаем клик всегда для инициирования процесса проверки в BCM.
                        StatusCircleButton(
                            connectionStatusInfo = connectionStatusInfo,
                            onClick = {
                                // Блокировка снята: вызываем переподключение в любом состоянии,
                                // чтобы BCM мог проанализировать ситуацию и выдать уведомление (Toast) через CFC.
                                onRetryConnection()
                            }
                        )
                    },
                    actions = {
                        // Кнопка перехода в раздел настроек
                        IconButton(
                            onClick = navigateToSettings,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        AppColors.SurfaceMedium,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Настройки",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.SurfaceDark,
                        titleContentColor = AppColors.TextPrimary
                    ),
                    modifier = Modifier
                        .height(COMPACT_TOP_BAR_HEIGHT)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = AppColors.SurfaceGradient
                            )
                        ),
                    windowInsets = WindowInsets(0.dp)
                )
            }
        ) { paddingValues ->
            // Основное рабочее пространство экрана
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        // Обработка длительного нажатия на пустую область экрана (подложку)
                        detectTapGestures(
                            onLongPress = {
                                showColorPicker = true
                            }
                        )
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Отрисовка выбранного типа дашборда (Дашборд 4)
                    DashboardType4(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings,
                        onTripReset = onTripReset,
                        onLongPress = { showColorPicker = true } // Передаем вызов диалога внутрь дашборда
                    )
                }
            }

            // Отображение диалога выбора цвета
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

// ==================== PREVIEWS ====================

/**
 * Превью домашнего экрана в стандартном рабочем режиме.
 */
@Preview(
    name = "Home - Normal",
    device = "spec:width=642dp,height=360dp,dpi=480",
    showBackground = true
)
@Composable
fun HomeScreenNormalPreview() {
    val fakeCarData = CarData(
        speed = 60f,
        voltage = 14.2f,
        fuel = 35f, 
        coolantTemp = 90f,
        transmissionTemp = 80f,
        isFuelLow = false,
        tirePressureLow = false,
        washerFluidLow = false
    )

    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = fakeCarData,
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(fuelTankCapacity = 60f, minFuelLevel = 5f),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}

/**
 * Превью домашнего экрана с активными предупреждениями.
 */
@Preview(
    name = "Home - Warnings",
    device = "spec:width=642dp,height=360dp,dpi=480",
    showBackground = true
)
@Composable
fun HomeScreenWarningsPreview() {
    val fakeCarData = CarData(
        speed = 0f,
        voltage = 11.8f,
        fuel = 3f, 
        coolantTemp = 105f,
        transmissionTemp = 95f,
        ecuErrors = "P0300",
        tirePressureLow = true,
        washerFluidLow = true,
        isFuelLow = true
    )

    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("My Car", "00:11:22:33:44:55"),
        carData = fakeCarData,
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(fuelTankCapacity = 60f, minFuelLevel = 5f),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}
