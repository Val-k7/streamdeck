package com.androidcontroldeck.network

import android.os.SystemClock
import com.androidcontroldeck.network.model.ControlPayload
import com.androidcontroldeck.logging.UnifiedLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ControlEventSender(
    private val webSocketClient: WebSocketClient,
    private val logger: UnifiedLogger? = null,
    private val json: Json = Json { encodeDefaults = true }
) {
    fun send(controlId: String, type: String, value: Float, meta: Map<String, String> = emptyMap()) {
        val id = UUID.randomUUID().toString()
        val sentAt = SystemClock.elapsedRealtime()
        val payload = ControlPayload(
            controlId = controlId,
            type = type,
            value = value,
            meta = meta,
            messageId = id,
            sentAt = sentAt,
        )
        val body = json.encodeToString(payload)
        logger?.logDebug(
            "Sending control event",
            mapOf("controlId" to controlId, "type" to type, "value" to value, "messageId" to id)
        )
        webSocketClient.send(body, messageId = id)
    }
}
