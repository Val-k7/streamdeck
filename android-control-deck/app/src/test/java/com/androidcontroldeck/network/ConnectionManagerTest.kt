package com.androidcontroldeck.network

import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionManagerTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val socketState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    private val settingsFlow = MutableStateFlow(SettingsState(serverIp = "10.0.0.2", serverPort = 7788))

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var webSocketClient: WebSocketClient

    @Before
    fun setUp() {
        settingsRepository = mockk(relaxed = true)
        webSocketClient = mockk(relaxed = true)
        every { settingsRepository.settings } returns settingsFlow
        every { webSocketClient.state } returns socketState
    }

    @Test
    fun `connect builds url and forwards token header`() {
        val manager = ConnectionManager(settingsRepository, webSocketClient, scope)
        advanceUntilIdle()

        manager.connect(token = "secret")

        verify {
            webSocketClient.connect(
                url = "ws://10.0.0.2:7788/ws",
                headers = mapOf("Authorization" to "Bearer secret")
            )
        }
    }

    @Test
    fun `connection state mirrors websocket state`() {
        val manager = ConnectionManager(settingsRepository, webSocketClient, scope)
        advanceUntilIdle()

        socketState.value = ConnectionState.Connected("ws://10.0.0.2:7788/ws")
        advanceUntilIdle()
        assertTrue(manager.isConnected.value)

        socketState.value = ConnectionState.Disconnected("closed")
        advanceUntilIdle()
        assertFalse(manager.isConnected.value)
    }
}
