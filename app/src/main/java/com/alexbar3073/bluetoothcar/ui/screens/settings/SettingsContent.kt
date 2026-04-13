// Файл: ui/screens/settings/SettingsContent.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.settings.components.DatabaseSection
import com.alexbar3073.bluetoothcar.ui.screens.settings.components.InfoSection
import com.alexbar3073.bluetoothcar.ui.screens.settings.components.SettingsSection
import com.alexbar3073.bluetoothcar.ui.screens.settings.components.WidgetsSection
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditDialogData
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ФАЙЛ: ui/screens/settings/SettingsContent.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Основное содержимое экрана настроек. Объединяет все секции настроек:
 * 1. Основные параметры автомобиля (SettingsSection)
 * 2. Управление базами данных (DatabaseSection)
 * 3. Настройки приложения (WidgetsSection)
 * 4. О приложении (InfoSection)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Является композаблой-контейнером для всех секций настроек
 * - Управляет вертикальной прокруткой контента
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: SettingsSection.kt, DatabaseSection.kt, WidgetsSection.kt, InfoSection.kt
 * 2. Вызывается из: SettingsScreen.kt
 */

@Composable
fun SettingsContent(
    appSettings: AppSettings,
    selectedDevice: BluetoothDeviceData?,
    navController: NavController,
    onEditDialogShow: (EditDialogData) -> Unit,
    onDeviceClear: () -> Unit,
    onUpdateSetting: (AppSettings) -> Unit,
    onImportErrors: () -> Unit,
    onExportErrors: () -> Unit,
    onStartOta: () -> Unit,
    isEngineRunning: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp)
    ) {
        // 1. Секция основных параметров автомобиля
        SectionHeader("ОСНОВНЫЕ ПАРАМЕТРЫ АВТОМОБИЛЯ")
        SettingsSection(
            appSettings = appSettings,
            selectedDevice = selectedDevice,
            navController = navController,
            onEditDialogShow = onEditDialogShow,
            onDeviceClear = onDeviceClear,
            onDeviceSelect = {
                navController.navigate("devices")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Секция управления базами данных (заголовок внутри DatabaseSection)
        DatabaseSection(
            onImportErrors = onImportErrors,
            onExportErrors = onExportErrors
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Секция настроек приложения
        SectionHeader("НАСТРОЙКИ ПРИЛОЖЕНИЯ")
        WidgetsSection(
            appSettings = appSettings,
            onUpdateSetting = onUpdateSetting
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Секция информации о приложении
        SectionHeader("О ПРИЛОЖЕНИИ")
        // ОБНОВЛЕНИЕ ПРОТОКОЛА: Передаем версию прошивки БК из настроек и коллбэк для OTA
        InfoSection(
            firmwareVersion = appSettings.firmwareVersion,
            isEngineRunning = isEngineRunning,
            onUpdateClick = onStartOta
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Футер
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BLUETOOTH CAR CONTROL",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.WhiteAlpha30
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Вспомогательный компонент для отрисовки заголовка секции настроек.
 * Вынесен для соблюдения единообразия стиля во всем файле.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.PrimaryBlue,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}
