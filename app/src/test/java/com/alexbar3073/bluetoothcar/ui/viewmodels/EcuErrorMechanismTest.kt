// Файл: ui/viewmodels/EcuErrorMechanismTest.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: UI/ViewModel/EcuErrorMechanismTest
 *
 * ФАЙЛ: ui/viewmodels/EcuErrorMechanismTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/viewmodels/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Тестирование механизма расшифровки ошибок ЭБУ. 
 * Проверяет цепочку: Коды в CarData -> Запрос в БД -> Список сущностей во ViewModel.
 *
 * ОТВЕТСТВЕННОСТЬ: Верификация корректности маппинга кодов ошибок в объекты описаний.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing (MockK + Turbine).
 */

@OptIn(ExperimentalCoroutinesApi::class)
class EcuErrorMechanismTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appController: AppController
    private lateinit var viewModel: SharedViewModel

    // Потоки имитации данных
    private val carDataFlow = MutableStateFlow(CarData())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appController = mockk(relaxed = true)

        // Базовые моки для инициализации ViewModel
        every { appController.carData } returns carDataFlow
        every { appController.appSettings } returns MutableStateFlow(AppSettings())
        every { appController.isInitialized } returns MutableStateFlow(true)
        every { appController.connectionStatusInfo } returns MutableStateFlow(com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState.UNDEFINED.toStatusInfo())

        viewModel = SharedViewModel(appController)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест проверяет, что при поступлении кодов ошибок в строке CarData,
     * ViewModel запрашивает их расшифровку и обновляет поток activeEcuErrors.
     */
    @Test
    fun `activeEcuErrors should update when carData contains error codes`() = runTest {
        // 1. ПОДГОТОВКА: Создаем тестовые сущности, которые "якобы" лежат в БД
        val errorP0300 = EcuErrorEntity(
            code = "P0300",
            priority = 1,
            canDrive = "ДА",
            shortDescription = "Пропуски зажигания",
            detailedDescription = "Обнаружены случайные пропуски зажигания",
            symptoms = emptyList(),
            clubExpertNote = "",
            causes = emptyList(),
            toolsNeeded = emptyList(),
            relatedCodes = emptyList()
        )
        
        val errorP0171 = errorP0300.copy(code = "P0171", shortDescription = "Бедная смесь")

        // 2. НАСТРОЙКА МОКА: Когда контроллер просят найти эти коды, он возвращает наш список
        every { 
            appController.getEcuErrorsByCodes(listOf("P0300", "P0171")) 
        } returns flowOf(listOf(errorP0300, errorP0171))

        // 3. ТЕСТИРОВАНИЕ ПОТОКА
        viewModel.activeEcuErrors.test {
            // Начальное состояние - пусто
            assertEquals(emptyList<EcuErrorEntity>(), awaitItem())

            // ДЕЙСТВИЕ: Машина прислала коды ошибок
            carDataFlow.value = CarData(ecuErrors = "P0300; P0171")

            // ПРОВЕРКА: ViewModel должна подхватить их, очистить от пробелов и вернуть расшифровки
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("P0300", result[0].code)
            assertEquals("P0171", result[1].code)
        }
    }

    /**
     * Тест проверяет, что пустая строка ошибок приводит к пустому списку в UI.
     */
    @Test
    fun `activeEcuErrors should be empty when carData errors are empty`() = runTest {
        viewModel.activeEcuErrors.test {
            assertEquals(emptyList<EcuErrorEntity>(), awaitItem())

            // Эмитируем пустую строку (или только разделители)
            carDataFlow.value = CarData(ecuErrors = ";  ;")
            
            // Состояние не должно измениться (или должен прийти пустой список после обработки)
            // В данном случае distinctUntilChanged может не пропустить одинаковые пустые списки,
            // но мы проверяем логику фильтрации.
            expectNoEvents()
        }
    }
}
