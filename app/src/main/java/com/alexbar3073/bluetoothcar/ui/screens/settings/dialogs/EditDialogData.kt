package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

/**
 * Модель данных для диалога редактирования числовых значений
 *
 * @property title Заголовок диалога
 * @property currentValue Текущее значение
 * @property minValue Минимальное допустимое значение
 * @property maxValue Максимальное допустимое значение
 * @property unit Единица измерения (л, мл/мин, мс и т.д.)
 * @property step Шаг изменения (для будущих улучшений)
 * @property onSave Callback для сохранения нового значения
 */
data class EditDialogData(
    val title: String = "",
    val currentValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val unit: String = "",
    val step: Float = 1f,
    val onSave: (Float) -> Unit = {}
)