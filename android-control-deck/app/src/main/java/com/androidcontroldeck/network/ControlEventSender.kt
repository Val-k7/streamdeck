package com.androidcontroldeck.network

import android.os.SystemClock
import com.androidcontroldeck.data.model.PendingAction
import com.androidcontroldeck.data.storage.PendingActionStorage
import com.androidcontroldeck.network.model.ControlPayload
import com.androidcontroldeck.network.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ControlEventSender(
    private val webSocketClient: WebSocketClient,
    private val storage: PendingActionStorage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
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
        enqueue(PendingAction(payload = payload))
        flushPending()
    }

    fun flushPending() {
        val current = _pendingActions.value
        current.forEach { pending ->
            val body = json.encodeToString(pending.payload)
            webSocketClient.send(body, messageId = pending.payload.messageId)
        }
    }

    fun purgePending() {
        _pendingActions.value = emptyList()
        storage.clear()
    }

    private fun enqueue(action: PendingAction) {
        _pendingActions.update { current ->
            val updated = current + action
            storage.save(updated)
            updated
        }
    }
}
