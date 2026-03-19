package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

/**
 * Дашборд Type 1 - компоновка виджетов по концепции двух окружностей.
 * Двухстрочная компоновка:
 * 1. Приборы (Спидометр и Топливо) - 85% высоты
 * 2. Данные о поездке (Trip) - 15% высоты
 */
@Composable
fun DashboardType1(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Распределяем высоту: верхняя часть под приборы, нижняя под Trip
        val topPartHeightPx = heightPx * 0.85f

        // Создаем геометрию специально для верхней части. 
        // Это заставит приборы отцентроваться в верхних 85% экрана.
        val geometry = remember(widthPx, topPartHeightPx) {
            Geometry.fromSize(
                size = Size(widthPx, topPartHeightPx),
                density = density
            )
        }

        LaunchedEffect(widthPx, heightPx) {
            AppLogger.logInfo(
                "DASHBOARD_LAYOUT: Total=${widthPx.toInt()}x${heightPx.toInt()}, TopHeight=${topPartHeightPx.toInt()}",
                "DashboardType1"
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ВЕРХНЯЯ СТРОКА: Основные приборы (85% высоты)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f)
            ) {
                // Спидометр
                SpeedometerWidget(
                    modifier = Modifier.fillMaxSize(),
                    carData = carData,
                    geometry = geometry
                )

                // Виджет топлива
                FuelWidget(
                    modifier = Modifier.fillMaxSize(),
                    carData = carData,
                    appSettings = appSettings,
                    geometry = geometry
                )
            }

            // НИЖНЯЯ СТРОКА: Данные Trip (15% высоты)
            TripWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f),
                carData = carData,
                appSettings = appSettings,
                geometry = geometry
            )
        }
    }
}
