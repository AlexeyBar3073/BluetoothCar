// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/screens/home/widgets/dashboards/dashboard_type_4/DashboardType4CombinedGauge.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Источники данных для центрального вывода комбинированного прибора.
 * Используется для управления состоянием отображения в DashboardType4CombinedGauge.
 */
internal enum class GaugeSource {
    FUEL, TRANS_TEMP, ENGINE_TEMP
}

/**
 * ТЕГ: Комбинированный прибор дашборда 4
 */

/**
 * Основной Composable-компонент комбинированного прибора.
 */
@Composable
internal fun DashboardType4CombinedGauge(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: DashboardType4Geometry
) {
    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f
    
    val startupRiseDuration = 1300
    val startupFallDuration = 5000
    val transitionDuration = 1500

    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) } 
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(1f, animationSpec = tween(startupRiseDuration, easing = FastOutSlowInEasing))
            startupAnim.animateTo(0f, animationSpec = tween(startupFallDuration, easing = LinearOutSlowInEasing))
            transitionAnim.animateTo(1f, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing))
            startupFinished = true
        }
    }

    var selectedSource by rememberSaveable { mutableStateOf(GaugeSource.ENGINE_TEMP) }

    fun getTempRatio(temp: Float): Float {
        val t = temp.coerceIn(50f, 130f)
        return (t - 50f) / 80f
    }

    val targetFuel = (carData.fuel / fuelTankCapacity).coerceIn(0f, 1f)
    val targetEngine = getTempRatio(carData.coolantTemp)
    val targetTrans = getTempRatio(carData.transmissionTemp)

    fun getDisplayRatio(target: Float): Float {
        return if (!startupFinished) {
            if (transitionAnim.value > 0f) target * transitionAnim.value else startupAnim.value
        } else target
    }

    val currentFuelRatio = getDisplayRatio(targetFuel)
    val currentEngineRatio = getDisplayRatio(targetEngine)
    val currentTransRatio = getDisplayRatio(targetTrans)

    val animatedFuel by animateFloatAsState(currentFuelRatio, spring(0.8f, 80f), label = "fuel")
    val animatedEngine by animateFloatAsState(currentEngineRatio, spring(0.8f, 80f), label = "engine")
    val animatedTrans by animateFloatAsState(currentTransRatio, spring(0.8f, 80f), label = "trans")

    val fuelIcon = painterResource(R.drawable.fuel_50)
    val engineIcon = painterResource(R.drawable.engine_coolant_new)
    val transIcon = painterResource(R.drawable.oil_temperature_new)

    val batteryIcon = painterResource(R.drawable.ic_battery_full_50)
    val tirePressureIcon = painterResource(R.drawable.ic_tire_pressure_48)
    val checkEngineIcon = painterResource(R.drawable.ic_engine_48)

    val bitmapKey = remember(geometry.width, geometry.height) { Pair(geometry.width, geometry.height) }
    var staticBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    val valuePaint = remember {
        android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }
    val unitPaint = remember {
        android.graphics.Paint().apply {
            color = Color.Gray.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val (displayValue, displayUnit, activeAngle) = when (selectedSource) {
        GaugeSource.FUEL -> Triple(if (startupFinished) carData.fuel else (targetFuel * transitionAnim.value * fuelTankCapacity), "л", 180f)
        GaugeSource.TRANS_TEMP -> Triple(if (startupFinished) carData.transmissionTemp else (50f + 80f * currentTransRatio), "°C", 270f)
        GaugeSource.ENGINE_TEMP -> Triple(if (startupFinished) carData.coolantTemp else (50f + 80f * currentEngineRatio), "°C", 0f)
    }

    Box(modifier = modifier
        .pointerInput(geometry.center) {
            detectTapGestures { offset ->
                val dx = offset.x - geometry.center.x
                val dy = offset.y - geometry.center.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < geometry.blackRadius) {
                    selectedSource = when (selectedSource) {
                        GaugeSource.ENGINE_TEMP -> GaugeSource.TRANS_TEMP
                        GaugeSource.TRANS_TEMP -> GaugeSource.FUEL
                        GaugeSource.FUEL -> GaugeSource.ENGINE_TEMP
                    }
                } else if (dist < geometry.outerRingRadius) {
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    selectedSource = when {
                        angle in 130f..230f -> GaugeSource.FUEL
                        angle in 231f..310f -> GaugeSource.TRANS_TEMP
                        angle > 310f || angle < 50f -> GaugeSource.ENGINE_TEMP
                        else -> selectedSource
                    }
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White, radius = geometry.outerRingRadius, center = geometry.center, style = Stroke(width = geometry.outerStrokeWidth))
            if (staticBitmap == null) staticBitmap = buildStaticBitmap(geometry)
            staticBitmap?.let { drawImage(it) }

            drawDualRadialGlow(geometry, geometry.ringColor)
            drawBlackRing(geometry)
            drawGaugeIcons(geometry, fuelIcon, engineIcon, transIcon)
            drawBottomIcons(geometry, batteryIcon, tirePressureIcon, checkEngineIcon)
            drawCenterValue(displayValue, displayUnit, geometry, valuePaint, unitPaint)
            drawActiveSourceArrow(geometry, activeAngle, geometry.ringColor)

            drawNeedleWithBloom(geometry, 140f + 80f * animatedFuel, geometry.ringColor)
            drawNeedleWithBloom(geometry, 230f + 80f * animatedTrans, geometry.ringColor)
            drawNeedleWithBloom(geometry, 40f - 80f * animatedEngine, geometry.ringColor)
        }
    }
}

private fun DrawScope.drawCenterValue(value: Float, unit: String, geometry: DashboardType4Geometry, valuePaint: android.graphics.Paint, unitPaint: android.graphics.Paint) {
    val valueTextSize = 42f * geometry.unit
    val unitTextSize = 16f * geometry.unit
    valuePaint.textSize = valueTextSize
    unitPaint.textSize = unitTextSize
    drawContext.canvas.nativeCanvas.apply {
        val valStr = value.toInt().toString()
        val fm = valuePaint.fontMetrics
        val baseline = geometry.center.y - (fm.ascent + fm.descent) / 2f
        drawText(valStr, geometry.center.x, baseline, valuePaint)
        drawText(unit, geometry.center.x, baseline + 32f * geometry.unit, unitPaint)
    }
}

private fun DrawScope.drawActiveSourceArrow(geometry: DashboardType4Geometry, angle: Float, color: Color) {
    val arrowRadius = geometry.blackRadius + 4f * geometry.unit
    val arrowSize = 6f * geometry.unit
    rotate(angle, pivot = geometry.center) {
        val path = Path().apply {
            moveTo(arrowRadius + arrowSize, 0f)
            lineTo(arrowRadius, -arrowSize / 1.5f)
            lineTo(arrowRadius, arrowSize / 1.5f)
            close()
        }
        translate(geometry.center.x, geometry.center.y) { drawPath(path, color) }
    }
}

private fun DrawScope.drawDualRadialGlow(geometry: DashboardType4Geometry, color: Color, peakSpread: Float = 0.01f) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth
    val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return
    val innerStop = innerRadius / outerRadius
    val peakStop = geometry.ringRadius / outerRadius
    val leftPeak = (peakStop - peakSpread).coerceIn(0f, 1f)
    val rightPeak = (peakStop + peakSpread).coerceIn(0f, 1f)
    val brush = Brush.radialGradient(colorStops = arrayOf(innerStop to Color.Transparent, leftPeak to color.copy(alpha = 0.6f), peakStop to color, rightPeak to color.copy(alpha = 0.6f), 1f to Color.Transparent), center = geometry.center, radius = outerRadius)
    drawCircle(brush = brush, center = geometry.center, radius = (outerRadius + innerRadius) / 2f, style = Stroke(width = outerRadius - innerRadius))
}

