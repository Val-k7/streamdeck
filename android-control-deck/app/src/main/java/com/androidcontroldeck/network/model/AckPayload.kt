package com.androidcontroldeck.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AckPayload(
    @SerialName("type") val type: String = "ack",
    @SerialName("messageId") val messageId: String,
    @SerialName("controlId") val controlId: String? = null,
    @SerialName("status") val status: String,
    @SerialName("receivedAt") val receivedAt: Long? = null,
    @SerialName("processedAt") val processedAt: Long? = null,
    @SerialName("error") val error: String? = null,
)
