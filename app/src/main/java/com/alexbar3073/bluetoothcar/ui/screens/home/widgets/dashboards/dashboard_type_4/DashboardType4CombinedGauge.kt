package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.cos
import kotlin.math.sin

/**
 * Универсальный прибор для Dashboard Type 4.
 * Состоит из 3 сегментов: Топливо, АКПП, Двигатель.
 * Расположены от 140 до 40 градусов (развертка 260 градусов).
 */
@Composable
internal fun DashboardType4CombinedGauge(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: DashboardType4Geometry
) {
    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f
    
    fun getTempRatio(temp: Float): Float {
        val t = temp.coerceIn(50f, 130f)
        return (t - 50f) / 80f
    }

    val fuelRatio = (carData.fuel / fuelTankCapacity).coerceIn(0f, 1f)
    val engineTempRatio = getTempRatio(carData.coolantTemp)
    val transTempRatio = getTempRatio(carData.transmissionTemp)

    // Анимации
    val animatedFuel by animateFloatAsState(fuelRatio, spring(0.8f, 80f), label = "fuel")
    val animatedEngine by animateFloatAsState(engineTempRatio, spring(0.8f, 80f), label = "engine")
    val animatedTrans by animateFloatAsState(transTempRatio, spring(0.8f, 80f), label = "trans")

    // Иконки
    val fuelIcon = painterResource(R.drawable.fuel_50)
    val engineIcon = painterResource(R.drawable.engine_coolant_new)
    val transIcon = painterResource(R.drawable.oil_temperature_new)

    val bitmapKey = remember(geometry.width, geometry.height) { Pair(geometry.width, geometry.height) }
    var staticBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Внешнее кольцо (белое)
            drawCircle(
                color = Color.White,
                radius = geometry.outerRingRadius,
                center = geometry.center,
                style = Stroke(width = geometry.outerStrokeWidth)
            )

            // 2. Статическая шкала
            if (staticBitmap == null) {
                staticBitmap = buildStaticBitmap(geometry)
            }
            staticBitmap?.let { drawImage(it) }

            // 3. Двойная подсветка (GLOW)
            drawDualRadialGlow(geometry, geometry.ringColor)

            // 4. Черная окружность - основание для стрелок
            drawBlackRing(geometry)

            // 5. Иконки
            drawGaugeIcons(geometry, fuelIcon, engineIcon, transIcon)

            // 6. Абсолютные значения (смещены ближе к центру от Glow)
            drawGaugeValues(geometry, carData)

            // 7. Стрелки (начинаются от внутреннего края Glow)
            // Топливо: 140 -> 220
            drawNeedleWithBloom(geometry, 140f + 80f * animatedFuel, geometry.ringColor)
            // АКПП: 230 -> 310
            drawNeedleWithBloom(geometry, 230f + 80f * animatedTrans, geometry.ringColor)
            // Двигатель: 320 -> 40
            drawNeedleWithBloom(geometry, 320f + 80f * animatedEngine, geometry.ringColor)
        }
    }
}

private fun DrawScope.drawDualRadialGlow(
    geometry: DashboardType4Geometry,
    color: Color,
    peakSpread: Float = 0.01f
) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth
    val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return

    val innerStop = innerRadius / outerRadius
    val peakStop = geometry.ringRadius / outerRadius

    val leftPeak = (peakStop - peakSpread).coerceIn(0f, 1f)
    val rightPeak = (peakStop + peakSpread).coerceIn(0f, 1f)

    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            innerStop to Color.Transparent,
            leftPeak to color.copy(alpha = 0.6f),
            peakStop to color,
            rightPeak to color.copy(alpha = 0.6f),
            1f to Color.Transparent
        ),
        center = geometry.center,
        radius = outerRadius
    )

    drawCircle(
        brush = brush,
        center = geometry.center,
        radius = (outerRadius + innerRadius) / 2f,
        style = Stroke(width = outerRadius - innerRadius)
    )
}

