// Файл: data/database/entities/EcuCombinationEntity.kt
package com.alexbar3073.bluetoothcar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ТЕГ: Database/Entity/EcuCombination
 *
 * ФАЙЛ: data/database/entities/EcuCombinationEntity.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/entities/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Представляет сущность комбинации ошибок ЭБУ. 
 * Используется для маппинга экспертных правил из JSON в базу данных Room.
 *
 * ОТВЕТСТВЕННОСТЬ: Хранение данных о сложных диагностических случаях.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room Entity + Kotlinx Serialization
 */
@Serializable
@Entity(tableName = "ecu_combinations")
data class EcuCombinationEntity(
    /** Уникальный идентификатор комбинации (автогенерация Room) */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Строковый ID правила из JSON (для обновления) */
    @SerialName("ruleId")
    val ruleId: String = "",

    /** Список кодов ошибок, входящих в комбинацию */
    @SerialName("combination")
    val codes: List<String>,

    /** Приоритет важности комбинации (используется для сортировки) */
    @SerialName("priority")
    val priority: Int = 50,

    /** Возможность движения при данной комбинации */
    @SerialName("canDrive")
    val canDrive: String = "ДА",

    /** Заголовок диагноза */
    @SerialName("diagnosis")
    val title: String,

    /** Краткое описание (в JSON отсутствует, берем часть диагноза или дефолт) */
    val shortDescription: String = "Комбинированная проблема",

    /** Подробное техническое описание совокупной проблемы */
    @SerialName("detailedExplanation")
    val detailedDescription: String,

    /** Список симптомов (в этой версии JSON может быть пустым) */
    val symptoms: List<String> = emptyList(),

    /** Примечание эксперта */
    @SerialName("expertTip")
    val clubExpertNote: String,

    /** Рекомендуемые действия (мапим список строк из JSON) */
    @SerialName("actions")
    val actions: List<String> = emptyList(),

    /** Список возможных причин (вычисляемое или мапируемое поле) */
    val causes: List<EcuErrorCause> = emptyList()
)
