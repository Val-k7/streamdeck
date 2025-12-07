package com.androidcontroldeck.network.model

import kotlinx.serialization.Serializable

/**
 * Représente un serveur découvert sur le réseau
 */
@Serializable
data class DiscoveredServer(
    val serverId: String,
    val serverName: String,
    val host: String,
    val port: Int,
    val protocol: String, // "ws" ou "wss"
    val version: String = "1.0.0",
    val capabilities: ServerCapabilities? = null,
    val discoveredAt: Long = System.currentTimeMillis(),
    val source: DiscoverySource = DiscoverySource.UNKNOWN
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

    /**
     * URL de découverte
     */
    val discoveryUrl: String
        get() = "$baseUrl/discovery"
}

@Serializable
data class ServerCapabilities(
    val tls: Boolean = false,
    val websocket: Boolean = true,
    val profiles: Boolean = true,
    val plugins: Boolean = true
)

enum class DiscoverySource {
    NSD,        // Découvert via Network Service Discovery (mDNS)
    UDP,        // Découvert via broadcast UDP
    MANUAL,     // Configuration manuelle
    PAIRED,     // Serveur déjà appairé
    UNKNOWN     // Source inconnue
}

