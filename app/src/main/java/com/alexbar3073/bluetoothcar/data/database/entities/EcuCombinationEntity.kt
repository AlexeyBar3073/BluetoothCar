// Файл: data/database/entities/EcuCombinationEntity.kt
package com.alexbar3073.bluetoothcar.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
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
 * Используется, когда набор определенных ошибок вместе указывает на специфическую проблему, 
 * отличную от суммы одиночных ошибок.
 *
 * ОТВЕТСТВЕННОСТЬ: Хранение данных о сложных диагностических случаях (комбинациях).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room Entity
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Сопоставление набора входящих кодов с экспертной базой знаний.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: (Использует: EcuErrorCause / Взаимодействует: EcuCombinationDao.kt, AppDatabase.kt)
 */
@Serializable
@Entity(tableName = "ecu_combinations")
data class EcuCombinationEntity(
    /** Уникальный идентификатор комбинации */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Список кодов ошибок, которые должны присутствовать одновременно для активации этой записи */
    val codes: List<String>,

    /** Заголовок комбинации (например, "Проблема с цепью питания датчиков") */
    val title: String,

    /** Краткое описание для карточки в списке */
    val shortDescription: String,

    /** Подробное техническое описание совокупной проблемы */
    val detailedDescription: String,

    /** Список симптомов, характерных именно для этой комбинации */
    val symptoms: List<String>,

    /** Примечание эксперта */
    val clubExpertNote: String,

    /** Возможные причины и решения для данной комбинации */
    val causes: List<EcuErrorCause>
)
