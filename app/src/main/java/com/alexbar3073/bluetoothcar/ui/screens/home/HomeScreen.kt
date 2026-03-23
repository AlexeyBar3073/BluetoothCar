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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.StatusCircleButton
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1.DashboardType1
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2.DashboardType2
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_3.DashboardType3
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4.DashboardType4
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.COMPACT_TOP_BAR_HEIGHT
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * Главный экран приложения "Бортовой компьютер".
 */
@Composable
fun HomeScreen(
    viewModel: SharedViewModel,
    navigateToSettings: () -> Unit
) {
    // Получаем состояние из ViewModel
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val carData by viewModel.carData.collectAsStateWithLifecycle()
    val connectionStatusInfo by viewModel.connectionStatusInfo.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    HomeScreenContent(
        selectedDevice = selectedDevice,
        carData = carData,
        connectionStatusInfo = connectionStatusInfo,
        appSettings = appSettings,
        onRetryConnection = { viewModel.retryConnection() },
        navigateToSettings = navigateToSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    selectedDevice: BluetoothDeviceData?,
    carData: CarData,
    connectionStatusInfo: ConnectionStatusInfo,
    appSettings: AppSettings?,
    onRetryConnection: () -> Unit,
    navigateToSettings: () -> Unit
) {
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
                        StatusCircleButton(
                            connectionStatusInfo = connectionStatusInfo,
                            onClick = {
                                if (connectionStatusInfo.allowsManualRetry) {
                                    onRetryConnection()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    DashboardType4(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings
                    )
                }
            }
        }
    }
}

// ========== PREVIEWS ==========

@Preview(
    name = "Home - 1080P (116x65mm)",
    device = "spec:width=642dp,height=360dp,dpi=480",
    showSystemUi = false,
    backgroundColor = 0xFF121212,
    showBackground = true
)
@Preview(
    name = "Home - 720P (196x113mm)",
    device = "spec:width=624dp,height=360dp,dpi=320",
    showSystemUi = false,
    backgroundColor = 0xFF121212,
    showBackground = true
)
@Composable
fun HomeScreenPreview() {
    val fakeCarData = CarData(
        speed = 96f,
        fuel = 42f, // Около 70% от 60л
        voltage = 12.6f,
        remainingRange = 305f,
        odometer = 326452f,
        tripA = 8675.2f,
        fuelConsumption = 12.4f,
        coolantTemp = 87f,
        transmissionTemp = 73f
    )
    
    HomeScreenContent(
        selectedDevice = BluetoothDeviceData("Toyota OBD", "00:11:22:33:44:55"),
        carData = fakeCarData,
        connectionStatusInfo = ConnectionState.LISTENING_DATA.toStatusInfo(),
        appSettings = AppSettings(fuelTankCapacity = 60f),
        onRetryConnection = {},
        navigateToSettings = {}
    )
}