private fun DrawScope.drawBlackRing(geometry: DashboardType4Geometry) {
    drawCircle(
        color = Color.Black,
        radius = geometry.blackRadius,
        center = geometry.center,
        style = Stroke(width = geometry.blackStrokeWidth)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.1f),
        radius = geometry.blackRadius + geometry.blackStrokeWidth / 2f,
        center = geometry.center,
        style = Stroke(width = 1f * geometry.unit)
    )
}

private fun DrawScope.drawNeedleWithBloom(
    geometry: DashboardType4Geometry,
    angle: Float,
    color: Color
) {
    val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
    
    rotate(angle, pivot = geometry.center) {
        // 1. Световая тень (Bloom) на черном кольце
        drawHeatBloomSegment(
            radius = geometry.blackRadius,
            color = color,
            geometry = geometry,
            sweepAngle = 45f,
            startAngle = 0f
        )

        // 2. Тело стрелки
        val tip = Offset(geometry.center.x + geometry.scaleRadius - 2f * geometry.unit, geometry.center.y)
        // Стрелка начинается от начала внутреннего Glow
        val start = Offset(geometry.center.x + innerGlowRadius, geometry.center.y)
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f)),
                start = start,
                end = tip
            ),
            start = start,
            end = tip,
            strokeWidth = 5f * geometry.unit,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White),
                start = start,
                end = tip
            ),
            start = start,
            end = tip,
            strokeWidth = 1.5f * geometry.unit,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawHeatBloomSegment(
    radius: Float,
    color: Color,
    geometry: DashboardType4Geometry,
    sweepAngle: Float = 45f,
    gradientWidth: Float = 7f,
    startAngle: Float = 0f
) {
    val scaledGradientWidth = gradientWidth * geometry.unit
    val outerRadius = radius + scaledGradientWidth
    val canvas = drawContext.canvas
    
    canvas.saveLayer(Rect(Offset.Zero, size), Paint())

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                (radius - scaledGradientWidth) / outerRadius to Color.Transparent,
                radius / outerRadius to color,
                1f to Color.Transparent
            ),
            center = geometry.center,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = geometry.center
    )

    drawCircle(
        color = Color.White,
        radius = radius,
        center = geometry.center,
        style = Stroke(1f * geometry.unit)
    )

    drawIntoCanvas {
        it.nativeCanvas.save()
        it.nativeCanvas.rotate(startAngle - 180f, geometry.center.x, geometry.center.y)
        val half = (sweepAngle / 360f) / 2f
        drawCircle(
            brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f - half to Color.Transparent,
                    0.5f to Color.Black,
                    0.5f + half to Color.Transparent,
                    1f to Color.Transparent
                ),
                center = geometry.center
            ),
            radius = outerRadius,
            center = geometry.center,
            blendMode = BlendMode.DstIn
        )
        it.nativeCanvas.restore()
    }
    canvas.restore()
}

private fun buildStaticBitmap(geometry: DashboardType4Geometry): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawSegmentScale(geometry, 140f, 80f, labels = listOf("0", "1/2", "1"), skipMiddleLabel = true)
        drawSegmentScale(geometry, 230f, 80f, labels = listOf("50", "90", "130"), skipMiddleLabel = true)
        drawSegmentScale(geometry, 320f, 80f, labels = listOf("50", "90", "130"), skipMiddleLabel = true)
    }
    return bitmap
}

