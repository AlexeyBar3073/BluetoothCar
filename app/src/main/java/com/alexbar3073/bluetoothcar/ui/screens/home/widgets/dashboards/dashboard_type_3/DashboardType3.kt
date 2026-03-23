package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_3

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

/**
 * Дашборд Type 3.
 * Основной акцент на спидометре в левой части экрана.
 * Правая часть разделена на сетку 2x2 для дополнительных виджетов.
 */
@Composable
fun DashboardType3(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Спидометр занимает левую половину экрана
        val leftPartWidthPx = widthPx * 0.5f
        
        // Геометрия для спидометра (левая часть)
        val leftGeometry = remember(leftPartWidthPx, heightPx) {
            DashboardType3Geometry.fromSize(
                size = Size(leftPartWidthPx, heightPx),
                density = density
            )
        }

        // Каждая ячейка в правой части (2x2)
        val gridItemWidthPx = widthPx * 0.25f
        val gridItemHeightPx = heightPx * 0.5f

        // Геометрия для виджетов в сетке (правая часть)
        val gridGeometry = remember(gridItemWidthPx, gridItemHeightPx) {
            DashboardType3Geometry.fromSize(
                size = Size(gridItemWidthPx, gridItemHeightPx),
                density = density
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Левая часть: Спидометр (50% ширины)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
            ) {
                DashboardType3Speedometer(
                    modifier = Modifier.fillMaxSize(),
                    carData = carData,
                    geometry = leftGeometry
                )
            }

            // Правая часть: Сетка 2x2 (50% ширины)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
            ) {
                // Верхняя строка
                Row(modifier = Modifier.weight(0.5f)) {
                    // Слева сверху: Температура АКПП
                    Box(modifier = Modifier.weight(0.5f)) {
                        DashboardType3TransmissionTemp(
                            modifier = Modifier.fillMaxSize(),
                            carData = carData,
                            geometry = gridGeometry
                        )
                    }
                    // Справа сверху: Температура двигателя
                    Box(modifier = Modifier.weight(0.5f)) {
                        DashboardType3EngineTemp(
                            modifier = Modifier.fillMaxSize(),
                            carData = carData,
                            geometry = gridGeometry
                        )
                    }
                }
                
                // Нижняя строка
                Row(modifier = Modifier.weight(0.5f)) {
                    // Слева снизу: Уровень топлива
                    Box(modifier = Modifier.weight(0.5f)) {
                        DashboardType3Fuel(
                            modifier = Modifier.fillMaxSize(),
                            carData = carData,
                            appSettings = appSettings,
                            geometry = gridGeometry
                        )
                    }
                    // Справа снизу: Пусто
                    Box(modifier = Modifier.weight(0.5f))
                }
            }
        }
    }
}
