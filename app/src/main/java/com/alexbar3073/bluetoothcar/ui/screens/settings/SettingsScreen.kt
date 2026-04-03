// Файл: ui/screens/settings/SettingsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
 * Предоставляет интерфейс для изменения параметров автомобиля, управления базами данных
 * (импорт/экспорт) и просмотра информации о приложении.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Использует: SharedViewModel.kt, SettingsContent.kt / Вызывается из: SetupNavigation.kt
 */

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    val context = LocalContext.current
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()

    // --- LAUNCHERS ДЛЯ ИМПОРТА (ЧТЕНИЕ ФАЙЛА) ---

    val errorFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importEcuErrors(it) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val combinationFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importEcuCombinations(it) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- LAUNCHERS ДЛЯ ЭКСПОРТА (СОХРАНЕНИЕ ФАЙЛА) ---

    val exportErrorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportEcuErrors(it) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportCombinationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportEcuCombinations(it) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    SettingsScreenContent(
        appSettings = appSettings,
        selectedDevice = selectedDevice,
        navController = navController,
        onUpdateSettings = { viewModel.updateSettings(it) },
        onClearSelectedDevice = { viewModel.clearSelectedDevice() },
        onImportErrors = { errorFileLauncher.launch("*/*") },
        onExportErrors = { exportErrorLauncher.launch("ecu_errors_backup.json") },
        onImportCombinations = { combinationFileLauncher.launch("*/*") },
        onExportCombinations = { exportCombinationLauncher.launch("ecu_combinations_backup.json") }
    )
}

/**
 * Основной UI-контент экрана настроек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    appSettings: AppSettings,
    selectedDevice: BluetoothDeviceData?,
    navController: NavController,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearSelectedDevice: () -> Unit,
    onImportErrors: () -> Unit = {},
    onExportErrors: () -> Unit = {},
    onImportCombinations: () -> Unit = {},
    onExportCombinations: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editDialogData by remember { mutableStateOf(EditDialogData()) }

    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            topBar = {
                CompactTopBar(
                    title = "НАСТРОЙКИ",
                    titleIcon = Icons.Default.Settings,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = { navController.popBackStack() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                SettingsContent(
                    appSettings = appSettings,
                    selectedDevice = selectedDevice,
                    navController = navController,
                    onEditDialogShow = { data ->
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
                    onUpdateSetting = onUpdateSettings,
                    onImportErrors = onImportErrors,
                    onExportErrors = onExportErrors,
                    onImportCombinations = onImportCombinations,
                    onExportCombinations = onExportCombinations
                )
            }
        }

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
