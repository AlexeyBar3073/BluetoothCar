package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

object Dashboard2Coords {
    const val BASE_WIDTH = 1280f
    const val BASE_HEIGHT = 713f
    
    val SpeedometerCenter = Offset(640f, 356f)
    val EngineTempCenter = Offset(204f, 188f)
    val TransmissionTempCenter = Offset(172f, 362f)
    val FuelCenter = Offset(172f, 533f)
    val RangeCenter = Offset(1032f, 328f)
}

@Deprecated("Используйте DashboardType4 для актуальной визуализации")
@Composable
fun DashboardType2(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val targetAspectRatio = Dashboard2Coords.BASE_WIDTH / Dashboard2Coords.BASE_HEIGHT

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(targetAspectRatio, matchHeightConstraintsFirst = true)
        ) {
            // 1. Фон
            Image(
                painter = painterResource(id = R.drawable.background_2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // 2. Спидометр
            SpeedometerNeedle(
                speed = carData.speed,
                baseCenter = Dashboard2Coords.SpeedometerCenter
            )

            // 3. Температура двигателя (Верхний левый)
            SmallNeedle(
                value = 90f,
                maxValue = 130f,
                startAngle = 180f,
                sweepAngle = 135f,
                baseCenter = Dashboard2Coords.EngineTempCenter
            )

            // 4. Температура АКПП (Средний левый)
            SmallNeedle(
                value = 85f,
                maxValue = 130f,
                startAngle = 180f,
                sweepAngle = 135f,
                baseCenter = Dashboard2Coords.TransmissionTempCenter
            )

            // 5. Топливо (Нижний левый)
            SmallNeedle(
                value = carData.fuel,
                maxValue = appSettings?.fuelTankCapacity ?: 60f,
                startAngle = 180f,
                sweepAngle = 135f,
                baseCenter = Dashboard2Coords.FuelCenter
            )
            
            // Правый прибор (Остаток хода) пока пропущен, ждет своей геометрии
        }
    }
}
