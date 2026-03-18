package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
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

// ========== PREVIEWS ==========

/**
 * Настройка превью для сравнения устройств с разной пропорцией пикселя.
 * Высота в DP зафиксирована (800dp), чтобы устройства отображались в одном размере по вертикали.
 * Ширина рассчитана исходя из физических размеров:
 * 1080P: 116/65 * 800 = 1428dp
 * 720P: 196/113 * 800 = 1388dp
 * DPI подобран так, чтобы получить реальное разрешение по вертикали (1080 и 720 пикселей).
 */

@Preview(
    name = "1. Device 1080P (1795x1080, 116x65mm)",
    device = "spec:width=1428dp,height=800dp,dpi=216",
    backgroundColor = 0xFF121212,
    showBackground = true
)
@Preview(
    name = "2. Device 720P (1280x720, 196x113mm)",
    device = "spec:width=1388dp,height=800dp,dpi=144",
    backgroundColor = 0xFF121212,
    showBackground = true
)
@Composable
fun DashboardType1Preview() {
    val fakeCarData = CarData(
        speed = 96f,
        fuel = 49f,
        voltage = 12.6f,
        remainingRange = 520f
    )
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        DashboardType1(
            carData = fakeCarData,
            appSettings = null
        )
    }
}
