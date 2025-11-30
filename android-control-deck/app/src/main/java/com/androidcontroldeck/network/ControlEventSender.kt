package com.androidcontroldeck.network

import com.androidcontroldeck.network.model.ControlPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ControlEventSender(
    private val webSocketClient: WebSocketClient,
    private val json: Json = Json { encodeDefaults = true }
) {
    fun send(controlId: String, type: String, value: Float, meta: Map<String, String> = emptyMap()) {
        val payload = ControlPayload(controlId = controlId, type = type, value = value, meta = meta)
        val body = json.encodeToString(payload)
        webSocketClient.send(body)
    }
}
