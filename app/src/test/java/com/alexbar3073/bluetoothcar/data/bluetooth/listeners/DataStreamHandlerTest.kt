// Файл: data/bluetooth/listeners/DataStreamHandlerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import app.cash.turbine.test
import kotlinx.serialization.json.*

/**
 * ТЕГ: Bluetooth/DataStream/Test
 *
 * ФАЙЛ: data/bluetooth/listeners/DataStreamHandlerTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование компонента DataStreamHandler.
 * Проверяет работу транспортного уровня обмена данными: отправку команд с гарантированной доставкой (ACK),
 * автоматическое присвоение ID сообщениям и трансляцию входящего потока данных в JSON-объекты.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка упаковки JSON-команд в транспортный конверт с инкрементальным msg_id.
 * 2. Тестирование логики повторных отправок (Retries) до получения подтверждения (ack_id).
 * 3. Верификация корректного парсинга и эмиссии входящих сообщений через Flow.
 * 4. Проверка остановки потоков данных при вызове stop().
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK и Turbine для тестирования Flow.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Проверка надежности протокола обмена данными на уровне "отправил - дождался подтверждения" 
 * в изолированной среде с управлением виртуальным временем.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: DataStreamHandler.kt.
 * Использует: AppBluetoothService, ConnectionState.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class DataStreamHandlerTest {

    /** Правило для подмены Main диспатчера */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Мок сервиса Bluetooth */
    private lateinit var bluetoothService: AppBluetoothService
    
    /** Тестируемый экземпляр обработчика потока данных */
    private lateinit var handler: DataStreamHandler
    
    /** Мок колбэка для отслеживания изменений состояния */
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    /** Поток для имитации входящих сырых данных от Bluetooth */
    private val incomingRawDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    
    /** Тестовый диспатчер для управления временем */
    private val testDispatcher = UnconfinedTestDispatcher()

    /**
     * Инициализация перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        
        // Настраиваем возвращаемый поток для имитации прослушивания данных
        every { bluetoothService.startDataListening() } returns incomingRawDataFlow
        coEvery { bluetoothService.sendData(any()) } returns true
    }

    /**
     * Тест: Проверяет, что команда оборачивается в JSON с ID и отправляется в сервис.
     */
    @Test
    fun `should wrap command in envelope with msg_id and send via service`() = runTest(testDispatcher) {
        // Инициализируем обработчик в контексте runTest
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        val testCommand = "{\"command\":\"TEST\"}"
        
        // Действие: отправляем команду
        handler.sendJsonCommand(testCommand)
        runCurrent()
        
        // Проверка: в сервис ушел JSON с msg_id=1 (число) и исходной командой
        coVerify { bluetoothService.sendData(match { it.contains("\"msg_id\":1") && it.contains("\"command\":\"TEST\"") }) }
    }

    /**
     * Тест: Гарантированная доставка. Проверяет переотправку при отсутствии ACK.
     */
    @Test
    fun `should retry sending command until ack_id is received`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        // Отправляем команду
        handler.sendJsonCommand("{\"cmd\":\"RETRY_TEST\"}")
        runCurrent()

        // Проверка первой попытки
        coVerify(exactly = 1) { bluetoothService.sendData(any()) }

        // Эмулируем прохождение времени (интервал в DSH составляет 1500мс)
        testScheduler.advanceTimeBy(1600)
        runCurrent()

        // Проверка: должна произойти вторая попытка отправки того же сообщения
        coVerify(exactly = 2) { bluetoothService.sendData(any()) }

        // Имитируем получение подтверждения (ACK) от устройства (как число или строку)
        incomingRawDataFlow.emit("{\"ack_id\":1}")
        runCurrent()

        // Перематываем время еще раз - переотправки должны прекратиться
        testScheduler.advanceTimeBy(3000)
        runCurrent()

        coVerify(exactly = 2) { bluetoothService.sendData(any()) }
    }

    /**
     * Тест: Трансляция входящих данных в Flow.
     */
    @Test
    fun `should emit all received messages as JsonObjects to incomingMessagesFlow`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()

        // Используем библиотеку Turbine для тестирования потока
        handler.incomingMessagesFlow.test {
            val dataPacket = "{\"data\":{\"val\":100}}"
            val ackPacket = "{\"ack_id\":\"5\"}"

            // Имитируем входящие данные
            incomingRawDataFlow.emit(dataPacket)
            val item1 = awaitItem()
            assertEquals(100, item1["data"]?.jsonObject?.get("val")?.jsonPrimitive?.int)

            // Пакеты с подтверждением (ack_id) также должны транслироваться
            incomingRawDataFlow.emit("{\"ack_id\":5}")
            val item2 = awaitItem()
            assertEquals(5L, item2["ack_id"]?.jsonPrimitive?.long)
        }
    }

    /**
     * Тест: Корректная остановка обработчика.
     */
    @Test
    fun `should stop listening and sending after stop call`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        // Действие: останавливаем шлюз
        handler.stop()
        runCurrent()

        // Проверка: сервис получил команду на прекращение прослушивания
        verify { bluetoothService.stopDataListening() }
        
        // Пытаемся отправить команду после остановки
        handler.sendJsonCommand("{\"cmd\":\"SHUTDOWN\"}")
        runCurrent()
        
        // Команда не должна уйти в сервис
        coVerify(exactly = 0) { bluetoothService.sendData(any()) }
    }
}
