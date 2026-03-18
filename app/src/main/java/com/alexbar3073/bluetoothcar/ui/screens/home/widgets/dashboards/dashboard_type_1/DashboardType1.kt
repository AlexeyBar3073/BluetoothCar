package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

/**
 * Дашборд Type 1 - компоновка виджетов по концепции двух окружностей.
 */
@Composable
fun DashboardType1(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Собираем информацию о железе
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val densityDpi = displayMetrics.densityDpi
        val densityValue = displayMetrics.density

        // Выводим информацию в лог
        remember(screenWidth, screenHeight) {
            AppLogger.logInfo(
                "DEVICE_INFO: Screen=${screenWidth}x${screenHeight}, Canvas=${widthPx.toInt()}x${heightPx.toInt()}, DPI=$densityDpi, Density=$densityValue",
                "DashboardType1"
            )
            true
        }

        // Создаем геометрию один раз при изменении размера
        val geometry = remember(widthPx, heightPx) {
            Geometry.fromSize(
                size = Size(widthPx, heightPx),
                density = density
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Спидометр (центрирован)
            SpeedometerWidget(
                modifier = Modifier.fillMaxSize(),
                carData = carData,
                geometry = geometry
            )

            // Виджет топлива (левый сектор)
            FuelWidget(
                modifier = Modifier.fillMaxSize(),
                carData = carData,
                appSettings = appSettings,
                geometry = geometry
            )
        }
    }
}