private fun DrawScope.drawSegmentScale(
    geometry: DashboardType4Geometry,
    startAngle: Float,
    sweepAngle: Float,
    labels: List<String> = emptyList(),
    skipMiddleLabel: Boolean = false
) {
    val ringColor = geometry.ringColor
    val steps = 40 
    
    val textPaint = android.graphics.Paint().apply {
        color = ringColor.toArgb()
        textSize = geometry.textSizePx
        isAntiAlias = true
    }

    for (i in 0..steps) {
        val angle = startAngle + sweepAngle * (i / steps.toFloat())
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cos = cos(rad)
        val sin = sin(rad)
        
        val isMajor = i % 10 == 0
        val isMedium = i % 5 == 0 && !isMajor
        
        val tickLen = if (isMajor) geometry.tickLarge else if (isMedium) geometry.tickMedium else geometry.tickSmall
        val strokeWidth = if (isMajor) 2f * geometry.unit else if (isMedium) 1.5f * geometry.unit else 1f * geometry.unit
        
        drawLine(
            color = ringColor,
            start = Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius),
            end = Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (labels.isNotEmpty() && i % 20 == 0) {
            val labelIndex = i / 20
            if (labelIndex < labels.size && !(skipMiddleLabel && labelIndex == 1)) {
                val text = labels[labelIndex]
                
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    
                    textPaint.textAlign = when (i) {
                        0 -> android.graphics.Paint.Align.LEFT
                        40 -> android.graphics.Paint.Align.RIGHT
                        else -> android.graphics.Paint.Align.CENTER
                    }
                    
                    val originalSize = geometry.textSizePx
                    if (text.contains("/")) {
                        textPaint.textSize = originalSize * 0.7f
                    } else {
                        textPaint.textSize = originalSize
                    }

                    val tRadius = geometry.scaleRadius - tickLen - 12f * geometry.unit
                    translate(geometry.center.x + cos * tRadius, geometry.center.y + sin * tRadius)
                    rotate(angle + 90f)
                    
                    drawText(text, 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint)

                    restore()
                }
            }
        }
    }
}

private fun DrawScope.drawGaugeIcons(
    geometry: DashboardType4Geometry,
    fuelIcon: Painter,
    engineIcon: Painter,
    transIcon: Painter
) {
    val baseIconSize = 20f * geometry.unit
    val dist = geometry.scaleRadius - geometry.tickLarge - 12f * geometry.unit
    val ringColor = geometry.ringColor

    drawIconCenteredAt(geometry, 180f, dist, baseIconSize, fuelIcon, ringColor)
    drawIconCenteredAt(geometry, 270f, dist, baseIconSize, transIcon, ringColor)
    drawIconCenteredAt(geometry, 0f, dist, baseIconSize, engineIcon, ringColor)
}

private fun DrawScope.drawIconCenteredAt(
    geometry: DashboardType4Geometry,
    angle: Float,
    distance: Float,
    size: Float,
    painter: Painter,
    color: Color
) {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val centerX = geometry.center.x + cos(rad) * distance
    val centerY = geometry.center.y + sin(rad) * distance
    
    translate(centerX - size / 2f, centerY - size / 2f) {
        with(painter) {
            draw(Size(size, size), colorFilter = ColorFilter.tint(color))
        }
    }
}

private fun DrawScope.drawGaugeValues(
    geometry: DashboardType4Geometry,
    carData: CarData
) {
    val valueTextSize = 14f * geometry.unit
    // Сдвигаем внутрь от начала Glow
    val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
    val dist = innerGlowRadius - 8f * geometry.unit 
    
    val textPaint = android.graphics.Paint().apply {
        color = Color.White.toArgb()
        textSize = valueTextSize
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    // Топливо: 180 градусов
    drawValueCenteredAt(geometry, 180f, dist, carData.fuel.toInt().toString(), textPaint)
    // АКПП: 270 градусов
    drawValueCenteredAt(geometry, 270f, dist, carData.transmissionTemp.toInt().toString(), textPaint)
    // Двигатель: 0 градусов
    drawValueCenteredAt(geometry, 0f, dist, carData.coolantTemp.toInt().toString(), textPaint)
}

private fun DrawScope.drawValueCenteredAt(
    geometry: DashboardType4Geometry,
    angle: Float,
    distance: Float,
    value: String,
    paint: android.graphics.Paint
) {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val centerX = geometry.center.x + cos(rad) * distance
    val centerY = geometry.center.y + sin(rad) * distance
    
    drawContext.canvas.nativeCanvas.apply {
        save()
        translate(centerX, centerY)
        drawText(value, 0f, -(paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f, paint)
        restore()
    }
}
