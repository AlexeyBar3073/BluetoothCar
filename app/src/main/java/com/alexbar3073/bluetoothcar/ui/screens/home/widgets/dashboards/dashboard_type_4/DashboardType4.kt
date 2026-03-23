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
 * Содержит два круговых прибора, каждый из которых состоит из двух сегментов (как в эксперименте Type 3).
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

        val leftPartWidthPx = widthPx * 0.5f
        val leftGeometry = remember(leftPartWidthPx, heightPx) {
            DashboardType4Geometry.fromSize(Size(leftPartWidthPx, heightPx), density)
        }

        val instrumentSizePx = heightPx * 0.65f
        val instrumentSizeDp = with(density) { instrumentSizePx.toDp() }

        val gridGeometry = remember(instrumentSizePx) {
            DashboardType4Geometry.fromSize(Size(instrumentSizePx, instrumentSizePx), density)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
            ) {
                DashboardType4Speedometer(
                    modifier = Modifier.fillMaxSize(),
                    carData = carData,
                    geometry = leftGeometry
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(instrumentSizeDp),
                    contentAlignment = Alignment.Center
                ) {
                    DashboardType4TransmissionTemp(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        geometry = gridGeometry
                    )
                    DashboardType4Fuel(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        appSettings = appSettings,
                        geometry = gridGeometry
                    )
                }

                Box(
                    modifier = Modifier.size(instrumentSizeDp),
                    contentAlignment = Alignment.Center
                ) {
                    DashboardType4EngineTemp(
                        modifier = Modifier.fillMaxSize(),
                        carData = carData,
                        geometry = gridGeometry
                    )
                }
            }
        }
    }
}
