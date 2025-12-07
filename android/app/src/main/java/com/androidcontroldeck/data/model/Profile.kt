package com.androidcontroldeck.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val rows: Int,
    val cols: Int,
    val controls: List<Control> = emptyList(),
    val version: Int = 1,
    val checksum: String? = null
)
