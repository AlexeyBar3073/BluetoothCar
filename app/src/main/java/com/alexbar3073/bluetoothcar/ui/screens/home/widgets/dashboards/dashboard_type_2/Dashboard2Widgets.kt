package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.alexbar3073.bluetoothcar.R

@Composable
fun SpeedometerNeedle(
    modifier: Modifier = Modifier,
    speed: Float,
    baseCenter: Offset // Координаты в системе 1280x713
) {
    val needlePainter = painterResource(id = R.drawable.pointer_small_2)
    val intrinsicSize = needlePainter.intrinsicSize
    val pivotInResource = Offset(32f, 251f)

    val startAngle = 150f 
    val sweepAngle = 240f
    val maxSpeed = 220f
    
    val currentGaugeAngle = startAngle + (speed / maxSpeed) * sweepAngle
    val rotationAngle = currentGaugeAngle - 270f // Ресурс смотрит на 12ч

    Canvas(modifier = modifier.fillMaxSize()) {
        // Вычисляем масштаб исходя из реального размера Canvas в пикселях
        val scale = size.width / 1280f
        
        val centerPx = Offset(baseCenter.x * scale, baseCenter.y * scale)
        val pivotPx = Offset(pivotInResource.x * scale, pivotInResource.y * scale)
        val scaledSize = Size(intrinsicSize.width * scale, intrinsicSize.height * scale)

        withTransform({
            translate(centerPx.x, centerPx.y)
            rotate(rotationAngle, pivot = Offset.Zero)
            translate(-pivotPx.x, -pivotPx.y)
        }) {
            with(needlePainter) {
                draw(size = scaledSize)
            }
        }
    }
}

@Composable
fun SmallNeedle(
    modifier: Modifier = Modifier,
    value: Float,
    maxValue: Float,
    startAngle: Float,
    sweepAngle: Float,
    baseCenter: Offset
) {
    // Использование LocalContext и getIdentifier помогает обойти ошибку NoSuchFieldError в Preview,
    // которая возникает при рассинхронизации сгенерированного R-класса и ресурсов.
    val context = LocalContext.current
    val needlePainterId = remember(context) {
        val id = context.resources.getIdentifier("pointer_small_2", "drawable", context.packageName)
        // Если идентификатор не найден через ресурсы (что странно, но бывает в Preview), 
        // используем прямую ссылку на R-поле или запасной вариант.
        if (id != 0) id else R.drawable.pointer_small_2
    }
    
    val needlePainter = painterResource(id = needlePainterId)
    val intrinsicSize = needlePainter.intrinsicSize
    
    // Если произошел откат к большой стрелке в Preview, корректируем точку вращения
    val pivotInResource = if (needlePainterId == R.drawable.pointer_small_2) {
        Offset(32f, 251f)
    } else {
        Offset(17f, 100f)
    }

    val currentGaugeAngle = startAngle + (value.coerceIn(0f, maxValue) / maxValue) * sweepAngle
    val rotationAngle = currentGaugeAngle - 270f // Ресурс смотрит на 12ч

    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = size.width / 1280f
        
        val centerPx = Offset(baseCenter.x * scale, baseCenter.y * scale)
        val pivotPx = Offset(pivotInResource.x * scale, pivotInResource.y * scale)
        val scaledSize = Size(intrinsicSize.width * scale, intrinsicSize.height * scale)

        withTransform({
            translate(centerPx.x, centerPx.y)
            rotate(rotationAngle, pivot = Offset.Zero)
            translate(-pivotPx.x, -pivotPx.y)
        }) {
            with(needlePainter) {
                draw(size = scaledSize)
            }
        }
    }
}
