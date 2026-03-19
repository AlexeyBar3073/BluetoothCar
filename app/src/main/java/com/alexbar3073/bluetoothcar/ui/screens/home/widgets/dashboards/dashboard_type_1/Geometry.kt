package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import kotlin.math.min
import kotlin.math.sqrt

internal data class Geometry(
    // Базовые параметры
    val unit: Float,
    val center: Offset,
    val width: Float,
    val height: Float,
    val margin: Float,
    val gapScale: Float,
    val maxRadius: Float,
    val outerStrokeWidth: Float,
    val outerRingRadius: Float,
    val scaleRadius: Float,
    val tickSmall: Float,
    val tickMedium: Float,
    val tickLarge: Float,
    val textOffsetFromTick: Float,
    val textSizePx: Float,
    val textRadius: Float,
    val glowGap: Float,
    val glowWidth: Float,
    val ringRadius: Float,
    val blackStrokeWidth: Float,
    val blackRadius: Float,
    val startAngle: Float,
    val fullSweep: Float,
    val maxSpeed: Float,
    val density: Density,
    val ringColor: Color,
    val logTag: String,
    val trailAlphaStops: List<Float>,

    // Радиусы для большой окружности
    val bigMaxRadius: Float,
    val bigOuterRingRadius: Float,
    val bigScaleRadius: Float,
    val bigTextRadius: Float,

    // Дополнительные параметры для TripWidget
    val tripArcRadius: Float
) {
    companion object {
        fun fromSize(
            size: Size,
            density: Density,
            ringColor: Color = Color(0xFFFC4903),
            logTag: String = "DashboardType1"
        ): Geometry {
            // 1. ОПРЕДЕЛЯЕМ РАЗМЕРЫ
            val canvasHeight = size.minDimension // В ландшафте это высота (864 на вашем устройстве)

            // 2. ВЫЧИСЛЯЕМ ВИРТУАЛЬНЫЙ UNIT
            val unit = canvasHeight / 288f

            val margin = 3f * unit
            val center = Offset(size.width / 2f, size.height / 2f)

            // 3. ПРИМЕНЯЕМ КОЭФФИЦИЕНТЫ
            val outerStrokeWidth = 2f * unit
            val gapScale = 8f * unit
            val tickSmall = 6f * unit
            val tickMedium = 10f * unit
            val tickLarge = 14f * unit
            val textOffsetFromTick = 12f * unit

            val textSizePx = 18f * unit 
            
            val glowGap = 10f * unit
            val glowWidth = 21.66f * unit
            val blackStrokeWidth = 8f * unit

            // Радиусы для малой окружности (спидометр)
            val maxRadius = canvasHeight / 2f - margin
            val outerRingRadius = maxRadius - outerStrokeWidth / 2f
            val scaleRadius = outerRingRadius - outerStrokeWidth / 2f - gapScale
            val textRadius = scaleRadius - tickLarge - textOffsetFromTick

            // Радиусы для большой окружности
            val bigMaxRadius = size.maxDimension / 2f - margin
            val bigOuterRingRadius = bigMaxRadius - outerStrokeWidth / 2f
            val bigScaleRadius = bigOuterRingRadius - outerStrokeWidth / 2f - gapScale
            val bigTextRadius = bigScaleRadius - tickLarge - textOffsetFromTick

            // Остальные параметры
            val outerGlowRadius = textRadius - glowGap
            val ringRadius = outerGlowRadius - glowWidth
            val innerBlackRadius = ringRadius + 1f * unit
            val blackRadius = innerBlackRadius + blackStrokeWidth / 2f

            val startAngle = 150f
            val fullSweep = 240f
            val maxSpeed = 220f

            val trailAlphaStops = listOf(
                0.062f, 0.123f, 0.185f, 0.246f, 0.308f, 0.370f,
                0.431f, 0.493f, 0.554f, 0.616f, 0.678f, 0.739f
            )

            // РАСЧЕТ РАДИУСА ДЛЯ TRIP WIDGET
            // Расстояние от центра экрана до верхней угловой точки TripWidget (с учетом margin)
            // Центр находится в (size.width/2, size.height/2).
            // Точка находится в (margin, size.height) относительно глобального контейнера.
            // В координатах относительно центра это (margin - size.width/2, size.height/2).
            val dx = margin - size.width / 2f
            val dy = size.height / 2f
            val tripArcRadius = sqrt(dx * dx + dy * dy)

            return Geometry(
                unit = unit,
                center = center,
                width = size.width,
                height = size.height,
                margin = margin,
                gapScale = gapScale,
                maxRadius = maxRadius,
                outerStrokeWidth = outerStrokeWidth,
                outerRingRadius = outerRingRadius,
                scaleRadius = scaleRadius,
                tickSmall = tickSmall,
                tickMedium = tickMedium,
                tickLarge = tickLarge,
                textOffsetFromTick = textOffsetFromTick,
                textSizePx = textSizePx,
                textRadius = textRadius,
                glowGap = glowGap,
                glowWidth = glowWidth,
                ringRadius = ringRadius,
                blackStrokeWidth = blackStrokeWidth,
                blackRadius = blackRadius,
                startAngle = startAngle,
                fullSweep = fullSweep,
                maxSpeed = maxSpeed,
                density = density,
                ringColor = ringColor,
                logTag = logTag,
                trailAlphaStops = trailAlphaStops,
                bigMaxRadius = bigMaxRadius,
                bigOuterRingRadius = bigOuterRingRadius,
                bigScaleRadius = bigScaleRadius,
                bigTextRadius = bigTextRadius,
                tripArcRadius = tripArcRadius
            )
        }
    }
}
