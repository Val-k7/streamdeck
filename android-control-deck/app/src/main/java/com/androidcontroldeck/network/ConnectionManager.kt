package com.androidcontroldeck.network

import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectionManager(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
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

    fun connect(token: String? = null) {
        val state = settings.value
        val url = "ws://${state.serverIp}:${state.serverPort}/ws"
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        webSocketClient.connect(url, headers)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }
}
