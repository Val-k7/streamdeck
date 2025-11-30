package com.androidcontroldeck.data.model

import com.androidcontroldeck.network.model.ControlPayload
import kotlinx.serialization.Serializable

@Serializable
data class PendingAction(
    val payload: ControlPayload,
    val queuedAt: Long = System.currentTimeMillis()
)
