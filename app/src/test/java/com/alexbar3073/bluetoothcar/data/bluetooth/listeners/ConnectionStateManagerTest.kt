// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/ConnectionStateManagerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * ТЕГ: Тесты менеджера соединений
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для ConnectionStateManager. Проверяют процесс установления 
 * RFCOMM-соединения, обработку 5 попыток подключения и мониторинг разрывов.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: ConnectionStateManager.kt
 * 2. Мокает: BluetoothService.kt
 * 3. Использует: ConnectionState, BluetoothDeviceData
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionStateManagerTest {

    private lateinit var bluetoothService: BluetoothService
    private lateinit var stateManager: ConnectionStateManager
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        stateManager = ConnectionStateManager(bluetoothService, stateCallback)
    }

    /**
     * Тест: Успешное подключение с первой попытки.
     */
    @Test
    fun `should return CONNECTED on successful connection`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Мокаем успешный результат от сервиса
        coEvery { 
            bluetoothService.connectToDevice(device, any(), any(), any()) 
        } answers {
            // Вызываем колбек успешного подключения, который пришел в аргументах
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }

        stateManager.start(device)
        advanceTimeBy(100) // Даем корутине отработать

        verify { stateCallback(ConnectionState.CONNECTING, null) }
        verify { stateCallback(ConnectionState.CONNECTED, null) }
    }

    /**
     * Тест: Обработка 5 неудачных попыток подключения.
     */
    @Test
    fun `should return DISCONNECTED after 5 failed attempts`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Всегда возвращаем ошибку подключения
        coEvery { 
            bluetoothService.connectToDevice(device, any(), any(), any()) 
        } returns Pair(false, "Connection refused")

        stateManager.start(device)
        
        // Проматываем время: 5 попыток по 2сек задержки + таймауты
        advanceTimeBy(15000)

        // Проверяем, что сервис вызывался 5 раз
        coVerify(exactly = 5) { bluetoothService.connectToDevice(device, any(), any(), any()) }
        
        // Итоговый статус после провала всех попыток
        verify { stateCallback(ConnectionState.DISCONNECTED, null) }
    }

    /**
     * Тест: Мониторинг разрыва уже установленного соединения.
     */
    @Test
    fun `should return DISCONNECTED when active connection is lost`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val monitorCallbackSlot = slot<(Boolean, BluetoothDeviceData?) -> Unit>()

        // 1. Сначала успешно подключаемся
        coEvery { 
            bluetoothService.connectToDevice(device, any(), any(), any()) 
        } returns Pair(true, null)
        
        // Захватываем колбек мониторинга
        every { 
            bluetoothService.startConnectionMonitoring(device, capture(monitorCallbackSlot)) 
        } just runs

        stateManager.start(device)
        advanceTimeBy(100)

        // 2. Эмулируем системное событие разрыва связи
        monitorCallbackSlot.captured(false, device)

        verify { stateCallback(ConnectionState.DISCONNECTED, null) }
    }

    /**
     * Тест: Ошибка при попытке запустить повторное подключение, если оно уже идет.
     */
    @Test
    fun `should return ERROR if start is called while already attempting connection`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Зависаем на первой попытке (не возвращаем результат сразу)
        coEvery { 
            bluetoothService.connectToDevice(device, any(), any(), any()) 
        } coAnswers { 
            kotlinx.coroutines.delay(5000)
            Pair(true, null)
        }

        stateManager.start(device)
        stateManager.start(device) // Второй вызов сразу

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("уже выполняется") }) }
    }
}
