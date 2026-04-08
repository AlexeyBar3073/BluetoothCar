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
 */
@Dao
interface EcuErrorDao {

    /**
     * Получить данные об ошибке по её уникальному коду.
     * Используется TRIM и UPPER для игнорирования пробелов и регистра при поиске.
     */
    @Query("SELECT * FROM ecu_errors WHERE UPPER(TRIM(code)) = UPPER(TRIM(:code)) LIMIT 1")
    fun getErrorByCode(code: String): Flow<EcuErrorEntity?>

    @Query("SELECT * FROM ecu_errors WHERE code IN (:codes)")
    fun getErrorsByCodes(codes: List<String>): Flow<List<EcuErrorEntity>>

    @Query("SELECT * FROM ecu_errors")
    fun getAllErrorsList(): List<EcuErrorEntity>

    /**
     * Получить все записи, помеченные как комбинации ошибок.
     * Используется во ViewModel для сопоставления набора активных кодов с экспертной базой.
     */
    @Query("SELECT * FROM ecu_errors WHERE isCombination = 1")
    fun getAllCombinations(): Flow<List<EcuErrorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(errors: List<EcuErrorEntity>)

    @Query("DELETE FROM ecu_errors")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM ecu_errors")
    fun getCount(): Int
}
