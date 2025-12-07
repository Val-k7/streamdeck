package com.androidcontroldeck.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    KEYBOARD,
    OBS,
    SCRIPT,
    AUDIO,
    CUSTOM
}

@Serializable
data class Action(
    val type: ActionType = ActionType.CUSTOM,
    val payload: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
