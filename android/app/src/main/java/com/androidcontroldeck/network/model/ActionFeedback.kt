package com.androidcontroldeck.network.model

import kotlinx.serialization.Serializable

/**
 * Représente le résultat d'une action pour un contrôle
 */
@Serializable
data class ActionFeedback(
    val controlId: String,
    val status: ActionStatus,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class ActionStatus {
        SUCCESS,
        ERROR,
        PENDING
    }
}



