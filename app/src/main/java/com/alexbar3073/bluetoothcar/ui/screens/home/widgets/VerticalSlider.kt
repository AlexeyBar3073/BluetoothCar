package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color

@Deprecated("Данный компонент не используется в текущей версии интерфейса")
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier
                .rotate(270f)
                .fillMaxWidth()
            //    .width(200.dp)
            ,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF5722),
                activeTrackColor = Color(0xFF2196F3),
                inactiveTrackColor = Color(0xFF757575),
            )
        )
    }
}