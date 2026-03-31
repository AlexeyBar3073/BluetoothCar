// Файл: data/bluetooth/listeners/ConnectionStateManagerTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ТЕГ: Bluetooth/StateManager/Test
 *
 * ФАЙЛ: data/bluetooth/listeners/ConnectionStateManagerTest.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модульное тестирование компонента ConnectionStateManager.
 * Проверяет логику установки физического Bluetooth-соединения (RFC/Socket).
 * Тестирует механизм автоматических повторных попыток (Retries) при сбоях и мониторинг разрыва связи.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка перехода в состояние CONNECTED при успешном сопряжении.
 * 2. Тестирование логики 5-кратных повторных попыток с задержкой при ошибках подключения.
 * 3. Проверка срабатывания состояния DISCONNECTED при внешнем разрыве связи (через монитор).
 * 4. Предотвращение одновременного запуска нескольких процессов подключения.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Unit Testing с использованием MockK.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Имитация асинхронного процесса установки соединения с использованием корутин и управление
 * виртуальным временем для проверки экспоненциальных/фиксированных задержек между ретраями.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Тестирует: ConnectionStateManager.kt.
 * Использует: AppBluetoothService, ConnectionState.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionStateManagerTest {

    /** Правило для подмены Main диспатчера */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Мок сервиса Bluetooth */
    private lateinit var bluetoothService: AppBluetoothService
    
    /** Тестируемый экземпляр менеджера состояний */
    private lateinit var stateManager: ConnectionStateManager
    
    /** Мок колбэка для отслеживания изменений состояния */
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    /** Тестовый диспатчер для мгновенного выполнения корутин */
    private val testDispatcher = UnconfinedTestDispatcher()

    /**
     * Инициализация перед каждым тестом.
     */
    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        // Инициализируем менеджер с тестовым диспатчером для синхронизации корутин
        stateManager = ConnectionStateManager(bluetoothService, stateCallback, testDispatcher)
    }

    /**
     * Тест: должен успешно подключиться и запустить мониторинг.
     */
    @Test
    fun `should return CONNECTED on successful connection and start monitoring`() = runTest(testDispatcher) {
        // Подготовка
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Настраиваем имитацию успешного подключения
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = eq(device),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } answers {
            // Вызываем колбэк успешного соединения
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }

        // Действие: запускаем процесс подключения
        stateManager.start(device)
        runCurrent()

        // Проверка: последовательность состояний и запуск мониторинга
        verify { stateCallback(ConnectionState.CONNECTING, null) }
        verify { stateCallback(ConnectionState.CONNECTED, null) }
        verify { bluetoothService.startConnectionMonitoring(eq(device), any()) }
    }

    /**
     * Тест: должен выдать ошибку после 5 неудачных попыток подключения.
     */
    @Test
    fun `should return ERROR after 5 failed attempts`() = runTest(testDispatcher) {
        // Подготовка
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Имитируем постоянный отказ в соединении
        coEvery { 
            bluetoothService.connectToDevice(any(), any(), any(), any()) 
        } returns Pair(false, "Connection refused")

        // Действие
        stateManager.start(device)
        
        // Проматываем время: 5 попыток по 2000мс задержки между ними
        advanceTimeBy(15000) 
        runCurrent()

        // Проверка: было ровно 5 попыток вызова сервиса
        coVerify(exactly = 5) { 
            bluetoothService.connectToDevice(any(), any(), any(), any()) 
        }
        
        // Проверка финального состояния ошибки
        verify { stateCallback(ConnectionState.ERROR, match { it.contains("после 5 попыток") }) }
    }

    /**
     * Тест: должен среагировать на разрыв связи, зафиксированный монитором.
     */
    @Test
    fun `should return DISCONNECTED when active connection is lost`() = runTest(testDispatcher) {
        // Подготовка
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val monitorCallbackSlot = slot<(Boolean, BluetoothDeviceData?) -> Unit>()

        // Успешное подключение в начале
        coEvery { 
            bluetoothService.connectToDevice(any(), any(), any(), any()) 
        } answers {
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }
        
        // Захватываем колбэк монитора
        every { 
            bluetoothService.startConnectionMonitoring(eq(device), capture(monitorCallbackSlot)) 
        } just runs

        stateManager.start(device)
        runCurrent()

        // Имитируем разрыв связи через колбэк монитора
        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(false, device)
            runCurrent()
            
            // Проверка: состояние изменилось на DISCONNECTED
            verify { stateCallback(ConnectionState.DISCONNECTED, null) }
        }
    }

    /**
     * Тест: предотвращение повторного запуска при уже активном процессе.
     */
    @Test
    fun `should return ERROR if start is called while already attempting connection`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Заставляем первую попытку "длиться" долго
        coEvery { 
            bluetoothService.connectToDevice(any(), any(), any(), any())
        } coAnswers { 
            kotlinx.coroutines.delay(5000)
            Pair(true, null)
        }

        // Запускаем первую попытку
        stateManager.start(device)
        
        // Пытаемся запустить вторую попытку параллельно
        stateManager.start(device)

        // Проверка: CSM должен выдать ошибку о занятости
        verify { stateCallback(ConnectionState.ERROR, match { it.contains("уже выполняется") }) }
    }
}