private fun DrawScope.drawBlackRing(geometry: DashboardType4Geometry) {
    drawCircle(color = Color.Black, radius = geometry.blackRadius, center = geometry.center, style = Stroke(width = geometry.blackStrokeWidth))
    drawCircle(color = Color.White.copy(alpha = 0.1f), radius = geometry.blackRadius + geometry.blackStrokeWidth / 2f, center = geometry.center, style = Stroke(width = 1f * geometry.unit))
}

private fun DrawScope.drawNeedleWithBloom(geometry: DashboardType4Geometry, angle: Float, color: Color) {
    val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
    rotate(angle, pivot = geometry.center) {
        drawHeatBloomSegment(radius = geometry.blackRadius, color = color, geometry = geometry)
        val tip = Offset(geometry.center.x + geometry.scaleRadius - 2f * geometry.unit, geometry.center.y)
        val start = Offset(geometry.center.x + innerGlowRadius, geometry.center.y)
        drawLine(brush = Brush.linearGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f)), start, tip), start, tip, 5f * geometry.unit, StrokeCap.Round)
        drawLine(brush = Brush.linearGradient(listOf(Color.Transparent, Color.White), start, tip), start, tip, 1.5f * geometry.unit, StrokeCap.Round)
    }
}

private fun DrawScope.drawHeatBloomSegment(radius: Float, color: Color, geometry: DashboardType4Geometry, sweepAngle: Float = 45f, gradientWidth: Float = 7f, startAngle: Float = 0f) {
    val scaledGradientWidth = gradientWidth * geometry.unit
    val outerRadius = radius + scaledGradientWidth
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), Paint())
    drawCircle(brush = Brush.radialGradient(colorStops = arrayOf(0f to Color.Transparent, (radius - scaledGradientWidth) / outerRadius to Color.Transparent, radius / outerRadius to color, 1f to Color.Transparent), center = geometry.center, radius = outerRadius), radius = outerRadius, center = geometry.center)
    drawCircle(color = Color.White, radius = radius, center = geometry.center, style = Stroke(1f * geometry.unit))
    drawIntoCanvas {
        it.nativeCanvas.save()
        it.nativeCanvas.rotate(startAngle - 180f, geometry.center.x, geometry.center.y)
        val half = (sweepAngle / 360f) / 2f
        drawCircle(brush = Brush.sweepGradient(colorStops = arrayOf(0f to Color.Transparent, 0.5f - half to Color.Transparent, 0.5f to Color.Black, 0.5f + half to Color.Transparent, 1f to Color.Transparent), center = geometry.center), radius = outerRadius, center = geometry.center, blendMode = BlendMode.DstIn)
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
        drawSegmentScale(geometry, 40f, -80f, labels = listOf("50", "90", "130"), skipMiddleLabel = true)
    }
    return bitmap
}

