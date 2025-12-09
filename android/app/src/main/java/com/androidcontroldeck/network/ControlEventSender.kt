package com.androidcontroldeck.network

import com.androidcontroldeck.data.model.PendingAction
import com.androidcontroldeck.data.storage.PendingActionStorage
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.ActionFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stub sender: keeps flows for UI feedback but does not send over WebSocket.
 */
class ControlEventSender(
    private val webSocketClient: WebSocketClient,
    private val logger: UnifiedLogger? = null,
    private val storage: PendingActionStorage,
    private val scope: CoroutineScope,
) {
    private val _pendingActions = MutableStateFlow<List<PendingAction>>(emptyList())
    val pendingActions: StateFlow<List<PendingAction>> = _pendingActions.asStateFlow()

    private val _actionFeedback = MutableStateFlow<Map<String, ActionFeedback>>(emptyMap())
    val actionFeedback: StateFlow<Map<String, ActionFeedback>> = _actionFeedback.asStateFlow()

    init {
        scope.launch { _pendingActions.emit(storage.load()) }
    }

    fun send(controlId: String, type: String, value: Float, meta: Map<String, String> = emptyMap()) {
        logger?.logDebug(
            "StubControlEventSender send",
            mapOf("controlId" to controlId, "type" to type, "value" to value, "meta" to meta.toString())
        )
        val feedback = ActionFeedback(
            controlId = controlId,
            status = ActionFeedback.ActionStatus.PENDING,
            timestamp = System.currentTimeMillis()
        )
        _actionFeedback.value = _actionFeedback.value + (controlId to feedback)
    }

    fun flushPending() {
        // no-op in stub
    }

    fun purgePending() {
        _pendingActions.value = emptyList()
        storage.save(emptyList())
    }
}
