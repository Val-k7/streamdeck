package com.androidcontroldeck.network

import android.os.SystemClock
import com.androidcontroldeck.data.model.PendingAction
import com.androidcontroldeck.data.storage.PendingActionStorage
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
    private val _pendingActions = MutableStateFlow<List<PendingAction>>(emptyList())
    val pendingActions: StateFlow<List<PendingAction>> = _pendingActions.asStateFlow()

    init {
        scope.launch { _pendingActions.emit(storage.load()) }

        scope.launch {
            webSocketClient.state.collect { state ->
                if (state is ConnectionState.Connected) flushPending()
            }
        }

        scope.launch {
            webSocketClient.ackEvents.collect { ack ->
                _pendingActions.update { current ->
                    val removed = current.filterNot { it.payload.messageId == ack.messageId }
                    if (removed.size != current.size) storage.save(removed)
                    removed
                }
            }
        }
    }

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
