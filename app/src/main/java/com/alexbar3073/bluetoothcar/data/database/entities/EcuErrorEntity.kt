// Файл: data/database/entities/EcuErrorEntity.kt
package com.alexbar3073.bluetoothcar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * ТЕГ: Database/Entity/EcuError
 *
 * ФАЙЛ: data/database/entities/EcuErrorEntity.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/entities/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Представляет сущность ошибки ЭБУ в базе данных Room.
 * Описывает структуру таблицы справочника кодов ошибок.
 *
 * ОТВЕТСТВЕННОСТЬ: Хранение данных об ошибке, включая описания, симптомы и возможные причины.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room Entity
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Полнота данных для офлайн-справочника.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: (Использует: EcuErrorCause / Взаимодействует: EcuErrorDao.kt, AppDatabase.kt)
 */
@Serializable
@Entity(tableName = "ecu_errors")
data class EcuErrorEntity(
    /** Уникальный код ошибки (например, P0335) - является первичным ключом */
    @PrimaryKey
    val code: String,

    /** Отображаемое имя ошибки (может совпадать с кодом) */
    val name: String = "",

    /** Приоритет критичности ошибки */
    val priority: Int,

    /** Статус возможности продолжения движения (ДА/НЕТ/С ОГРАНИЧЕНИЯМИ) */
    val canDrive: String,

    /** Краткое описание (заголовок) ошибки */
    val shortDescription: String,

    /** Подробное техническое описание проблемы */
    val detailedDescription: String,

    /** Список наблюдаемых симптомов (хранится как JSON через TypeConverter) */
    val symptoms: List<String>,

    /** Примечание эксперта или специфические советы для конкретной модели */
    val clubExpertNote: String,

    /** Список возможных причин с вероятностью и действиями (хранится как JSON через TypeConverter) */
    val causes: List<EcuErrorCause>,

    /** Список инструментов, необходимых для диагностики/ремонта (хранится как JSON через TypeConverter) */
    val toolsNeeded: List<String>,

    /** Список связанных кодов ошибок (хранится как JSON через TypeConverter) */
    val relatedCodes: List<String>,

    /** 
     * Флаг, указывающий на то, является ли это комбинацией ошибок.
     * В текущей версии подсистемы используется для фильтрации при импорте.
     */
    val isCombination: Boolean = false
)

/**
 * Вспомогательная модель для описания причины ошибки.
 */
@Serializable
data class EcuErrorCause(
    /** Описание причины */
    val cause: String,
    /** Вероятность возникновения в процентах */
    val probability: Int,
    /** Рекомендуемое действие для проверки или устранения */
    val action: String
)