private fun DrawScope.drawSegmentScale(geometry: DashboardType4Geometry, startAngle: Float, sweepAngle: Float, labels: List<String> = emptyList(), skipMiddleLabel: Boolean = false) {
    val ringColor = geometry.ringColor
    val steps = 40
    val textPaint = android.graphics.Paint().apply { color = ringColor.toArgb(); textSize = geometry.textSizePx; isAntiAlias = true }
    for (i in 0..steps) {
        val angle = startAngle + sweepAngle * (i / steps.toFloat())
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cos = cos(rad); val sin = sin(rad)
        val isMajor = i % 10 == 0; val isMedium = i % 5 == 0 && !isMajor
        val tickLen = if (isMajor) geometry.tickLarge else if (isMedium) geometry.tickMedium else geometry.tickSmall
        val strokeWidth = if (isMajor) 2f * geometry.unit else if (isMedium) 1.5f * geometry.unit else 1f * geometry.unit
        drawLine(color = ringColor, start = Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius), end = Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        if (labels.isNotEmpty() && i % 20 == 0) {
            val labelIndex = i / 20
            if (labelIndex < labels.size && !(skipMiddleLabel && labelIndex == 1)) {
                val text = labels[labelIndex]
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.save()
                    textPaint.textAlign = android.graphics.Paint.Align.CENTER
                    if (sweepAngle > 0) {
                        if (labelIndex == 0) textPaint.textAlign = android.graphics.Paint.Align.LEFT
                        if (labelIndex == 2) textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                    } else {
                        if (labelIndex == 0) textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                        if (labelIndex == 2) textPaint.textAlign = android.graphics.Paint.Align.LEFT
                    }
                    val originalSize = geometry.textSizePx
                    textPaint.textSize = if (text.contains("/")) originalSize * 0.7f else originalSize
                    val tRadius = geometry.scaleRadius - tickLen - 12f * geometry.unit
                    canvas.nativeCanvas.translate(geometry.center.x + cos * tRadius, geometry.center.y + sin * tRadius)
                    canvas.nativeCanvas.rotate(angle + 90f)
                    canvas.nativeCanvas.drawText(text, 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint)
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
}

private fun DrawScope.drawGaugeIcons(geometry: DashboardType4Geometry, fuelIcon: Painter, engineIcon: Painter, transIcon: Painter) {
    val baseIconSize = 20f * geometry.unit
    val dist = geometry.scaleRadius - geometry.tickLarge - 12f * geometry.unit
    val ringColor = geometry.ringColor
    drawIconCenteredAt(geometry, 180f, dist, baseIconSize, fuelIcon, ringColor, shouldRotate = false)
    drawIconCenteredAt(geometry, 270f, dist, baseIconSize, transIcon, ringColor, shouldRotate = false)
    drawIconCenteredAt(geometry, 0f, dist, baseIconSize, engineIcon, ringColor, shouldRotate = false)
}

private fun DrawScope.drawBottomIcons(geometry: DashboardType4Geometry, batteryIcon: Painter, tirePressureIcon: Painter, checkEngineIcon: Painter) {
    val iconSize = 22f * geometry.unit
    // ВОЗВРАЩЕНО: радиус длинной риски для комбинированного прибора
    val dist = geometry.scaleRadius - geometry.tickLarge
    val mainColor = geometry.ringColor
    drawIconCenteredAt(geometry, 65f, dist, iconSize, checkEngineIcon, mainColor, showGlow = true)
    drawIconCenteredAt(geometry, 90f, dist, iconSize, batteryIcon, mainColor, showGlow = true)
    drawIconCenteredAt(geometry, 115f, dist, iconSize, tirePressureIcon, mainColor, showGlow = true)
}

private fun DrawScope.drawIconCenteredAt(geometry: DashboardType4Geometry, angle: Float, distance: Float, size: Float, painter: Painter, color: Color, shouldRotate: Boolean = true, showGlow: Boolean = false) {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val centerX = geometry.center.x + cos(rad) * distance
    val centerY = geometry.center.y + sin(rad) * distance
    if (showGlow) {
        val glowRadius = size * 1.5f
        drawCircle(brush = Brush.radialGradient(0f to color.copy(alpha = 0.5f), 0.3f to color.copy(alpha = 0.2f), 0.7f to color.copy(alpha = 0.05f), 1f to Color.Transparent, center = Offset(centerX, centerY), radius = glowRadius), radius = glowRadius, center = Offset(centerX, centerY))
    }
    rotate(if (shouldRotate) angle - 90f else 0f, pivot = Offset(centerX, centerY)) {
        translate(centerX - size / 2f, centerY - size / 2f) {
            with(painter) { draw(Size(size, size), colorFilter = ColorFilter.tint(color)) }
        }
    }
}
