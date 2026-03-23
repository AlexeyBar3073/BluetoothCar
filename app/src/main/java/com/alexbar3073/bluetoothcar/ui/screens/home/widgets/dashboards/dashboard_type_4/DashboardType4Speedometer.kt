package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.CarData
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Виджет спидометра для дашборда Type 4. Полностью повторяет логику Type 1.
 */
@Composable
internal fun DashboardType4Speedometer(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: DashboardType4Geometry
) {
    val maxSpeed = 220f
    val maxVoltage = 16f
    val startupRiseDuration = 1300
    val startupFallDuration = 5000
    val transitionDuration = 1500

    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(
                maxSpeed,
                animationSpec = tween(startupRiseDuration, easing = FastOutSlowInEasing)
            )
            startupAnim.animateTo(
                0f,
                animationSpec = tween(startupFallDuration, easing = LinearOutSlowInEasing)
            )
            transitionAnim.animateTo(
                1f,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
            startupFinished = true
        }
    }

    val displaySpeed = if (!startupFinished) {
        if (transitionAnim.value > 0f) {
            val targetSpeed = max(0f, carData.speed)
            targetSpeed * transitionAnim.value
        } else {
            startupAnim.value
        }
    } else {
        carData.speed
    }

    val displayVoltage = if (!startupFinished) {
        if (transitionAnim.value > 0f) {
            val targetVoltage = max(0f, carData.voltage)
            targetVoltage * transitionAnim.value
        } else {
            startupAnim.value / maxSpeed * maxVoltage
        }
    } else {
        carData.voltage
    }

    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed.coerceIn(0f, maxSpeed),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "needle"
    )

    val animatedVoltage by animateFloatAsState(
        targetValue = displayVoltage.coerceIn(0f, maxVoltage),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "voltage"
    )

    val isLowVoltage = animatedVoltage <= 11.8f
    val isCharging = animatedVoltage > 13.0f

    val alarmIcon = painterResource(R.drawable.battery_full_48)
    val normalIcon = painterResource(R.drawable.battery_50)
    val chargingIcon = painterResource(R.drawable.battery_charging_50)
    val currentIcon = if (isCharging) chargingIcon else if (isLowVoltage) alarmIcon else normalIcon

    val blinkAlpha by animateFloatAsState(
        targetValue = if (isLowVoltage) 0.3f else 1f,
        animationSpec = if (isLowVoltage) infiniteRepeatable(animation = tween(500)) else tween(0),
        label = "blink_anim"
    )

    val speedPaint = remember {
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

    var ringRotation by remember { mutableFloatStateOf(0f) }
    val anglesState = remember(animatedVoltage, ringRotation) {
        derivedStateOf { calculateAngles(animatedVoltage, ringRotation) }
    }
    val angles by anglesState

    LaunchedEffect(angles.ringRotation) {
        if (angles.ringRotation != ringRotation) {
            ringRotation = angles.ringRotation
        }
    }

    val bitmapKey = remember(geometry.width, geometry.height) {
        Pair(geometry.width, geometry.height)
    }

    var scaleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var ringBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    val trailCache = rememberTrailCache(geometry)
    val windowPath = rememberVoltmeterWindow(geometry)

    val speedTextSize = 42f * geometry.unit
    val unitTextSize = 16f * geometry.unit

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawVoltmeter(
                geometry = geometry,
                voltage = animatedVoltage,
                ringBitmap = ringBitmap ?: buildVoltageRingBitmap(geometry).also { ringBitmap = it },
                ringRotation = ringRotation,
                iconPainter = currentIcon,
                alpha = if (isLowVoltage) blinkAlpha else 1f,
                needleAngle = angles.needleAngle,
                windowPath = windowPath
            )

            drawSpeedometer(
                geometry = geometry,
                speed = animatedSpeed,
                scaleBitmap = scaleBitmap ?: buildScaleBitmap(geometry, geometry.ringColor).also { scaleBitmap = it },
                needleBitmap = needleBitmap ?: buildNeedleBitmap(geometry, geometry.ringColor).also { needleBitmap = it },
                ringColor = geometry.ringColor,
                speedPaint = speedPaint,
                unitPaint = unitPaint,
                trailCache = trailCache,
                speedTextSize = speedTextSize,
                unitTextSize = unitTextSize
            )
        }
    }
}

