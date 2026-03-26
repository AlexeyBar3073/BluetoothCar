// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/DataStreamHandlerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты обработчика протокола данных
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для DataStreamHandler. Проверяют реализацию 5-фазного JSON-протокола
 * обмена данными: отправка настроек, подтверждение, запрос данных, парсинг CarData.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: DataStreamHandler.kt
 * 2. Мокает: BluetoothService.kt
 * 3. Использует: AppSettings, CarData, ConnectionState
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStreamHandlerTest {

    private lateinit var bluetoothService: BluetoothService
    private lateinit var handler: DataStreamHandler
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    /** Имитация входящего потока данных из BluetoothService */
    private val incomingDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * Настройка перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        
        // Мокаем получение данных от сервиса
        every { bluetoothService.startDataListening() } returns incomingDataFlow
        
        handler = DataStreamHandler(bluetoothService, testScope, stateCallback)
    }

    /**
     * Тест: Фаза 1 и 2 — Отправка настроек и ожидание подтверждения.
     */
    @Test
    fun `should transition to REQUESTING_DATA after settings are acknowledged`() = testScope.runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val settings = AppSettings()
        
        handler.start(device, settings)
        
        // Даем время на установку подписки и первую отправку
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        
        // Проверяем, что была попытка отправить настройки
        coVerify { bluetoothService.sendData(match { it.contains("settings") }) }
        verify { stateCallback(ConnectionState.SENDING_SETTINGS, null) }

        // Эмулируем получение подтверждения от устройства
        incomingDataFlow.emit("{\"settings\":\"OK\"}")
        
        // Ждем интервал отправки (1500мс)
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()

        // Должен перейти к следующей фазе
        verify { stateCallback(ConnectionState.REQUESTING_DATA, null) }
    }

    /**
     * Тест: Фаза 5 — Парсинг данных CarData.
     */
    @Test
    fun `should emit CarData when receiving valid data JSON`() = testScope.runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Пропускаем фазы 1-4 (подтверждаем всё сразу)
        coEvery { bluetoothService.sendData(any()) } returns true
        
        handler.start(device, AppSettings())
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        
        // Подтверждаем настройки
        incomingDataFlow.emit("{\"settings\":\"OK\"}")
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()
        
        // Подтверждаем команду
        incomingDataFlow.emit("{\"GET_DATA\":\"OK\"}")
        testScheduler.advanceTimeBy(1600)
        testScheduler.runCurrent()

        // Проверяем эмиссию данных
        handler.carDataFlow.test {
            // Эмулируем приход данных от БК
            val jsonResponse = """{"data":{"speed":85.5,"voltage":13.8,"fuel":42.0}}"""
            incomingDataFlow.emit(jsonResponse)
            
            val result = awaitItem()
            assert(result.speed == 85.5f)
            assert(result.voltage == 13.8f)
            assert(result.fuel == 42.0f)
        }
    }

    /**
     * Тест: Таймаут протокола.
     * Если подтверждение не пришло за 10 секунд, должна быть ошибка.
     */
    @Test
    fun `should return ERROR if settings are not acknowledged within timeout`() = testScope.runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        handler.start(device, AppSettings())
        
        // Проматываем 11 секунд (больше таймаута в 10 сек)
        testScheduler.advanceTimeBy(11000)
        testScheduler.runCurrent()

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("Таймаут") }) }
    }
}
