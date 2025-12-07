package com.androidcontroldeck.network

import android.os.SystemClock
import com.androidcontroldeck.data.model.PendingAction
import com.androidcontroldeck.data.storage.PendingActionStorage
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.ActionFeedback
import com.androidcontroldeck.network.model.ControlPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    private val logger: UnifiedLogger? = null,
    private val storage: PendingActionStorage,
    private val scope: CoroutineScope,
    private val json: Json = Json { encodeDefaults = true }
) {
    private val _pendingActions = MutableStateFlow<List<PendingAction>>(emptyList())
    val pendingActions: StateFlow<List<PendingAction>> = _pendingActions.asStateFlow()

    // Map de controlId -> ActionFeedback pour le feedback visuel
    private val _actionFeedback = MutableStateFlow<Map<String, ActionFeedback>>(emptyMap())
    val actionFeedback: StateFlow<Map<String, ActionFeedback>> = _actionFeedback.asStateFlow()

    // Map de messageId -> controlId pour mapper les ACK aux contrôles
    private val messageIdToControlId = mutableMapOf<String, String>()

    init {
        scope.launch { _pendingActions.emit(storage.load()) }

        scope.launch {
            webSocketClient.state.collect { state ->
                if (state is ConnectionState.Connected) flushPending()
            }
        }

        scope.launch {
            webSocketClient.ackEvents.collect { ack ->
                // Mettre à jour les actions en attente
                _pendingActions.update { current ->
                    val removed = current.filterNot { it.payload.messageId == ack.messageId }
                    if (removed.size != current.size) storage.save(removed)
                    removed
                }

                // Mettre à jour le feedback visuel
                val controlId = ack.controlId ?: messageIdToControlId.remove(ack.messageId)
                if (controlId != null) {
                    val status = when (ack.status) {
                        "ok" -> ActionFeedback.ActionStatus.SUCCESS
                        "error" -> ActionFeedback.ActionStatus.ERROR
                        else -> ActionFeedback.ActionStatus.PENDING
                    }
                    val feedback = ActionFeedback(
                        controlId = controlId,
                        status = status,
                        errorMessage = ack.error,
                        timestamp = System.currentTimeMillis()
                    )
                    _actionFeedback.update { current ->
                        current + (controlId to feedback)
                    }

                    // Effacer le feedback après 3 secondes pour les succès, 5 secondes pour les erreurs
                    val clearDelay = if (status == ActionFeedback.ActionStatus.SUCCESS) 3000L else 5000L
                    scope.launch {
                        delay(clearDelay)
                        _actionFeedback.update { current ->
                            val updated = current.toMutableMap()
                            // Ne supprimer que si c'est le même feedback (pas un nouveau)
                            current[controlId]?.let { existing ->
                                if (existing.timestamp == feedback.timestamp) {
                                    updated.remove(controlId)
                                }
                            }
                            updated
                        }
                    }
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

        // Mapper messageId -> controlId pour le feedback
        messageIdToControlId[id] = controlId

        // Marquer comme PENDING
        val feedback = ActionFeedback(
            controlId = controlId,
            status = ActionFeedback.ActionStatus.PENDING,
            timestamp = System.currentTimeMillis()
        )
        _actionFeedback.update { current ->
            current + (controlId to feedback)
        }

        webSocketClient.send(body, messageId = id)
    }

    fun flushPending() {
        val actions = _pendingActions.value
        if (actions.isEmpty()) return

        logger?.logDebug("Flushing ${'$'}{actions.size} pending actions")
        actions.forEach { action ->
            val body = json.encodeToString(action.payload)
            webSocketClient.send(body, messageId = action.payload.messageId)
        }
    }

    fun purgePending() {
        logger?.logDebug("Purging all pending actions")
        _pendingActions.update {
            storage.save(emptyList())
            emptyList()
        }
    }
}
