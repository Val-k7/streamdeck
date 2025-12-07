package com.androidcontroldeck.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ControlType {
    BUTTON,
    TOGGLE,
    FADER,
    KNOB,
    PAD
}

@Serializable
data class Control(
    val id: String,
    val type: ControlType,
    val row: Int,
    val col: Int,
    @SerialName("rowSpan") val rowSpan: Int = 1,
    @SerialName("colSpan") val colSpan: Int = 1,
    val label: String = "",
    val colorHex: String? = null,
    val icon: String? = null,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val value: Float = minValue,
    val action: Action = Action()
)