private fun DrawScope.drawSpeedometer(
    geometry: DashboardType4Geometry,
    speed: Float,
    scaleBitmap: ImageBitmap,
    needleBitmap: ImageBitmap,
    ringColor: Color,
    speedPaint: android.graphics.Paint,
    unitPaint: android.graphics.Paint,
    trailCache: TrailCache,
    speedTextSize: Float,
    unitTextSize: Float
) {
    val needleAngle = geometry.startAngle + geometry.fullSweep * (speed / geometry.maxSpeed)

    drawImage(scaleBitmap, Offset.Zero)
    drawDualRadialGlow(geometry, speed, ringColor)

    rotate(needleAngle, pivot = geometry.center) {
        drawImage(needleBitmap, Offset.Zero)
    }

    drawSpeedText(speed, geometry, speedPaint, unitPaint, speedTextSize, unitTextSize)
    drawSpeedometerTrail(geometry, speed, trailCache)
}

private data class TrailCache(
    val innerRadius: Float,
    val radiusClipPath: Path
)

@Composable
private fun rememberTrailCache(geometry: DashboardType4Geometry): TrailCache {
    return remember(geometry.width, geometry.height) {
        val outerRadius = geometry.scaleRadius
        val radiusClipPath = Path().apply {
            addOval(Rect(geometry.center.x - outerRadius, geometry.center.y - outerRadius, geometry.center.x + outerRadius, geometry.center.y + outerRadius))
        }
        TrailCache(outerRadius - geometry.tickLarge, radiusClipPath)
    }
}

@Composable
private fun rememberVoltmeterWindow(geometry: DashboardType4Geometry): Path {
    return remember(geometry.width, geometry.height) {
        val radius = geometry.scaleRadius
        Path().apply {
            arcTo(Rect(geometry.center.x - radius, geometry.center.y - radius, geometry.center.x + radius, geometry.center.y + radius), 40f, 100f, true)
            lineTo(geometry.center.x, geometry.center.y)
            close()
        }
    }
}

private fun DrawScope.drawSpeedometerTrail(geometry: DashboardType4Geometry, speed: Float, trailCache: TrailCache) {
    val sweepAngle = geometry.fullSweep * (speed / geometry.maxSpeed)
    if (sweepAngle <= 0f) return

    val color = getTrailColor(speed)
    val currentAngle = geometry.startAngle + sweepAngle
    val angleRad = currentAngle * PI.toFloat() / 180f
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)

    val needleEnd = Offset(geometry.center.x + geometry.scaleRadius * cosA, geometry.center.y + geometry.scaleRadius * sinA)
    val bigRadius = geometry.scaleRadius * 1.1f
    val bigInnerRadius = bigRadius - geometry.tickLarge
    val bigCircleCenter = Offset(needleEnd.x - bigRadius * cosA, needleEnd.y - bigRadius * sinA)

    val innerStop = bigInnerRadius / bigRadius
    val colorStops = geometry.trailAlphaStops.mapIndexed { i, alpha ->
        (innerStop + i * (1f - innerStop) / (geometry.trailAlphaStops.size - 1)) to color.copy(alpha = alpha)
    }.toTypedArray()

    val radialBrush = Brush.radialGradient(colorStops = colorStops, center = bigCircleCenter, radius = bigRadius)

    clipPath(trailCache.radiusClipPath) {
        val sectorClipPath = Path().apply {
            arcTo(Rect(geometry.center.x - bigRadius, geometry.center.y - bigRadius, geometry.center.x + bigRadius, geometry.center.y + bigRadius), geometry.startAngle, sweepAngle, true)
            lineTo(geometry.center.x, geometry.center.y)
            close()
        }

        clipPath(sectorClipPath) {
            drawCircle(brush = radialBrush, center = bigCircleCenter, radius = bigRadius - geometry.tickLarge / 2f, style = Stroke(width = geometry.tickLarge))
        }
    }

    drawTrailBorder(geometry, sweepAngle, color)
}

