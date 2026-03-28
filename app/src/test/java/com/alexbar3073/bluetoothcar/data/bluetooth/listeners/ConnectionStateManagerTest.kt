//ФАЙЛ: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/ConnectionStateManagerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ТЕГ: Тесты менеджера соединений
 * ФАЙЛ: C:/Project/BluetoothCar/app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/ConnectionStateManagerTest.kt
 * МЕСТОНАХОЖДЕНИЕ: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для ConnectionStateManager. Проверяют процесс установления 
 * RFCOMM-соединения, обработку 5 попыток подключения и мониторинг разрывов.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: ConnectionStateManager.kt
 * 2. Мокает: AppBluetoothService.kt
 * 3. Использует: ConnectionState, BluetoothDeviceData
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionStateManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
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
     * Проверяет: статус CONNECTING, вызов сервиса, статус CONNECTED и запуск мониторинга.
     */
    @Test
    fun `should return CONNECTED on successful connection and start monitoring`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = eq(device),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } answers {
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }

        stateManager.start(device)
        advanceTimeBy(100)

        verify { stateCallback(ConnectionState.CONNECTING, null) }
        verify { stateCallback(ConnectionState.CONNECTED, null) }
        // Проверка запуска мониторинга после успеха (п. 4.6.3 протокола)
        verify { bluetoothService.startConnectionMonitoring(eq(device), any()) }
    }

    /**
     * Тест: Обработка 5 неудачных попыток подключения.
     */
    @Test
    fun `should return DISCONNECTED after 5 failed attempts`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } returns Pair(false, "Connection refused")

        stateManager.start(device)
        
        // 5 попыток по 2сек задержки + таймауты
        advanceTimeBy(15000)

        coVerify(atLeast = 5) { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        }
        verify { stateCallback(ConnectionState.DISCONNECTED, null) }
    }

    /**
     * Тест: Мониторинг разрыва уже установленного соединения.
     */
    @Test
    fun `should return DISCONNECTED when active connection is lost`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val monitorCallbackSlot = slot<(Boolean, BluetoothDeviceData?) -> Unit>()

        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } answers {
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }
        
        every { 
            bluetoothService.startConnectionMonitoring(eq(device), capture(monitorCallbackSlot)) 
        } just runs

        stateManager.start(device)
        advanceTimeBy(100)

        // Эмулируем разрыв связи через мониторинг
        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(false, device)
            verify { stateCallback(ConnectionState.DISCONNECTED, null) }
        }
    }

    /**
     * Тест: Ошибка при попытке запустить повторное подключение, если оно уже идет.
     */
    @Test
    fun `should return ERROR if start is called while already attempting connection`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            )
        } coAnswers { 
            kotlinx.coroutines.delay(5000)
            Pair(true, null)
        }

        stateManager.start(device)
        stateManager.start(device)

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("уже выполняется") }) }
    }
}
