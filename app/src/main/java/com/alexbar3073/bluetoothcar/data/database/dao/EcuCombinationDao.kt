// Файл: data/database/dao/EcuCombinationDao.kt
package com.alexbar3073.bluetoothcar.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alexbar3073.bluetoothcar.data.database.entities.EcuCombinationEntity
import kotlinx.coroutines.flow.Flow

/**
 * ТЕГ: Database/DAO/EcuCombination
 *
 * ФАЙЛ: data/database/dao/EcuCombinationDao.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/dao/
 */
@Dao
interface EcuCombinationDao {

    @Query("SELECT * FROM ecu_combinations")
    fun getAllCombinations(): Flow<List<EcuCombinationEntity>>

    /**
     * Получить все комбинации в виде списка для экспорта.
     */
    @Query("SELECT * FROM ecu_combinations")
    fun getAllCombinationsList(): List<EcuCombinationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(combinations: List<EcuCombinationEntity>)

    @Query("DELETE FROM ecu_combinations")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM ecu_combinations")
    fun getCount(): Int
}