private fun getTrailColor(speed: Float): Color {
    val white = Color.White; val green = Color.Green; val yellow = Color.Yellow; val orange = Color(0xFFFFA500); val red = Color.Red; val burgundy = Color(0xFF800000)
    return when {
        speed < 30f -> white
        speed in 30f..50f -> lerp(white, green, (speed - 30f) / 20f)
        speed < 60f -> green
        speed in 60f..80f -> lerp(green, yellow, (speed - 60f) / 20f)
        speed < 90f -> yellow
        speed in 90f..110f -> lerp(yellow, orange, (speed - 90f) / 20f)
        speed < 130f -> orange
        speed in 130f..150f -> lerp(orange, red, (speed - 130f) / 20f)
        speed < 170f -> red
        speed in 170f..190f -> lerp(red, burgundy, (speed - 170f) / 20f)
        else -> burgundy
    }
}

private fun DrawScope.drawTrailBorder(geometry: DashboardType4Geometry, sweepAngle: Float, color: Color) {
    val borderWidth = 1f * geometry.unit
    val borderRadius = geometry.scaleRadius + borderWidth / 2f
    val borderRect = Rect(geometry.center.x - borderRadius, geometry.center.y - borderRadius, geometry.center.x + borderRadius, geometry.center.y + borderRadius)

    drawArc(
        brush = Brush.sweepGradient(colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 1f)), center = geometry.center),
        startAngle = geometry.startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = borderRect.topLeft,
        size = borderRect.size,
        style = Stroke(width = borderWidth)
    )
}

private fun DrawScope.drawSpeedText(
    speed: Float,
    geometry: DashboardType4Geometry,
    speedPaint: android.graphics.Paint,
    unitPaint: android.graphics.Paint,
    speedTextSize: Float,
    unitTextSize: Float
) {
    speedPaint.textSize = speedTextSize
    unitPaint.textSize = unitTextSize

    drawContext.canvas.nativeCanvas.apply {
        val speedStr = speed.toInt().toString()
        val fm = speedPaint.fontMetrics
        val baseline = geometry.center.y - (fm.ascent + fm.descent) / 2f

        drawText(speedStr, geometry.center.x, baseline, speedPaint)
        drawText("км/ч", geometry.center.x, baseline + 32f * geometry.unit, unitPaint)
    }
}

