package com.androidcontroldeck.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ControlPayload(
    @SerialName("controlId") val controlId: String,
    val type: String,
    val value: Float,
    val meta: Map<String, String> = emptyMap()
)
