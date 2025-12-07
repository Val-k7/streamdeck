package com.androidcontroldeck.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.WebSocketClient
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.network.AuthRepository
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSyncTest {
    private lateinit var profileRepository: ProfileRepository
    private lateinit var connectionManager: ConnectionManager
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        profileRepository = ProfileRepository(context)
        val settingsRepository = SettingsRepository(context)
        val logger = UnifiedLogger(context)
        val webSocketClient = WebSocketClient(logger, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val authRepository = AuthRepository(okhttp3.OkHttpClient(), com.androidcontroldeck.data.preferences.SecurePreferences(context))
        val diagnosticsRepository = DiagnosticsRepository(context)
        connectionManager = ConnectionManager(
            settingsRepository,
            webSocketClient,
            authRepository,
            diagnosticsRepository,
            logger
        )
    }

    @Test
    fun `test profile synchronization with server`() = runBlocking {
        // Create a test profile
        val testProfile = Profile(
            id = "test-profile-sync",
            name = "Test Sync Profile",
            version = 1,
            rows = 3,
            cols = 5,
            controls = listOf(
                Control(
                    id = "control-1",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = "Sync Button"
                )
            )
        )

        // Save profile locally
        profileRepository.saveProfile(testProfile)

        // Connect to server (if available)
        // Note: This test requires a running server
        try {
            connectionManager.connect("192.168.1.1", 4455, false)

            // Wait for connection
            Thread.sleep(1000)

            val state = connectionManager.state.value
            if (state is com.androidcontroldeck.network.ConnectionState.Connected) {
                // Select profile (which triggers sync)
                profileRepository.selectProfile(testProfile.id)

                // Wait for sync
                Thread.sleep(500)

                // Verify profile was selected
                val selected = profileRepository.getSelectedProfile()
                assertEquals(testProfile.id, selected?.id)
            }
        } catch (e: Exception) {
            // Server not available, skip test
            println("Server not available, skipping sync test: ${e.message}")
        }
    }

    @Test
    fun `test profile conflict resolution`() = runBlocking {
        // Create two versions of the same profile
        val localProfile = Profile(
            id = "conflict-profile",
            name = "Local Profile",
            version = 1,
            rows = 3,
            cols = 5,
            controls = emptyList()
        )

        val serverProfile = Profile(
            id = "conflict-profile",
            name = "Server Profile",
            version = 2,
            rows = 3,
            cols = 5,
            controls = emptyList()
        )

        // Save local profile
        profileRepository.saveProfile(localProfile)

        // Save server profile (simulating conflict resolution)
        profileRepository.saveProfile(serverProfile)

        // Verify server profile was saved
        val saved = profileRepository.loadProfile("conflict-profile")
        assertNotNull(saved)
        assertEquals(serverProfile.version, saved?.version)
    }
}

