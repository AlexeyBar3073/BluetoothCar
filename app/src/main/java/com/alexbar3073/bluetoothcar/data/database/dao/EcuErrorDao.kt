// Файл: data/database/dao/EcuErrorDao.kt
package com.alexbar3073.bluetoothcar.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import kotlinx.coroutines.flow.Flow

/**
 * ТЕГ: Database/DAO/EcuError
 *
 * ФАЙЛ: data/database/dao/EcuErrorDao.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/dao/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Объект доступа к данным (DAO) для таблицы ecu_errors.
 * Содержит методы для выполнения SQL-запросов к базе данных Room.
 *
 * ОТВЕТСТВЕННОСТЬ: Выполнение операций чтения и записи для сущностей ошибок ЭБУ.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room DAO
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Реактивность через Flow и безопасная вставка данных.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: (Использует: EcuErrorEntity.kt / Взаимодействует: AppDatabase.kt, AppController.kt)
 */
@Dao
interface EcuErrorDao {

    /**
     * Получить данные об ошибке по её уникальному коду.
     * @param code Код ошибки (например, P0335)
     * @return Объект ошибки или null, если не найдено
     */
    @Query("SELECT * FROM ecu_errors WHERE code = :code LIMIT 1")
    suspend fun getErrorByCode(code: String): EcuErrorEntity?

    /**
     * Получить список данных для нескольких кодов одновременно.
     * Используется для отображения списка текущих активных ошибок.
     * @param codes Список кодов ошибок
     * @return Список найденных сущностей
     */
    @Query("SELECT * FROM ecu_errors WHERE code IN (:codes)")
    fun getErrorsByCodes(codes: List<String>): Flow<List<EcuErrorEntity>>

    /**
     * Массовая вставка ошибок в базу данных.
     * Используется при первичном импорте из JSON.
     * Если запись с таким кодом уже существует — она будет заменена (REPLACE).
     * @param errors Список сущностей для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(errors: List<EcuErrorEntity>)

    /**
     * Получить общее количество записей в таблице.
     * Позволяет проверить, пуста ли база перед импортом.
     * @return Количество строк
     */
    @Query("SELECT COUNT(*) FROM ecu_errors")
    suspend fun getCount(): Int
}
