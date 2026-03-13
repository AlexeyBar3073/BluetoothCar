// Файл: ui/screens/home/widgets/StatusCircleButton.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.data.bluetooth.CompactDisplayType
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ФАЙЛ: ui/screens/home/widgets/StatusCircleButton.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Круглая кнопка статуса подключения для топ-бара.
 * Заменяет CompactBluetoothStatusWidget в навигационной части топ-бара.
 * Стиль аналогичен кнопке настроек: круг с фоном SurfaceMedium.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * Использует готовые данные из ConnectionStatusInfo для отображения иконки/индикатора.
 * Стиль кнопки унифицирован с кнопкой настроек.
 *
 * @param modifier Модификатор для настройки кнопки
 * @param connectionStatusInfo Полная структура данных о статусе подключения
 * @param onClick Функция, вызываемая при клике на кнопку
 */
@Composable
fun StatusCircleButton(
    modifier: Modifier = Modifier,
    connectionStatusInfo: ConnectionStatusInfo,
    onClick: () -> Unit
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(
                enabled = true,
                onClick = onClick
            )
    ) {
        // Фон кнопки - такой же как у кнопки настроек
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceMedium)
        )

        // Выбираем тип отображения на основе compactDisplayType
        when (connectionStatusInfo.compactDisplayType) {
            CompactDisplayType.ICON -> {
                // Статическая иконка - для большинства состояний
                Icon(
                    imageVector = connectionStatusInfo.compactIcon,
                    contentDescription = connectionStatusInfo.displayName,
                    tint = connectionStatusInfo.iconColor, // Цвет иконки из статуса
                    modifier = Modifier.size(16.dp)
                )
            }
            CompactDisplayType.PROGRESS_INDICATOR -> {
                // Анимированный круговой индикатор - для состояний процесса подключения
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    progress = { progress }, // Анимированное значение заполнения
                    strokeWidth = 2.dp,
                    color = connectionStatusInfo.iconColor // Цвет индикатора из статуса
                )
            }
            CompactDisplayType.CUSTOM_ANIMATION -> {
                // Зарезервировано для будущих кастомных анимаций
                Icon(
                    imageVector = connectionStatusInfo.compactIcon,
                    contentDescription = connectionStatusInfo.displayName,
                    tint = connectionStatusInfo.iconColor, // Цвет иконки из статуса
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}