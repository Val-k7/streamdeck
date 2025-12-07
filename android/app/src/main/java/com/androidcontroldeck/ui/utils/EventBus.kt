package com.androidcontroldeck.ui.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus d'événements global pour l'application Android
 *
 * Permet de communiquer entre composants de manière découplée
 */
object EventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 50)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * Émet un événement
     */
    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }

    /**
     * Émet un événement (version non-suspend)
     */
    fun tryEmit(event: AppEvent): Boolean {
        return _events.tryEmit(event)
    }
}

/**
 * Types d'événements de l'application
 */
sealed class AppEvent {
    /**
     * Événements de connexion
     */
    sealed class Connection : AppEvent() {
        data class Connected(val serverIp: String) : Connection()
        data class Disconnected(val reason: String?) : Connection()
        data class Connecting(val serverIp: String) : Connection()
        data class Error(val message: String) : Connection()
    }

    /**
     * Événements de profil
     */
    sealed class Profile : AppEvent() {
        data class Created(val profileId: String, val profileName: String) : Profile()
        data class Updated(val profileId: String, val profileName: String) : Profile()
        data class Deleted(val profileId: String) : Profile()
        data class Selected(val profileId: String, val profileName: String) : Profile()
        data class Imported(val profileId: String, val profileName: String) : Profile()
        data class Exported(val profileId: String, val profileName: String) : Profile()
    }

    /**
     * Événements de contrôle
     */
    sealed class Control : AppEvent() {
        data class ActionExecuted(
            val controlId: String,
            val action: String,
            val success: Boolean,
            val message: String? = null
        ) : Control()

        data class ActionError(
            val controlId: String,
            val action: String,
            val error: String
        ) : Control()
    }

    /**
     * Événements de synchronisation
     */
    sealed class Sync : AppEvent() {
        object Started : Sync()
        object Completed : Sync()
        data class Error(val message: String) : Sync()
        data class Progress(val current: Int, val total: Int) : Sync()
    }

    /**
     * Événements généraux
     */
    sealed class General : AppEvent() {
        data class ShowMessage(val message: String, val type: MessageType) : General()
        data class ShowError(val error: String) : General()
        data class ShowSuccess(val message: String) : General()
    }
}

/**
 * Type de message
 */
enum class MessageType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}





