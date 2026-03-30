// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/DataStreamHandlerTest.kt
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
 * ТЕГ: Тесты DataStreamHandler
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Тестирование транспортного шлюза. Проверяется только механизм доставки сообщений,
 * присвоение ID и трансляция входящего потока.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStreamHandlerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var handler: DataStreamHandler
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    private val incomingRawDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        
        // Настройка потока данных от Bluetooth сервиса
        every { bluetoothService.startDataListening() } returns incomingRawDataFlow
        coEvery { bluetoothService.sendData(any()) } returns true
    }

    /**
     * Тест: Присвоение msg_id и отправка.
     */
    @Test
    fun `should wrap command in envelope with msg_id and send via service`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        val testCommand = "{\"command\":\"TEST\"}"
        handler.sendJsonCommand(testCommand)
        runCurrent()
        
        // Проверяем, что в сервис ушел JSON с msg_id="1"
        coVerify { bluetoothService.sendData(match { it.contains("\"msg_id\":\"1\"") && it.contains("\"command\":\"TEST\"") }) }
    }

    /**
     * Тест: Гарантированная доставка (переотправка).
     * Если ack_id не получен, сообщение должно отправляться повторно через интервал.
     */
    @Test
    fun `should retry sending command until ack_id is received`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        handler.sendJsonCommand("{\"cmd\":\"RETRY_TEST\"}")
        runCurrent()

        // Первая попытка отправки
        coVerify(exactly = 1) { bluetoothService.sendData(any()) }

        // Эмулируем прохождение времени (интервал в DSH = 1500мс)
        testScheduler.advanceTimeBy(1600)
        runCurrent()

        // Должна быть вторая попытка отправки той же команды
        coVerify(exactly = 2) { bluetoothService.sendData(any()) }

        // Эмулируем получение подтверждения
        incomingRawDataFlow.emit("{\"ack_id\":\"1\"}")
        runCurrent()

        // Еще раз перематываем время - больше отправок быть не должно
        testScheduler.advanceTimeBy(3000)
        runCurrent()

        coVerify(exactly = 2) { bluetoothService.sendData(any()) }
    }

    /**
     * Тест: Трансляция входящих данных.
     * Проверяет, что все пакеты пробрасываются в incomingMessagesFlow как JsonObject.
     */
    @Test
    fun `should emit all received messages as JsonObjects to incomingMessagesFlow`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()

        handler.incomingMessagesFlow.test {
            val dataPacket = "{\"data\":{\"val\":100}}"
            val ackPacket = "{\"ack_id\":\"5\"}"

            // 1. Проверка обычного пакета
            incomingRawDataFlow.emit(dataPacket)
            val item1 = awaitItem()
            assertEquals(100, item1["data"]?.jsonObject?.get("val")?.jsonPrimitive?.int)

            // 2. Проверка пакета с ack_id (теперь тоже пробрасывается)
            incomingRawDataFlow.emit(ackPacket)
            val item2 = awaitItem()
            assertEquals("5", item2["ack_id"]?.jsonPrimitive?.content)
        }
    }

    /**
     * Тест: Остановка обработчика.
     */
    @Test
    fun `should stop listening and sending after stop call`() = runTest(testDispatcher) {
        handler = DataStreamHandler(bluetoothService, backgroundScope, stateCallback, testDispatcher)
        handler.start()
        
        handler.stop()
        runCurrent()

        verify { bluetoothService.stopDataListening() }
        
        handler.sendJsonCommand("{\"cmd\":\"SHUTDOWN\"}")
        runCurrent()
        
        // После остановки команды не должны уходить в сервис
        coVerify(exactly = 0) { bluetoothService.sendData(any()) }
    }
}
