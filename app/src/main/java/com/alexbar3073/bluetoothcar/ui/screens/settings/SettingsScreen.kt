// Файл: ui/screens/SettingsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditDialogData
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditValueDialog
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.ThemeSelectionDialog
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ФАЙЛ: ui/screens/SettingsScreen.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Главный экран настроек приложения. Отображает все настройки приложения,
 * управляет диалогами редактирования и взаимодействует с SharedViewModel.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Отображение всех настроек приложения
 * 2. Управление диалогами редактирования (EditValueDialog, ThemeSelectionDialog)
 * 3. Взаимодействие с SharedViewModel для получения и обновления настроек
 * 4. Навигация назад и к выбору устройств
 * 5. Обработка состояния экрана (диалоги, загрузка)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Получает все данные из SharedViewModel через StateFlow
 * - Все обновления настроек выполняются через SharedViewModel
 * - Управление диалогами осуществляется локальным состоянием
 * - Не содержит бизнес-логики, только UI логику
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: SettingsContent.kt для основного содержимого
 * 2. Использует: SharedViewModel.kt для получения и обновления данных
 * 3. Использует: EditValueDialog.kt, ThemeSelectionDialog.kt для диалогов
 * 4. Взаимодействует: NavController для навигации
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.05 16:30: АДАПТАЦИЯ к текущему SharedViewModel
 *   1. Заменен метод updateSetting{...} на прямой вызов updateSettings()
 *   2. Исправлен clearDevice() -> clearSelectedDevice()
 *   3. Упрощена логика обновления настроек
 * - 2026.02.05 16:45: Добавлено оформление согласно требованиям ТЗ
 * - 2026.02.05 21:50: ОБНОВЛЕНИЕ ТОП-БАРА
 *   1. Заменена точка на иконку настроек
 *   2. Использована компактная высота топ-бара
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Состояние экрана
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editDialogData by remember { mutableStateOf(EditDialogData()) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Отладочный вывод для отслеживания изменений
    LaunchedEffect(appSettings) {
        Log.d("SettingsScreen", "Обновление appSettings в UI: ${appSettings?.let { it }}")
    }

    BluetoothCarTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Иконка настроек вместо точки
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = AppColors.PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "НАСТРОЙКИ",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
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
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.SurfaceDark,
                        titleContentColor = AppColors.TextPrimary,
                        navigationIconContentColor = AppColors.TextSecondary
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                // Основное содержимое настроек
                if (appSettings != null) {
                    SettingsContent(
                        appSettings = appSettings!!,
                        selectedDevice = selectedDevice,
                        navController = navController,
                        onEditDialogShow = { data ->
                            editDialogData = data.copy(onSave = { newValue ->
                                // СОГЛАСНО ТЗ: Обновляем настройки через прямой вызов
                                val currentSettings = appSettings!!
                                val updatedSettings = when (data.title) {
                                    "Объем топливного бака" ->
                                        currentSettings.copy(fuelTankCapacity = newValue)

                                    "Мин. остаток топлива" ->
                                        currentSettings.copy(minFuelLevel = newValue)

                                    "Производительность форсунки" ->
                                        currentSettings.copy(injectorPerformance = newValue)

                                    "Количество форсунок" ->
                                        currentSettings.copy(injectorCount = newValue.toInt())

                                    "Сигналы датчика скорости" ->
                                        currentSettings.copy(speedSensorSignalsPerMeter = newValue.toInt())

                                    "Интервал обновления" ->
                                        currentSettings.copy(updateInterval = newValue.toInt())

                                    else -> currentSettings
                                }

                                // Вызываем метод обновления настроек
                                viewModel.updateSettings(updatedSettings)
                            })
                            showEditDialog = true
                        },
                        onThemeDialogShow = { showThemeDialog = true },
                        onDeviceClear = {
                            // Очищаем выбранное устройство
                            viewModel.clearSelectedDevice()
                        },
                        onTestButtonClick = {
                            Log.d("SettingsScreen", "Тест переключателя")
                            val currentSettings = appSettings!!
                            val updatedSettings = currentSettings.copy(
                                showSpeedometer = !currentSettings.showSpeedometer
                            )
                            viewModel.updateSettings(updatedSettings)
                        },
                        onUpdateSetting = { newSettings ->
                            // Прямой вызов обновления настроек
                            viewModel.updateSettings(newSettings)
                        }
                    )
                }
            }
        }

        // Диалог редактирования значения
        if (showEditDialog) {
            BluetoothCarTheme {
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

        // Диалог выбора темы
        if (showThemeDialog) {
            BluetoothCarTheme {
                ThemeSelectionDialog(
                    currentTheme = appSettings?.selectedTheme ?: "system",
                    onDismiss = { showThemeDialog = false },
                    onThemeSelected = { selectedTheme ->
                        val currentSettings = appSettings
                        if (currentSettings != null) {
                            val updatedSettings = currentSettings.copy(
                                selectedTheme = selectedTheme
                            )
                            viewModel.updateSettings(updatedSettings)
                        }
                        showThemeDialog = false
                    }
                )
            }
        }
    }
}
