// Файл: ui/screens/home/widgets/CompactBluetoothStatusWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.data.bluetooth.CompactDisplayType
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo

/**
 * ФАЙЛ: ui/screens/home/widgets/CompactBluetoothStatusWidget.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Компактная версия виджета для отображения состояния Bluetooth соединения.
 * Использует готовые данные из ConnectionStatusInfo.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * ТОЛЬКО ИСПОЛЬЗУЕТ готовые данные из ConnectionStatusInfo без дополнительных запросов.
 *
 * @param modifier Модификатор для настройки виджета
 * @param connectionStatusInfo Полная структура данных о статусе подключения (гарантированно не null)
 * @param onClick Функция, вызываемая при клике на виджет (опционально)
 */
@Composable
fun CompactBluetoothStatusWidget(
    modifier: Modifier = Modifier,
    connectionStatusInfo: ConnectionStatusInfo,
    onClick: (() -> Unit)? = null
) {
    // Анимация прогресса для индикатора (используется только для PROGRESS_INDICATOR)
    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = true,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.small,
        color = connectionStatusInfo.iconColor // Используем готовый цвет иконки как фон
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Выбираем тип отображения на основе compactDisplayType из ConnectionStatusInfo
            when (connectionStatusInfo.compactDisplayType) {
                CompactDisplayType.ICON -> {
                    // Статическая иконка - для большинства состояний
                    Icon(
                        imageVector = connectionStatusInfo.compactIcon, // Используем компактную иконку
                        contentDescription = connectionStatusInfo.displayName,
                        tint = Color.White, // Белый на цветном фоне
                        modifier = Modifier.size(16.dp)
                    )
                }
                CompactDisplayType.PROGRESS_INDICATOR -> {
                    // Анимированный круговой индикатор - для состояний процесса подключения
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        progress = { progress }, // Анимированное значение заполнения
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                CompactDisplayType.CUSTOM_ANIMATION -> {
                    // Зарезервировано для будущих кастомных анимаций
                    Icon(
                        imageVector = connectionStatusInfo.compactIcon,
                        contentDescription = connectionStatusInfo.displayName,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = connectionStatusInfo.shortStatusText, // Используем готовый короткий текст для UI
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}