package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData

/**
 * Дашборд Type 1 - компоновка виджетов по концепции двух окружностей.
 *
 * КОНЦЕПЦИЯ:
 *   - Малая окружность (minRadius) → SpeedometerWidget (по центру)
 *   - Большая окружность (maxRadius) → боковые секторы
 *        Левый сектор (90°-270°) → FuelWidget
 *        Правый сектор (-90°-90°) → TempWidget (в разработке)
 *
 * @param modifier Модификатор для настройки размера и позиции
 * @param carData Данные автомобиля
 * @param appSettings Настройки приложения (может быть null)
 */
@Composable
fun DashboardType1(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

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
            // Рисуется поверх спидометра, но будет обрезан по сектору
            FuelWidget(
                modifier = Modifier.fillMaxSize(),
                carData = carData,
                appSettings = appSettings,
                geometry = geometry
            )

            // TODO: Добавить TempWidget (правый сектор)
        }
    }
}