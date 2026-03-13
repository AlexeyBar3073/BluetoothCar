// Файл: ui/screens/settings/SettingsContent.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
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
 * 1. Основные настройки (SettingsSection)
 * 2. Настройки виджетов (WidgetsSection)
 * 3. Информация о приложении (InfoSection)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Является композаблой-контейнером для всех секций настроек
 * - Управляет вертикальной прокруткой контента
 * - Не содержит бизнес-логики, только композицию UI
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: SettingsSection.kt, WidgetsSection.kt, InfoSection.kt
 * 2. Вызывается из: SettingsScreen.kt
 * 3. Использует: EditDialogData.kt для передачи данных в диалоги
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.05 16:40: Убран параметр scope для совместимости с текущей архитектурой
 * - 2026.02.05 16:45: Добавлено оформление согласно требованиям ТЗ
 * - 2026.02.05 17:10: Исправлен вызов SettingsSection (убраны параметры scope)
 */

@Composable
fun SettingsContent(
    appSettings: AppSettings,
    selectedDevice: BluetoothDeviceData?,
    navController: NavController,
    onEditDialogShow: (EditDialogData) -> Unit,
    onThemeDialogShow: () -> Unit,
    onDeviceClear: () -> Unit,
    onTestButtonClick: () -> Unit,
    onUpdateSetting: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp)
    ) {
        // 1. Секция основных настроек
        SettingsSection(
            appSettings = appSettings,
            selectedDevice = selectedDevice,
            navController = navController,
            onEditDialogShow = onEditDialogShow,
            onThemeDialogShow = onThemeDialogShow,
            onDeviceClear = onDeviceClear,
            onDeviceSelect = {
                if (selectedDevice == null) {
                    navController.navigate("devices")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Секция настроек виджетов
        WidgetsSection(
            appSettings = appSettings,
            onUpdateSetting = onUpdateSetting
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Секция информации о приложении
        InfoSection(
            onTestClick = onTestButtonClick
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