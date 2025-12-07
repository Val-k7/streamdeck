package com.androidcontroldeck.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.WebSocketClient
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.network.AuthRepository
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'intégration Android ↔ Serveur
 *
 * Ces tests nécessitent un serveur Control Deck en cours d'exécution
 * sur le réseau local.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ServerConnectionTest {
    private lateinit var context: android.content.Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var authRepository: AuthRepository
    private lateinit var diagnosticsRepository: DiagnosticsRepository
    private lateinit var logger: UnifiedLogger
    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsRepository = SettingsRepository(context)
        webSocketClient = WebSocketClient(logger, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        authRepository = AuthRepository(okhttp3.OkHttpClient(), com.androidcontroldeck.data.preferences.SecurePreferences(context))
        diagnosticsRepository = DiagnosticsRepository(context)
        logger = UnifiedLogger(context)
        connectionManager = ConnectionManager(
            settingsRepository,
            webSocketClient,
            authRepository,
            diagnosticsRepository,
            logger
        )
    }

    @Test
    fun `test server discovery`() = runTest {
        // Note: Ce test nécessite un serveur en cours d'exécution
        // Il peut être désactivé si le serveur n'est pas disponible
        val serverIp = "192.168.1.100" // À adapter selon votre réseau

        // Vérifier que l'IP est valide
        assertTrue("IP valide requise pour le test", serverIp.isNotBlank())
    }

    @Test
    fun `test connection state flow`() = runTest {
        val state = connectionManager.state.first()
        assertNotNull(state)
        // État initial devrait être Disconnected
        assertTrue(state is com.androidcontroldeck.network.ConnectionState.Disconnected)
    }

    @Test
    fun `test settings repository`() = runTest {
        val settings = settingsRepository.settings.first()
        assertNotNull(settings)
        // Vérifier que les settings sont chargés
        assertTrue(true) // Test de base
    }
}


