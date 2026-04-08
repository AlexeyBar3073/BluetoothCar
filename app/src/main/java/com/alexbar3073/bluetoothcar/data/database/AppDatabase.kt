// Файл: data/database/AppDatabase.kt
package com.alexbar3073.bluetoothcar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alexbar3073.bluetoothcar.data.database.converters.EcuErrorTypeConverters
import com.alexbar3073.bluetoothcar.data.database.dao.EcuErrorDao
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity

/**
 * ТЕГ: Database/AppDatabase
 *
 * ФАЙЛ: data/database/AppDatabase.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Основной класс базы данных Room. Служит точкой доступа к данным приложения.
 * Содержит конфигурацию таблиц, конвертеров и предоставляет доступ к DAO.
 *
 * ОТВЕТСТВЕННОСТЬ: Управление жизненным циклом БД и предоставление объектов доступа (DAO).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room Database (Singleton pattern для инициализации)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Централизованное хранение всех локальных данных.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: (Использует: EcuErrorEntity.kt, EcuErrorDao.kt, EcuErrorTypeConverters.kt / Взаимодействует: ServiceLocator.kt)
 */
@Database(entities = [EcuErrorEntity::class], version = 5, exportSchema = false)
@TypeConverters(EcuErrorTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Получить DAO для работы с ошибками ЭБУ.
     */
    abstract fun ecuErrorDao(): EcuErrorDao

    companion object {
        /** Имя файла базы данных */
        private const val DATABASE_NAME = "car_database.db"

        /**
         * Создать и инициализировать экземпляр базы данных.
         * Используется внутри ServiceLocator.
         *
         * @param context Контекст приложения
         * @return Экземпляр AppDatabase
         */
        fun build(context: Context): AppDatabase {
            // Строим базу данных через Room builder
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            // В реальном приложении здесь могут быть миграции
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
