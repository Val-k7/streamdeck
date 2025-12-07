package com.androidcontroldeck.network.model

import kotlinx.serialization.Serializable

/**
 * Représente un serveur appairé (connecté avec succès)
 */
@Serializable
data class PairedServer(
    val serverId: String,
    val serverName: String,
    val host: String,
    val port: Int,
    val protocol: String, // "ws" ou "wss"
    val fingerprint: String? = null, // SHA-256 fingerprint du certificat (si TLS)
    val pairedAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isAutoConnect: Boolean = true // Se connecter automatiquement au démarrage
) {
    /**
     * URL de base pour les requêtes HTTP
     */
    val baseUrl: String
        get() = "$protocol://$host:$port"

    /**
     * URL WebSocket
     */
    val webSocketUrl: String
        get() = "${if (protocol == "wss") "wss" else "ws"}://$host:$port/ws"
}

