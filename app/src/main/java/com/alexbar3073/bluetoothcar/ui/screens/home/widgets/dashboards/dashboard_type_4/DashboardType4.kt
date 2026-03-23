package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

/**
 * Дашборд Type 4.
 * Содержит два круговых прибора одинакового размера:
 * - Слева: Спидометр.
 * - Справа: Универсальный комбинированный прибор.
 * В нижней части отображается TripWidget.
 */
@Composable
fun DashboardType4(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Распределяем высоту: верхняя часть под приборы (85%), нижняя под Trip (15%)
        val topPartHeightPx = heightPx * 0.85f
        val partWidthPx = widthPx * 0.5f
        
        // Геометрия для приборов (половина ширины)
        val gaugeGeometry = remember(partWidthPx, topPartHeightPx) {
            DashboardType4Geometry.fromSize(Size(partWidthPx, topPartHeightPx), density)
        }

        // Геометрия для TripWidget (полная ширина, чтобы дуги доходили до краев экрана)
        val tripGeometry = remember(widthPx, topPartHeightPx) {
            DashboardType4Geometry.fromSize(Size(widthPx, topPartHeightPx), density)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ВЕРХНЯЯ СТРОКА: Основные приборы (85% высоты)
            Row(modifier = Modifier.fillMaxWidth().weight(0.85f)) {
                // Левая часть со спидометром
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.5f)
                ) {
                    DashboardType4Speedometer(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        geometry = gaugeGeometry
                    )
                }

                // Правая часть с комбинированным прибором
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.5f)
                ) {
                    DashboardType4CombinedGauge(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings,
                        geometry = gaugeGeometry
                    )
                }
            }

            // НИЖНЯЯ СТРОКА: Данные Trip (15% высоты)
            TripWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f),
                carData = carData,
                appSettings = appSettings,
                geometry = tripGeometry
            )
        }
    }
}
