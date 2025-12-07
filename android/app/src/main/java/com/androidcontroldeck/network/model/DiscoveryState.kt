package com.androidcontroldeck.network.model

/**
 * État de la découverte de serveurs
 */
sealed class DiscoveryState {
    /**
     * Détection inactive (état initial)
     */
    object Idle : DiscoveryState()

    /**
     * Détection en cours
     */
    object Discovering : DiscoveryState()

    /**
     * Détection arrêtée manuellement
     */
    object Stopped : DiscoveryState()

    /**
     * Erreur lors de la détection
     */
    data class Error(val message: String) : DiscoveryState()
}


