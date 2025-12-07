package com.androidcontroldeck.data.repository

import kotlinx.serialization.Serializable

/**
 * État de synchronisation d'un profil avec le serveur
 */
@Serializable
data class ProfileSyncState(
    val profileId: String,
    val status: SyncStatus,
    val lastSyncAttempt: Long? = null,
    val lastSyncSuccess: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
) {
    enum class SyncStatus {
        IDLE,           // Aucune synchronisation en cours
        SYNCING,        // Synchronisation en cours
        SUCCESS,        // Dernière synchronisation réussie
        ERROR,          // Dernière synchronisation échouée
        RETRYING        // Nouvelle tentative en cours
    }
}



