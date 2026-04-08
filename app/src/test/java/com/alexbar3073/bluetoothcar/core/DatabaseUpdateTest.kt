// Файл: core/DatabaseUpdateTest.kt
package com.alexbar3073.bluetoothcar.core

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.database.AppDatabase
import com.alexbar3073.bluetoothcar.data.database.dao.EcuErrorDao
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * ТЕГ: Core/DatabaseUpdate/Test
 *
 * ФАЙЛ: core/DatabaseUpdateTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Тестирование функционала импорта и экспорта баз данных через AppController.
 * Проверяет логику "умного обновления" (Upsert), валидацию JSON-формата и корректность выгрузки данных.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка успешного импорта корректных данных об ошибках.
 * 2. Верификация защиты от некорректного формата JSON.
 * 3. Тестирование процесса экспорта текущей базы в файл.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseUpdateTest {

    /** Правило для подмены Main диспатчера в тестах */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var database: AppDatabase
    private lateinit var ecuErrorDao: EcuErrorDao
    private lateinit var appController: AppController
    
    private val testUri = mockk<Uri>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        database = mockk(relaxed = true)
        ecuErrorDao = mockk(relaxed = true)

        // Подменяем статический доступ к БД в ServiceLocator через моки в AppController (если возможно)
        // Но так как AppController использует ServiceLocator.getDatabase(), нам нужно замокать сам ServiceLocator
        mockkObject(ServiceLocator)
        every { ServiceLocator.getDatabase() } returns database
        every { database.ecuErrorDao() } returns ecuErrorDao
        every { context.contentResolver } returns contentResolver

        appController = AppController(context, settingsRepository)
    }

    @After
    fun tearDown() {
        // Обязательно очищаем ресурсы, чтобы остановить фоновые корутины AppController
        appController.cleanup()
        unmockkAll()
    }

    /**
     * Тест: Успешный импорт ошибок ЭБУ (Upsert логика).
     */
    @Test
    fun `importEcuErrorsFromUri should call insertAll when JSON is valid`() = runTest {
        // 1. ПОДГОТОВКА: Создаем валидный JSON с одной ошибкой
        val validJson = """
            [
              {
                "code": "P0300",
                "priority": 1,
                "canDrive": "ДА",
                "shortDescription": "Пропуски зажигания",
                "detailedDescription": "Обнаружены множественные пропуски",
                "symptoms": [],
                "clubExpertNote": "",
                "causes": [],
                "toolsNeeded": [],
                "relatedCodes": []
              }
            ]
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(validJson.toByteArray())
        every { contentResolver.openInputStream(testUri) } returns inputStream

        // 2. ДЕЙСТВИЕ: Вызываем импорт
        val result = appController.importEcuErrorsFromUri(testUri)

        // 3. ПРОВЕРКА: Результат - успех, 1 запись, вызван метод вставки
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        verify { ecuErrorDao.insertAll(any()) }
    }

    /**
     * Тест: Ошибка импорта при неверном формате данных.
     */
    @Test
    fun `importEcuErrorsFromUri should return failure when JSON format is invalid`() = runTest {
        // 1. ПОДГОТОВКА: JSON не соответствует модели (отсутствует обязательное поле code)
        val invalidJson = """[{"wrong_field": "data"}]"""
        val inputStream = ByteArrayInputStream(invalidJson.toByteArray())
        every { contentResolver.openInputStream(testUri) } returns inputStream

        // 2. ДЕЙСТВИЕ
        val result = appController.importEcuErrorsFromUri(testUri)

        // 3. ПРОВЕРКА: Результат - ошибка, insertAll НЕ вызван
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Формат данных") == true)
        verify(exactly = 0) { ecuErrorDao.insertAll(any()) }
    }

    /**
     * Тест: Успешный экспорт базы данных в JSON.
     */
    @Test
    fun `exportEcuErrorsToUri should write valid JSON to output stream`() = runTest {
        // 1. ПОДГОТОВКА: Настраиваем DAO на возврат одной ошибки
        val error = EcuErrorEntity(
            code = "P0101", priority = 2, canDrive = "ДА",
            shortDescription = "ДМРВ", detailedDescription = "Сигнал вне диапазона",
            symptoms = emptyList(), clubExpertNote = "", causes = emptyList(),
            toolsNeeded = emptyList(), relatedCodes = emptyList()
        )
        every { ecuErrorDao.getAllErrorsList() } returns listOf(error)
        
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(testUri) } returns outputStream

        // 2. ДЕЙСТВИЕ: Выполняем экспорт
        val result = appController.exportEcuErrorsToUri(testUri)

        // 3. ПРОВЕРКА: Записанные данные можно распарсить обратно в тот же объект
        assertTrue(result.isSuccess)
        val writtenJson = outputStream.toString()
        val exportedErrors = Json.decodeFromString<List<EcuErrorEntity>>(writtenJson)
        
        assertEquals(1, exportedErrors.size)
        assertEquals("P0101", exportedErrors[0].code)
    }
}
