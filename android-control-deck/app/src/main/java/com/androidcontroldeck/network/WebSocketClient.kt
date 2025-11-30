package com.androidcontroldeck.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

sealed class ConnectionState {
    data class Connected(val url: String) : ConnectionState()
    data class Connecting(val url: String) : ConnectionState()
    data class Disconnected(val reason: String? = null) : ConnectionState()
}

class WebSocketClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var lastUrl: String? = null
    private var lastHeaders: Map<String, String> = emptyMap()

    fun connect(url: String, headers: Map<String, String> = emptyMap()) {
        lastUrl = url
        lastHeaders = headers
        _state.value = ConnectionState.Connecting(url)
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        val request = requestBuilder.build()
        webSocket?.cancel()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect(reason: String? = null) {
        heartbeatJob?.cancel()
        webSocket?.close(1000, reason)
        webSocket = null
        _state.value = ConnectionState.Disconnected(reason)
    }

    fun send(payload: String) {
        if (webSocket == null && lastUrl != null) {
            reconnect()
        }
        if (_state.value is ConnectionState.Connected) {
            webSocket?.send(payload)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(15_000)
                if (_state.value is ConnectionState.Connected) {
                    webSocket?.send("\uD83D\uDCBE")
                }
            }
        }
    }

    private fun reconnect() {
        val targetUrl = lastUrl ?: return
        val delayMs = (1 shl reconnectAttempts).coerceAtMost(30) * 1_000L
        scope.launch {
            delay(delayMs)
            reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(6)
            connect(targetUrl, lastHeaders)
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _state.value = ConnectionState.Connected(response.request.url.toString())
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocketClient", "message: $text")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("WebSocketClient", "message bytes: ${bytes.utf8()}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnectionState.Disconnected("closing: $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnectionState.Disconnected("closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnectionState.Disconnected(t.message)
            reconnect()
        }
    }
}
