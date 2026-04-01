// Файл: ui/screens/devices/DevicesScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.DeviceType
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Устройства/Bluetooth/Screen
 * 
 * ФАЙЛ: ui/screens/devices/DevicesScreen.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/devices/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Экран управления Bluetooth-подключениями. 
 * Позволяет выбирать устройства из списка сопряженных в системе.
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение списка сопряженных устройств, выбор активного устройства,
 * индикация статуса Bluetooth.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: MVVM (использует SharedViewModel).
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Реактивное обновление состояния списка устройств через StateFlow.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: Вызывается из навигации (NavHost), использует SharedViewModel 
 * для бизнес-логики и AppColors для оформления. Использует CompactTopBar для заголовка.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    // Подписка на состояния из ViewModel с учетом жизненного цикла
    val isInitialized by sharedViewModel.isInitialized.collectAsStateWithLifecycle()
    val selectedDevice by sharedViewModel.selectedDevice.collectAsStateWithLifecycle()
    val appSettings by sharedViewModel.appSettings.collectAsStateWithLifecycle()

    // Проверка состояния Bluetooth и получение списка сопряженных устройств
    val isBluetoothEnabled = if (isInitialized) sharedViewModel.isBluetoothEnabled() else false
    val pairedDevices = if (isInitialized) sharedViewModel.getPairedDevices() ?: emptyList() else emptyList()

    // Применение темы оформления приложения
    BluetoothCarTheme(themeMode = appSettings.selectedTheme) {
        Scaffold(
            topBar = {
                // Использование унифицированного компактного Топбара (40 DP)
                CompactTopBar(
                    title = "УСТРОЙСТВА BLUETOOTH",
                    titleIcon = if (isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    titleIconTint = if (isBluetoothEnabled) AppColors.PrimaryBlue else AppColors.Error,
                    navigationIcon = Icons.Default.ArrowBack,
                    onNavigationClick = { navController.popBackStack() }
                )
            }
        ) { paddingValues ->
            // Основной контент экрана с градиентным фоном
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 20.dp)
                ) {
                    // 1. Отображение карточки текущего выбранного устройства
                    if (selectedDevice.isValidDevice()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // Заголовок блока текущего устройства
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.BluetoothConnected,
                                        null,
                                        tint = AppColors.PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "ТЕКУЩЕЕ УСТРОЙСТВО",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.PrimaryBlue
                                    )
                                }
                                
                                // Разделитель
                                Divider(
                                    color = AppColors.SurfaceMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                // Информация об устройстве (Имя, Тип и MAC-адрес)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        null,
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            selectedDevice.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = AppColors.TextPrimary
                                        )
                                        // Отображение типа устройства
                                        Text(
                                            selectedDevice.deviceType.getDisplayName(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextSecondary
                                        )
                                        // MAC-адрес
                                        Text(
                                            selectedDevice.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppColors.TextSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 2. Список сопряженных устройств
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            // Заголовок списка сопряженных устройств
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    null,
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "СОПРЯЖЕННЫЕ УСТРОЙСТВА",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "(${pairedDevices.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.PrimaryBlue
                                )
                            }

                            Divider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Отображение списка или заглушки в зависимости от состояния Bluetooth
                            if (isBluetoothEnabled) {
                                if (pairedDevices.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Нет сопряженных устройств\n\nДобавьте устройство в настройках системы",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AppColors.TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                    ) {
                                        items(pairedDevices) { device ->
                                            DeviceListItem(
                                                device = device,
                                                isSelected = selectedDevice.address == device.address,
                                                onClick = {
                                                    // Сохраняем выбор и возвращаемся на предыдущий экран
                                                    sharedViewModel.selectBluetoothDevice(device)
                                                    navController.popBackStack()
                                                }
                                            )
                                            // Разделитель между элементами списка
                                            if (device != pairedDevices.last()) {
                                                Divider(
                                                    color = AppColors.SurfaceMedium,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Заглушка при выключенном Bluetooth
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Bluetooth выключен\n\nВключите его в настройках системы",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Вспомогательный компонент для отображения отдельного устройства в списке.
 * 
 * @param device Данные устройства для отображения
 * @param isSelected Флаг, указывающий является ли устройство текущим выбранным
 * @param onClick Обработчик нажатия на элемент
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListItem(
    device: BluetoothDeviceData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка типа устройства в круглой подложке
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AppColors.PrimaryBlue.copy(alpha = 0.2f) else AppColors.SurfaceMedium
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Выбор иконки на основе типа устройства
                val icon = when (device.deviceType) {
                    DeviceType.CAR_AUDIO -> Icons.Default.DirectionsCar
                    DeviceType.HEADSET -> Icons.Default.Headset
                    DeviceType.HEADPHONES -> Icons.Default.Headphones
                    DeviceType.AUDIO_VIDEO -> Icons.Default.Speaker
                    DeviceType.COMPUTER -> Icons.Default.Computer
                    DeviceType.PHONE -> Icons.Default.PhoneAndroid
                    DeviceType.PERIPHERAL -> Icons.Default.Keyboard
                    else -> Icons.Default.Bluetooth
                }
                Icon(
                    icon, 
                    contentDescription = null,
                    tint = if (isSelected) AppColors.PrimaryBlue else AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Название и MAC-адрес устройства
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) AppColors.PrimaryBlue else AppColors.TextPrimary
                )
                // Отображение типа устройства
                Text(
                    device.deviceType.getDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) AppColors.PrimaryBlue.copy(alpha = 0.7f) else AppColors.TextSecondary
                )
                // MAC-адрес
                Text(
                    device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) AppColors.PrimaryBlue.copy(alpha = 0.5f) else AppColors.TextSecondary.copy(alpha = 0.7f)
                )
            }

            // Индикатор выбора
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = AppColors.PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
