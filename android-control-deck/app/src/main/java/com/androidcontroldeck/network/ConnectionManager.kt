package com.androidcontroldeck.network

import android.util.Log
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ConnectionManager(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val settings: StateFlow<SettingsState> = settingsRepository.settings
        .stateIn(scope, SharingStarted.Eagerly, SettingsState())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        scope.launch {
            webSocketClient.state.collect { state ->
                _isConnected.value = state is ConnectionState.Connected
            }
        }
    }

    suspend fun connect(handshakeSecret: String? = null) {
        val state = settings.value
        val scheme = if (state.useTls) "wss" else "ws"
        val httpScheme = if (state.useTls) "https" else "http"
        val certificatePinner = state.pinnedCertSha256?.takeIf { it.isNotBlank() }?.let { fingerprint ->
            CertificatePinner.Builder()
                .add(state.serverIp, "sha256/$fingerprint")
                .build()
        }

        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .apply { certificatePinner?.let { certificatePinner(it) } }
            .build()

        webSocketClient.updateClient(client)
        webSocketClient.updateConfig(WebSocketConfig(heartbeatIntervalMs = state.heartbeatIntervalMs.toLong()))

        val savedSecret = handshakeSecret?.takeIf { it.isNotBlank() }
            ?: authRepository.getHandshakeSecret()
        val token = authRepository.getToken() ?: savedSecret?.let {
            runCatching {
                authRepository.performHandshake(
                    "$httpScheme://${state.serverIp}:${state.serverPort}",
                    it,
                    certificatePinner
                )
            }.onFailure { error ->
                Log.e("ConnectionManager", "Handshake failed", error)
            }.getOrNull()
        }

        val url = "$scheme://${state.serverIp}:${state.serverPort}/ws"
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") }
            ?: run {
                Log.w("ConnectionManager", "No token available, skipping connection")
                return
            }
        webSocketClient.connect(url, headers)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }
}
