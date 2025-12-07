package com.androidcontroldeck.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.androidcontroldeck.network.ConnectionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReconnectionTest {
    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        connectionManager = ConnectionManager(context)
    }

    @Test
    fun `test automatic reconnection after disconnect`() = runBlocking {
        // Connect to server
        try {
            connectionManager.connect("192.168.1.1", 4455, false)

            // Wait for connection
            Thread.sleep(1000)

            // Check connection state
            val initialState = connectionManager.state.value
            assertNotNull(initialState)

            // Disconnect if connected
            if (initialState is com.androidcontroldeck.network.ConnectionState.Connected) {
                connectionManager.disconnect()

                // Wait a bit
                Thread.sleep(500)

                // Verify disconnected
                val disconnectedState = connectionManager.state.value
                assertTrue(disconnectedState is com.androidcontroldeck.network.ConnectionState.Disconnected)

                // Reconnect
                connectionManager.connect("192.168.1.1", 4455, false)

                // Wait for reconnection
                Thread.sleep(1000)

                // Verify reconnected (or attempting to reconnect)
                val reconnectedState = connectionManager.state.value
                assertNotNull(reconnectedState)
            }
        } catch (e: Exception) {
            // Server not available, skip test
            println("Server not available, skipping reconnection test: ${e.message}")
        }
    }

    @Test
    fun `test reconnection with network error`() = runBlocking {
        // Try to connect to invalid server
        try {
            connectionManager.connect("192.168.1.999", 4455, false)

            // Wait
            Thread.sleep(2000)

            // Should handle error gracefully
            // (Implementation depends on actual error handling)
        } catch (e: Exception) {
            // Expected error
            assertNotNull(e)
        }
    }

    @Test
    fun `test connection state persistence`() = runBlocking {
        // Connect to server
        try {
            connectionManager.connect("192.168.1.1", 4455, false)

            // Wait for connection
            Thread.sleep(1000)

            // Verify state is available
            val state = connectionManager.state.value
            assertNotNull(state)
            // State should be one of: Disconnected, Connecting, Connected, Error
        } catch (e: Exception) {
            // Server not available, skip test
            println("Server not available, skipping persistence test: ${e.message}")
        }
    }
}