private fun DrawScope.drawDualRadialGlow(
    geometry: DashboardType4Geometry,
    speed: Float,
    baseColor: Color,
    peakSpread: Float = 0.01f
) {
    val innerRadius = geometry.ringRadius - geometry.glowWidth
    val outerRadius = geometry.ringRadius + geometry.glowWidth
    if (innerRadius <= 0f) return

    val glowColor = if (speed <= 120f) baseColor else lerp(baseColor, Color.Red, ((speed - 120f) / 100f).coerceIn(0f, 1f))
    val innerStop = innerRadius / outerRadius
    val peakStop = geometry.ringRadius / outerRadius

    val leftPeak = (peakStop - peakSpread).coerceIn(0f, 1f)
    val rightPeak = (peakStop + peakSpread).coerceIn(0f, 1f)

    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            innerStop to Color.Transparent,
            leftPeak to glowColor.copy(alpha = 0.6f),
            peakStop to glowColor,
            rightPeak to glowColor.copy(alpha = 0.6f),
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

// ==============================
// ВОЛЬТМЕТР
// ==============================

private const val BASE_MIN_VOLTAGE = 12.0f
private const val BASE_MAX_VOLTAGE = 12.7f
private const val BASE_START_ANGLE = 140f
private const val BASE_SWEEP_ANGLE = -100f
private const val EXTENSION_DEGREES = 130f

private val BURGUNDY = Color(0xFF800000)
private val DARK_GREEN = Color(0xFF1B5E20)
private val DARK_BLUE = Color(0xFF0D47A1)

private data class VoltageAngles(val needleAngle: Float, val ringRotation: Float)

private fun DrawScope.drawVoltmeter(
    geometry: DashboardType4Geometry,
    voltage: Float,
    ringBitmap: ImageBitmap,
    ringRotation: Float,
    iconPainter: Painter,
    alpha: Float,
    needleAngle: Float,
    windowPath: Path
) {
    clipPath(windowPath) {
        rotate(ringRotation, pivot = geometry.center) {
            drawImage(ringBitmap, Offset(geometry.center.x - ringBitmap.width / 2f, geometry.center.y - ringBitmap.height / 2f))
        }
    }
    drawBatteryIconAndText(geometry, voltage, iconPainter, alpha)
    drawVoltageNeedle(geometry, needleAngle, voltageToColor(voltage))
}

private fun calculateAngles(voltage: Float, currentRingRotation: Float): VoltageAngles {
    val baseMinAngle = BASE_START_ANGLE
    val baseMaxAngle = BASE_START_ANGLE + BASE_SWEEP_ANGLE
    val degreesPerVolt = abs(BASE_SWEEP_ANGLE) / (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE)
    val voltageAngleAbsolute = BASE_START_ANGLE - (voltage - BASE_MIN_VOLTAGE) * degreesPerVolt

    return if (currentRingRotation != 0f) {
        val limiter = if (currentRingRotation > 0f) baseMaxAngle else baseMinAngle
        val desired = voltageAngleAbsolute + currentRingRotation
        val overflow = desired - limiter
        var newRotation = currentRingRotation - overflow

        if (currentRingRotation > 0f && newRotation < 0f) newRotation = 0f
        if (currentRingRotation < 0f && newRotation > 0f) newRotation = 0f

        newRotation = newRotation.coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(limiter, newRotation)
    } else {
        val clampedAngle = voltageAngleAbsolute.coerceIn(baseMaxAngle, baseMinAngle)
        val overflow = voltageAngleAbsolute - clampedAngle
        var newRotation = -overflow
        newRotation = newRotation.coerceIn(-EXTENSION_DEGREES, EXTENSION_DEGREES)
        if (abs(newRotation) < 0.0001f) newRotation = 0f
        VoltageAngles(clampedAngle, newRotation)
    }
}

private fun DrawScope.drawVoltageNeedle(geometry: DashboardType4Geometry, needleAngle: Float, ringColor: Color) {
    val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()
    val cos = cos(angleRad); val sin = sin(angleRad)
    val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
    val start = Offset(geometry.center.x + cos * innerGlowRadius, geometry.center.y + sin * innerGlowRadius)
    val tip = Offset(geometry.center.x + cos * (geometry.scaleRadius - 2f * geometry.unit), geometry.center.y + sin * (geometry.scaleRadius - 2f * geometry.unit))

    drawLine(brush = Brush.linearGradient(colors = listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)), start = start, end = tip), start = start, end = tip, strokeWidth = 5f * geometry.unit, cap = StrokeCap.Round)
    drawLine(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White), start = start, end = tip), start = start, end = tip, strokeWidth = 1.5f * geometry.unit, cap = StrokeCap.Round)
}

