// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/data/bluetooth/ConnectionStateTest.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ТЕГ: Тесты состояний подключения
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модульные тесты для перечисления ConnectionState и структуры ConnectionStatusInfo.
 * Проверяют корректность логических проверок состояний и правильность маппинга данных для UI.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Тестирует: ConnectionState.kt
 * 2. Используется для: Гарантии корректного отображения статусов в UI виджетах.
 */
class ConnectionStateTest {

    /**
     * Тест: Проверка метода isActiveState().
     * Активными считаются состояния, где соединение установлено и идет (или готово к) обмену данными.
     */
    @Test
    fun `isActiveState should return true only for active states`() {
        // Активные состояния
        assertTrue("CONNECTED должен быть активным", ConnectionState.CONNECTED.isActiveState())
        assertTrue("LISTENING_DATA должен быть активным", ConnectionState.LISTENING_DATA.isActiveState())
        assertTrue("SENDING_SETTINGS должен быть активным", ConnectionState.SENDING_SETTINGS.isActiveState())
        assertTrue("REQUESTING_DATA должен быть активным", ConnectionState.REQUESTING_DATA.isActiveState())
        
        // Неактивные состояния
        assertFalse("CONNECTING не является активным (еще не подключено)", ConnectionState.CONNECTING.isActiveState())
        assertFalse("DISCONNECTED не является активным", ConnectionState.DISCONNECTED.isActiveState())
        assertFalse("ERROR не является активным", ConnectionState.ERROR.isActiveState())
        assertFalse("UNDEFINED не является активным", ConnectionState.UNDEFINED.isActiveState())
    }

    /**
     * Тест: Проверка конвертации в полную структуру для UI.
     * Проверяет, что метод toStatusInfo() корректно переносит базовые свойства.
     */
    @Test
    fun `toStatusInfo should map all properties correctly`() {
        val state = ConnectionState.LISTENING_DATA
        val info = state.toStatusInfo()
        
        assertEquals("State enum должен совпадать", state, info.state)
        assertEquals("Отображаемое имя должно быть корректным", "Соединение установлено", info.displayName)
        assertTrue("LISTENING_DATA должен иметь флаг isActive=true", info.isActive)
        assertFalse("LISTENING_DATA не является ошибкой", info.isError)
        assertFalse("LISTENING_DATA не является процессом подключения", info.isConnecting)
    }

    /**
     * Тест: Проверка идентификации терминальных состояний.
     * Терминальные состояния требуют вмешательства или перезапуска процесса.
     */
    @Test
    fun `terminal states should be identified correctly`() {
        assertTrue("ERROR - терминальное состояние", ConnectionState.ERROR.isTerminalState())
        assertTrue("DISCONNECTED - терминальное состояние", ConnectionState.DISCONNECTED.isTerminalState())
        assertTrue("DEVICE_UNAVAILABLE - терминальное состояние", ConnectionState.DEVICE_UNAVAILABLE.isTerminalState())
        
        assertFalse("CONNECTED не терминальное", ConnectionState.CONNECTED.isTerminalState())
        assertFalse("CONNECTING не терминальное", ConnectionState.CONNECTING.isTerminalState())
    }

    /**
     * Тест: Проверка флага возможности ручного переподключения.
     * Согласно ТЗ, только определенные состояния (ошибки) позволяют ручной повтор.
     */
    @Test
    fun `manual retry should be allowed only for specific states`() {
        assertTrue("ERROR должен позволять ручной повтор", ConnectionState.ERROR.allowsManualRetry)
        assertTrue("DEVICE_UNAVAILABLE должен позволять ручной повтор", ConnectionState.DEVICE_UNAVAILABLE.allowsManualRetry)
        
        assertFalse("CONNECTING не должен позволять ручной повтор", ConnectionState.CONNECTING.allowsManualRetry)
        assertFalse("LISTENING_DATA не должен позволять ручной повтор", ConnectionState.LISTENING_DATA.allowsManualRetry)
    }
}
