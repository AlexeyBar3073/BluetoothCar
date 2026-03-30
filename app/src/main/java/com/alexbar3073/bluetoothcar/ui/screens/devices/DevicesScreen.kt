// Файл: ui/screens/devices/DevicesScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.DeviceType
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ФАЙЛ: ui/screens/devices/DevicesScreen.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/devices/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Экран выбора Bluetooth устройств. Позволяет пользователю выбрать
 * Bluetooth устройство для подключения.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    // Состояния из SharedViewModel
    val isInitialized by sharedViewModel.isInitialized.collectAsStateWithLifecycle()
    val selectedDevice by sharedViewModel.selectedDevice.collectAsStateWithLifecycle()
    
    // Подписка на статус подключения обеспечивает реактивность при системных изменениях (например, выключении Bluetooth)
    val connectionStatus by sharedViewModel.connectionStatusInfo.collectAsStateWithLifecycle()

    // Динамически получаем состояние Bluetooth и список устройств.
    // Зависимость от connectionStatus гарантирует рекомпозицию и обновление данных при изменении состояния связи.
    val isBluetoothEnabled = connectionStatus.run {
        if (isInitialized) sharedViewModel.isBluetoothEnabled() else false
    }
    val pairedDevices = connectionStatus.run {
        if (isInitialized) sharedViewModel.getPairedDevices() else emptyList()
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
                            Icon(
                                imageVector = if (isBluetoothEnabled) {
                                    Icons.Default.Bluetooth
                                } else {
                                    Icons.Default.BluetoothDisabled
                                },
                                contentDescription = "Bluetooth",
                                tint = if (isBluetoothEnabled) {
                                    AppColors.PrimaryBlue
                                } else {
                                    AppColors.Error
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "УСТРОЙСТВА BLUETOOTH",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад"
                            )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 20.dp)
                ) {
                    // Карточка текущего выбранного устройства
                    if (selectedDevice.isValidDevice()) {
                        val device = selectedDevice
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.SurfaceLight
                            ),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothConnected,
                                        contentDescription = "Выбрано",
                                        tint = AppColors.PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "ТЕКУЩЕЕ УСТРОЙСТВО",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.PrimaryBlue
                                    )
                                }

                                Divider(
                                    color = AppColors.SurfaceMedium,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = "Устройство",
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = AppColors.TextPrimary
                                        )

                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Список сопряженных устройств
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.SurfaceLight
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Устройства",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "СОПРЯЖЕННЫЕ УСТРОЙСТВА",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.TextSecondary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "(доступно: ${pairedDevices?.size ?: 0})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (pairedDevices.isNullOrEmpty()) {
                                        AppColors.TextSecondary
                                    } else {
                                        AppColors.PrimaryBlue
                                    }
                                )
                            }

                            Divider(
                                color = AppColors.SurfaceMedium,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            if (isBluetoothEnabled) {
                                if (pairedDevices.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Нет сопряженных устройств\n\nСопрягите устройство в настройках Bluetooth системы",
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
                                        items(pairedDevices!!) { device ->
                                            DeviceListItem(
                                                device = device,
                                                isSelected = selectedDevice.address == device.address,
                                                onClick = {
                                                    sharedViewModel.selectBluetoothDevice(device)
                                                    navController.popBackStack()
                                                }
                                            )

                                            if (device != pairedDevices.last()) {
                                                Divider(
                                                    color = AppColors.SurfaceMedium,
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Bluetooth выключен\n\nВключите Bluetooth в настройках системы для отображения устройств",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            AppColors.PrimaryBlue.copy(alpha = 0.2f)
                        } else {
                            AppColors.SurfaceMedium
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val deviceIcon = when (device.deviceType) {
                    DeviceType.CAR_AUDIO -> Icons.Default.DirectionsCar
                    DeviceType.HEADSET -> Icons.Default.Headset
                    DeviceType.HEADPHONES -> Icons.Default.Headphones
                    DeviceType.AUDIO_VIDEO -> Icons.Default.Speaker
                    DeviceType.COMPUTER -> Icons.Default.Computer
                    DeviceType.PHONE -> Icons.Default.PhoneAndroid
                    DeviceType.PERIPHERAL -> Icons.Default.Keyboard
                    DeviceType.UNKNOWN -> Icons.Default.Bluetooth
                }

                Icon(
                    imageVector = deviceIcon,
                    contentDescription = "Тип устройства",
                    tint = if (isSelected) {
                        AppColors.PrimaryBlue
                    } else {
                        AppColors.TextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        AppColors.PrimaryBlue
                    } else {
                        AppColors.TextPrimary
                    }
                )

                Text(
                    text = device.deviceType.getDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        AppColors.PrimaryBlue.copy(alpha = 0.7f)
                    } else {
                        AppColors.TextSecondary
                    }
                )

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        AppColors.PrimaryBlue.copy(alpha = 0.5f)
                    } else {
                        AppColors.TextSecondary.copy(alpha = 0.7f)
                    }
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = AppColors.PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