private fun voltageToColor(v: Float): Color = when {
    v <= 11.8f -> Color.Red
    v <= 12.0f -> lerp(BURGUNDY, Color.Red, ((v - 11.8f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.2f -> lerp(Color.Red, Color.Yellow, ((v - 12.0f) / 0.2f).coerceIn(0f, 1f))
    v <= 12.5f -> lerp(Color.Yellow, Color.Green, ((v - 12.2f) / 0.3f).coerceIn(0f, 1f))
    v <= 12.75f -> lerp(Color.Green, DARK_GREEN, ((v - 12.5f) / 0.25f).coerceIn(0f, 1f))
    v <= 13.1f -> lerp(DARK_GREEN, DARK_BLUE, ((v - 12.75f) / 0.35f).coerceIn(0f, 1f))
    else -> Color(0xFF3378D8)
}

private fun buildSweepBrush(): Brush {
    val extendedRange = (BASE_MAX_VOLTAGE - BASE_MIN_VOLTAGE) * (EXTENSION_DEGREES / abs(BASE_SWEEP_ANGLE))
    val extendedMin = BASE_MIN_VOLTAGE - extendedRange
    val extendedMax = BASE_MAX_VOLTAGE + extendedRange

    fun normalizeInverted(v: Float): Float = 1.0f - ((v - extendedMin) / (extendedMax - extendedMin)).coerceIn(0f, 1f)

    val stops = arrayOf(
        0.00f to DARK_BLUE,
        normalizeInverted(13.1f) to DARK_BLUE,
        normalizeInverted(13.0f) to DARK_GREEN,
        normalizeInverted(12.75f) to DARK_GREEN,
        normalizeInverted(12.5f) to Color.Green,
        normalizeInverted(12.2f) to Color.Yellow,
        normalizeInverted(12.0f) to Color.Red,
        normalizeInverted(11.8f) to BURGUNDY,
        1.00f to BURGUNDY
    )
    return Brush.sweepGradient(colorStops = stops)
}

private fun buildVoltageRingBitmap(geometry: DashboardType4Geometry): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val composeCanvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    val sweepBrush = buildSweepBrush()

    drawScope.draw(geometry.density, LayoutDirection.Ltr, composeCanvas, Size(geometry.width, geometry.height)) {
        val ringWidth = geometry.tickLarge; val ringOuterRadius = geometry.scaleRadius - ringWidth / 2f
        rotate(270f) {
            drawArc(brush = sweepBrush, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = ringWidth))
        }

        val innerRadius = ringOuterRadius - ringWidth / 2f
        val outerRadius = ringOuterRadius + ringWidth / 2f
        val thickness = outerRadius - innerRadius
        val dissolveDepth = thickness * 0.65f
        val fadeEnd = innerRadius + dissolveDepth

        val radialBrush = Brush.radialGradient(
            colorStops = arrayOf(0f to Color.Transparent, (innerRadius / outerRadius) to Color.Transparent, (fadeEnd / outerRadius) to Color.Black, 1f to Color.Black),
            center = geometry.center,
            radius = outerRadius
        )

        drawArc(brush = radialBrush, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = ringWidth), blendMode = BlendMode.DstIn)

        rotate(270f) {
            drawArc(color = Color.White.copy(alpha = 0.02f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(geometry.center.x - ringOuterRadius, geometry.center.y - ringOuterRadius), size = Size(ringOuterRadius * 2, ringOuterRadius * 2), style = Stroke(width = ringWidth), blendMode = BlendMode.Overlay)
        }
    }
    return bitmap
}

private fun buildScaleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.White, geometry.outerRingRadius, geometry.center, style = Stroke(geometry.outerStrokeWidth))
        val textPaint = android.graphics.Paint().apply { color = ringColor.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; textSize = geometry.textSizePx; isAntiAlias = true }
        for (i in 0..geometry.maxSpeed.toInt() step 2) {
            val angle = geometry.startAngle + geometry.fullSweep * (i / geometry.maxSpeed)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cos = cos(rad); val sin = sin(rad)
            val is20 = i % 20 == 0; val tickLen = if (is20) geometry.tickLarge else if (i % 10 == 0) geometry.tickMedium else geometry.tickSmall
            drawLine(ringColor, Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius), Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)), if (is20) 2f * geometry.unit else if (i % 10 == 0) 1.5f * geometry.unit else 1f * geometry.unit, StrokeCap.Round)
            if (is20) drawContext.canvas.nativeCanvas.apply { save(); translate(geometry.center.x + cos * geometry.textRadius, geometry.center.y + sin * geometry.textRadius); rotate(angle + 90f); drawText(i.toString(), 0f, -(textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f, textPaint); restore() }
        }
    }
    return bitmap
}

