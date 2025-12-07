package com.androidcontroldeck.data.sync

import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.OfflineCache
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.network.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestionnaire de synchronisation des profils avec le serveur
 */
class SyncManager(
    private val offlineCache: OfflineCache,
    private val profileStorage: ProfileStorage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingSyncs = MutableStateFlow<List<PendingSync>>(emptyList())
    val pendingSyncs: StateFlow<List<PendingSync>> = _pendingSyncs.asStateFlow()

    /**
     * Synchronise les profils avec le serveur
     */
    suspend fun syncProfiles(
        connectionState: ConnectionState,
        serverProfiles: List<Profile>? = null
    ) {
        if (connectionState !is ConnectionState.Connected) {
            // Mode hors ligne : utiliser le cache
            _syncState.value = SyncState.Offline
            return
        }

        _syncState.value = SyncState.Syncing

        try {
            // 1. Sauvegarder les profils locaux dans le cache
            val localProfiles = profileStorage.loadProfiles()
            offlineCache.cacheAllProfiles(localProfiles)

            // 2. Si des profils serveur sont fournis, les fusionner
            if (serverProfiles != null) {
                // TODO: Implémenter la logique de fusion intelligente
                // Pour l'instant, on garde les profils locaux
            }

            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Ajoute une synchronisation en attente
     */
    fun addPendingSync(profile: Profile, action: SyncAction) {
        scope.launch {
            val pending = PendingSync(
                profileId = profile.id,
                profile = profile,
                action = action,
                timestamp = System.currentTimeMillis()
            )
            _pendingSyncs.value = _pendingSyncs.value + pending
        }
    }

    /**
     * Traite les synchronisations en attente
     */
    suspend fun processPendingSyncs(
        connectionState: ConnectionState,
        onSync: (Profile, SyncAction) -> Unit
    ) {
        if (connectionState !is ConnectionState.Connected) {
            return
        }

        val pending = _pendingSyncs.value.toList()
        _pendingSyncs.value = emptyList()

        pending.forEach { sync ->
            try {
                onSync(sync.profile, sync.action)
            } catch (e: Exception) {
                // En cas d'erreur, remettre dans la liste des pendants
                _pendingSyncs.value = _pendingSyncs.value + sync
            }
        }
    }

    /**
     * Charge les profils depuis le cache hors ligne
     */
    suspend fun loadFromCache(): List<Profile> {
        return offlineCache.getAllCachedProfiles()
    }
}

/**
 * État de synchronisation
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Synced : SyncState()
    object Offline : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Action de synchronisation
 */
enum class SyncAction {
    UPLOAD,
    DOWNLOAD,
    UPDATE,
    DELETE
}

/**
 * Synchronisation en attente
 */
data class PendingSync(
    val profileId: String,
    val profile: Profile,
    val action: SyncAction,
    val timestamp: Long
)





