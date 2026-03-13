package com.alexbar3073.bluetoothcar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.StatusCircleButton
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1.DashboardType1
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ФАЙЛ: ui/screens/home/HomeScreen.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Главный экран приложения "Бортовой компьютер". Отображает состояние подключения,
 * данные от автомобиля и управляющие элементы.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Отображение статуса подключения и выбранного устройства
 * 2. Отображение данных от БК (скорость, RPM, температура и т.д.)
 * 3. Предоставление кнопок для управления подключением
 * 4. Навигация к экрану настроек
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Использует готовые данные из ConnectionStatusInfo без дополнительных преобразований
 * - Не содержит бизнес-логики, только отображение
 * - Все действия делегируются SharedViewModel
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Получает данные от: SharedViewModel.kt
 * 2. Использует виджеты: BluetoothStatusWidget.kt, CarDataWidget.kt
 * 3. Использует тему: ui/theme/Theme.kt
 * 4. Вызывает навигацию к: SettingsScreen.kt
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    navigateToSettings: () -> Unit
) {
    BluetoothCarTheme {
        // Получаем состояние из ViewModel
        val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
        val carData by viewModel.carData.collectAsStateWithLifecycle()
        val connectionStatusInfo by viewModel.connectionStatusInfo.collectAsStateWithLifecycle()
        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Иконка дома - меняем цвет в зависимости от выбора устройства
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
                    // В HomeScreen.kt меняем navigationIcon:

                    navigationIcon = {
                        // НОВАЯ КРУГЛАЯ КНОПКА СТАТУСА
                        StatusCircleButton(
                            connectionStatusInfo = connectionStatusInfo,
                            onClick = {
                                if (connectionStatusInfo.allowsManualRetry) {
                                    viewModel.retryConnection()
                                }
                            }
                        )
                    },
                    actions = {
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
                        titleContentColor = AppColors.TextPrimary,
                        navigationIconContentColor = AppColors.PrimaryBlue,
                        actionIconContentColor = AppColors.TextSecondary
                    ),
                    modifier = Modifier
                        .height(COMPACT_TOP_BAR_HEIGHT) // Меняем высоту
                        .background(
                            brush = Brush.verticalGradient(
                                colors = AppColors.SurfaceGradient
                            )
                        ),
                    windowInsets = WindowInsets(0.dp)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                // КАРТОЧКА СО СТАТУСОМ ПОДКЛЮЧЕНИЯ УБРАНА
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    DashboardType1(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings
                    )
                }
            }
        }
    }
}

/**
 * Элемент информации для отображения пар ключ-значение.
 * Используется в информационной карточке для компактного отображения данных.
 */
@Composable
private fun InfoItem(
    title: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Форматирует временную метку в читаемый формат.
 * Используется для отображения времени последнего обновления данных.
 */
private fun formatTime(timestamp: Long): String {
    val minutesAgo = (System.currentTimeMillis() - timestamp) / (1000 * 60)
    return when {
        minutesAgo < 1 -> "только что"
        minutesAgo < 60 -> "${minutesAgo.toInt()} мин назад"
        else -> "${(minutesAgo / 60).toInt()} ч назад"
    }
}