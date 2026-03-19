package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

@Composable
fun DashboardType2(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1B2631), Color(0xFF0F161E))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ЛЕВАЯ ЧАСТЬ: Спидометр
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerType2(
                    modifier = Modifier.size(320.dp),
                    speed = carData.speed
                )
            }

            // ЦЕНТРАЛЬНАЯ ЧАСТЬ
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                InfoTopPanel(
                    voltage = carData.voltage,
                    odometer = carData.odometer
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                ConsumptionWidget(consumption = carData.fuelConsumption)
                
                Spacer(modifier = Modifier.weight(1f))
                
                RangeSliderWidget(
                    remainingRange = carData.remainingRange,
                    maxRange = 850f
                )
            }

            // ПРАВАЯ ЧАСТЬ: Малые приборы
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF141C24).copy(alpha = 0.4f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallGaugeWidget(
                    label = "Топливо", 
                    value = carData.fuel, 
                    maxValue = appSettings?.fuelTankCapacity ?: 60f,
                    unit = "л"
                )
                // Заглушки для температур
                SmallGaugeWidget(label = "ДВС", value = 92f, maxValue = 130f, unit = "°C")
                SmallGaugeWidget(label = "АКПП", value = 85f, maxValue = 130f, unit = "°C")
            }
        }
    }
}