private fun buildNeedleBitmap(geometry: DashboardType4Geometry, ringColor: Color): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val drawScope = CanvasDrawScope()
    drawScope.draw(geometry.density, LayoutDirection.Ltr, Canvas(bitmap), Size(geometry.width, geometry.height)) {
        drawCircle(Color.Black, geometry.blackRadius, geometry.center, style = Stroke(geometry.blackStrokeWidth))

        drawHeatBloomSegment(radius = geometry.blackRadius, color = ringColor, geometry = geometry, sweepAngle = 45f, startAngle = 0f)

        val innerGlowRadius = geometry.ringRadius - geometry.glowWidth
        val tip = Offset(geometry.center.x + geometry.scaleRadius - 1f * geometry.unit, geometry.center.y)
        val start = Offset(geometry.center.x + innerGlowRadius, geometry.center.y)
        
        drawLine(Brush.linearGradient(listOf(ringColor.copy(alpha = 0f), ringColor.copy(alpha = 0.5f)), start, tip), start, tip, 5f * geometry.unit, StrokeCap.Round)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), start, tip), start, tip, 1.5f * geometry.unit, StrokeCap.Round)
    }
    return bitmap
}

private fun DrawScope.drawHeatBloomSegment(radius: Float, color: Color, geometry: DashboardType4Geometry, sweepAngle: Float = 45f, gradientWidth: Float = 7f, startAngle: Float = 270f) {
    val scaledGradientWidth = gradientWidth * geometry.unit
    val outerRadius = radius + scaledGradientWidth
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), Paint())

    drawCircle(brush = Brush.radialGradient(colorStops = arrayOf(0f to Color.Transparent, (radius - scaledGradientWidth) / outerRadius to Color.Transparent, radius / outerRadius to color, 1f to Color.Transparent), center = geometry.center, radius = outerRadius), radius = outerRadius, center = geometry.center)
    drawCircle(Color.White, radius, geometry.center, style = Stroke(1f * geometry.unit))

    drawIntoCanvas {
        it.nativeCanvas.save(); it.nativeCanvas.rotate(startAngle - 180f, geometry.center.x, geometry.center.y)
        val half = (sweepAngle / 360f) / 2f
        drawCircle(brush = Brush.sweepGradient(colorStops = arrayOf(0f to Color.Transparent, 0.5f - half to Color.Transparent, 0.5f to Color.Black, 0.5f + half to Color.Transparent, 1f to Color.Transparent), center = geometry.center), radius = outerRadius, center = geometry.center, blendMode = BlendMode.DstIn)
        it.nativeCanvas.restore()
    }
    canvas.restore()
}

private fun DrawScope.drawBatteryIconAndText(geometry: DashboardType4Geometry, voltage: Float, iconPainter: Painter, alpha: Float = 1f) {
    val iconSize = geometry.textSizePx; val voltageText = String.format(Locale.US, "%.1fV", voltage)
    val textPaint = android.graphics.Paint().apply { color = Color.Gray.toArgb(); textSize = geometry.textSizePx * 0.8f; isAntiAlias = true }
    val totalWidth = iconSize + 4f * geometry.unit + textPaint.measureText(voltageText)
    val blockCenterX = geometry.center.x; val blockCenterY = geometry.center.y + geometry.textRadius - 5f * geometry.unit
    val blockLeft = blockCenterX - totalWidth / 2f
    translate(blockLeft, blockCenterY - iconSize / 2f) { with(iconPainter) { draw(Size(iconSize, iconSize), alpha = alpha, colorFilter = ColorFilter.tint(voltageToColor(voltage))) } }
    drawContext.canvas.nativeCanvas.drawText(voltageText, blockLeft + iconSize + 4f * geometry.unit, blockCenterY - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
}
