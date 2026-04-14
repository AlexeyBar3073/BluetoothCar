// Файл: ui/screens/settings/SettingsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditDialogData
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditValueDialog
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.OtaDialog
import com.alexbar3073.bluetoothcar.data.bluetooth.OtaManager
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import kotlin.math.roundToInt

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

    // --- LAUNCHER ДЛЯ OTA (ВЫБОР ФАЙЛА ПРОШИВКИ) ---

    val otaFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Пытаемся получить имя файла из URI (через DocumentFile или ContentResolver)
            // Для упрощения передаем URI и просим ViewModel запустить процесс
            // Валидация имени файла произойдет внутри OtaManager
            val fileName = it.lastPathSegment ?: "firmware.bin"
            viewModel.startOtaUpdate(it, fileName) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val otaState by viewModel.otaState.collectAsStateWithLifecycle()
    val carData by viewModel.carData.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    // Состояния для отображения предупреждающих диалогов
    var showNoConnectionDialog by remember { mutableStateOf(false) }
    var showEngineRunningDialog by remember { mutableStateOf(false) }

    SettingsScreenContent(
        appSettings = appSettings,
        selectedDevice = selectedDevice,
        navController = navController,
        otaState = otaState,
        isConnected = isConnected,
        onUpdateSettings = { viewModel.updateSettings(it) },
        onClearSelectedDevice = { viewModel.clearSelectedDevice() },
        onImportErrors = { errorFileLauncher.launch("*/*") },
        onExportErrors = { exportErrorLauncher.launch("ecu_errors_backup.json") },
        onStartOta = { 
            if (!isConnected) {
                // ПРАВИЛО 1: Открываем диалог согласно задаче
                showNoConnectionDialog = true
            } else if (carData.engineStatus) {
                // ПРАВИЛО 1: Заменяем Toast на диалог по требованию пользователя
                showEngineRunningDialog = true
            } else {
                otaFileLauncher.launch(arrayOf("application/octet-stream", "application/x-binary", "application/bin"))
            }
        },
        onResetOta = { viewModel.resetOtaState() },
        showNoConnectionDialog = showNoConnectionDialog,
        onDismissNoConnectionDialog = { showNoConnectionDialog = false },
        showEngineRunningDialog = showEngineRunningDialog,
        onDismissEngineRunningDialog = { showEngineRunningDialog = false }
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
    otaState: OtaManager.OtaState = OtaManager.OtaState.Idle,
    carData: com.alexbar3073.bluetoothcar.data.models.CarData = com.alexbar3073.bluetoothcar.data.models.CarData(),
    isConnected: Boolean = false,
    showNoConnectionDialog: Boolean = false,
    onDismissNoConnectionDialog: () -> Unit = {},
    showEngineRunningDialog: Boolean = false,
    onDismissEngineRunningDialog: () -> Unit = {},
    onUpdateSettings: (AppSettings) -> Unit,
    onClearSelectedDevice: () -> Unit,
    onImportErrors: () -> Unit = {},
    onExportErrors: () -> Unit = {},
    onStartOta: () -> Unit = {},
    onResetOta: () -> Unit = {}
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
                                "Количество форсунок" -> appSettings.copy(injectorCount = newValue.roundToInt())
                                "Сигналы датчика скорости" -> appSettings.copy(speedSensorSignalsPerMeter = newValue.roundToInt())
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
                    onStartOta = onStartOta,
                    isEngineRunning = carData.engineStatus,
                    isConnected = isConnected
                )
            }
        }

        if (otaState !is OtaManager.OtaState.Idle) {
            OtaDialog(
                state = otaState,
                onDismiss = onResetOta
            )
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

        // ДИАЛОГ ПРЕДУПРЕЖДЕНИЯ ОБ ОТСУТСТВИИ СВЯЗИ
        // ПРАВИЛО 1 и 4: Оформлен в едином стиле приложения (непрозрачный фон, бордюр)
        if (showNoConnectionDialog) {
            Dialog(
                onDismissRequest = onDismissNoConnectionDialog,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppColors.DialogBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.DialogBackground,
                        contentColor = AppColors.TextPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "НЕТ СОЕДИНЕНИЯ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Text(
                            text = "Для обновления прошивки необходимо активное Bluetooth-соединение с бортовым компьютером. Пожалуйста, подключитесь к устройству и повторите попытку.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onDismissNoConnectionDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "ПОНЯТНО",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ДИАЛОГ ПРЕДУПРЕЖДЕНИЯ О ЗАПУЩЕННОМ ДВИГАТЕЛЕ
        // ПРАВИЛО 1 и 4: Оформлен в едином стиле приложения (непрозрачный фон, бордюр)
        if (showEngineRunningDialog) {
            Dialog(
                onDismissRequest = onDismissEngineRunningDialog,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppColors.DialogBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.DialogBackground,
                        contentColor = AppColors.TextPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "ДВИГАТЕЛЬ ЗАПУЩЕН",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Text(
                            text = "Обновление прошивки при запущенном двигателе небезопасно. Пожалуйста, заглушите двигатель и повторите попытку.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onDismissEngineRunningDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "ПОНЯТНО",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
