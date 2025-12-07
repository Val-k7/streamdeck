package com.androidcontroldeck.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.network.model.DiscoverySource
import com.androidcontroldeck.network.model.ServerCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire de découverte de serveurs Control Deck
 * Utilise NSD (Network Service Discovery) et UDP broadcast comme fallback
 */
class ServerDiscoveryManager(
    private val context: Context,
    private val logger: UnifiedLogger,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var udpSocket: DatagramSocket? = null
    private var udpListenerJob: kotlinx.coroutines.Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    // Cache temporaire pour éviter de perdre les serveurs pendant les transitions
    private val serverCache = mutableMapOf<String, Pair<DiscoveredServer, Long>>()
    private val CACHE_DURATION_MS = 30_000L // 30 secondes

    /**
     * Démarrer la découverte de serveurs
     */
    fun startDiscovery() {
        if (_isDiscovering.value) {
            AppLogger.d(TAG, "Discovery already in progress")
            return
        }

        _isDiscovering.value = true
        _discoveredServers.value = emptyList()

        AppLogger.d(TAG, "Starting server discovery...")
        logger.logInfo("Starting server discovery", emptyMap())

        // Démarrer NSD discovery
        startNsdDiscovery()

        // Démarrer UDP broadcast discovery (fallback)
        startUdpDiscovery()
    }

    /**
     * Arrêter la découverte
     */
    fun stopDiscovery() {
        if (!_isDiscovering.value) {
            return
        }

        AppLogger.d(TAG, "Stopping server discovery...")
        _isDiscovering.value = false

        stopNsdDiscovery()
        stopUdpDiscovery()
    }

    /**
     * Démarrer la découverte NSD (mDNS)
     */
    private fun startNsdDiscovery() {
        if (nsdManager == null) {
            AppLogger.w(TAG, "NSD service not available")
            return
        }

        nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "NSD discovery start failed: $errorCode")
                logger.logNetworkError("NSD discovery failed", Exception("Error code: $errorCode"))
                // Essayer UDP si NSD échoue
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLogger.e(TAG, "NSD discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                AppLogger.d(TAG, "NSD discovery started for type: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                AppLogger.d(TAG, "NSD discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                AppLogger.d(TAG, "Service found: ${serviceInfo.serviceName}, type: ${serviceInfo.serviceType}")
                try {
                    nsdManager.resolveService(serviceInfo, createResolveListener())
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to resolve service: ${serviceInfo.serviceName}", e)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                AppLogger.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                scope.launch {
                    removeServer(serviceInfo.serviceName)
                }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start NSD discovery", e)
            logger.logNetworkError("NSD discovery start exception", e)
        }
    }

    /**
     * Créer un listener pour résoudre les services NSD
     */
    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                AppLogger.e(TAG, "Failed to resolve service: ${serviceInfo.serviceName}, error: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                AppLogger.d(TAG, "Service resolved: ${serviceInfo.serviceName}, host: ${serviceInfo.host?.hostAddress}, port: ${serviceInfo.port}")
                AppLogger.d(TAG, "Service attributes: ${serviceInfo.attributes}")
                scope.launch {
                    try {
                        // Les attributs TXT peuvent être des ByteArray, les convertir en String
                        val serverId = serviceInfo.attributes["id"]?.let { attr ->
                            when (attr) {
                                is ByteArray -> String(attr)
                                is String -> attr
                                else -> attr.toString()
                            }
                        }?.trim() ?: run {
                            AppLogger.w(TAG, "No serverId in service attributes, using service name as fallback")
                            // Fallback: utiliser le service name ou récupérer via /discovery
                            null
                        }

                        val port = serviceInfo.port
                        val protocol = serviceInfo.attributes["protocol"]?.let { attr ->
                            when (attr) {
                                is ByteArray -> String(attr)
                                is String -> attr
                                else -> attr.toString()
                            }
                        }?.trim() ?: "ws"
                        val host = serviceInfo.host?.hostAddress ?: run {
                            AppLogger.e(TAG, "No host address in resolved service")
                            return@launch
                        }

                        AppLogger.d(TAG, "Processing resolved service: host=$host, port=$port, protocol=$protocol, serverId=$serverId")

                        // Si on n'a pas de serverId depuis les attributs, récupérer via /discovery
                        val finalServerId = serverId ?: run {
                            AppLogger.d(TAG, "Fetching server info to get serverId")
                            val tempServer = fetchServerInfo(host, port, protocol, "", serviceInfo.serviceName)
                            tempServer?.serverId
                        }

                        if (finalServerId != null) {
                            // Récupérer plus d'informations via l'endpoint /discovery
                            val server = fetchServerInfo(host, port, protocol, finalServerId, serviceInfo.serviceName)
                            if (server != null) {
                                AppLogger.d(TAG, "Adding discovered server: ${server.serverName} (${server.serverId})")
                                addDiscoveredServer(server.copy(source = DiscoverySource.NSD))
                            } else {
                                AppLogger.w(TAG, "Failed to fetch server info from /discovery endpoint")
                            }
                        } else {
                            AppLogger.w(TAG, "Could not determine serverId for service: ${serviceInfo.serviceName}")
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Error processing resolved service", e)
                    }
                }
            }
        }
    }

    /**
     * Arrêter la découverte NSD
     */
    private fun stopNsdDiscovery() {
        nsdDiscoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error stopping NSD discovery", e)
            }
        }
        nsdDiscoveryListener = null
    }

    /**
     * Démarrer la découverte UDP (fallback)
     */
    private fun startUdpDiscovery() {
        udpListenerJob = scope.launch(Dispatchers.IO) {
            try {
                AppLogger.d(TAG, "Starting UDP discovery...")

                // Créer un socket pour envoyer et recevoir
                val udpSocket = DatagramSocket(4457)
                udpSocket.broadcast = true
                udpSocket.soTimeout = 10000 // 10 secondes de timeout

                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                val requestMessage = """{"type":"control-deck-discovery-request"}"""
                val requestData = requestMessage.toByteArray()

                // Envoyer une première requête immédiatement
                try {
                    val requestPacket = DatagramPacket(
                        requestData,
                        requestData.size,
                        InetAddress.getByName("255.255.255.255"),
                        4456
                    )
                    udpSocket.send(requestPacket)
                    AppLogger.d(TAG, "UDP discovery request sent to port 4456 from port 4457")
                } catch (e: Exception) {
                    AppLogger.d(TAG, "Failed to send initial UDP request", e)
                }

                // Lancer une coroutine pour envoyer des requêtes périodiques
                val requestJob = scope.launch(Dispatchers.IO) {
                    while (_isDiscovering.value) {
                        kotlinx.coroutines.delay(5000) // Attendre 5 secondes
                        if (!_isDiscovering.value) break

                        try {
                            // Vérifier que le socket est toujours ouvert
                            if (udpSocket.isClosed) {
                                AppLogger.w(TAG, "UDP socket is closed, stopping periodic requests")
                                break
                            }

                            val requestPacket = DatagramPacket(
                                requestData,
                                requestData.size,
                                InetAddress.getByName("255.255.255.255"),
                                4456
                            )
                            udpSocket.send(requestPacket)
                            AppLogger.d(TAG, "UDP discovery request sent (periodic)")
                        } catch (e: java.net.SocketException) {
                            // Socket fermé ou erreur de permission
                            AppLogger.d(TAG, "UDP socket error (may be closed): ${e.message}")
                            break
                        } catch (e: Exception) {
                            // Autres erreurs (EPERM, etc.) - continuer à essayer
                            AppLogger.d(TAG, "UDP periodic request send failed: ${e.message}")
                            // Ne pas arrêter, continuer à essayer
                        }
                    }
                }

                // Écouter les réponses
                try {
                    while (_isDiscovering.value) {
                        try {
                            udpSocket.receive(responsePacket)
                            val response = String(responsePacket.data, 0, responsePacket.length)
                            AppLogger.d(TAG, "UDP packet received from ${responsePacket.address.hostAddress}:${responsePacket.port}: $response")

                            val responseData = json.parseToJsonElement(response).jsonObject

                            if (responseData["type"]?.jsonPrimitive?.content == "control-deck-discovery-response") {
                                AppLogger.d(TAG, "UDP discovery response received: $response")
                                val serverId = responseData["serverId"]?.jsonPrimitive?.content ?: continue
                                val serverName = responseData["serverName"]?.jsonPrimitive?.content ?: "Unknown"
                                val port = responseData["port"]?.jsonPrimitive?.content?.toIntOrNull() ?: continue
                                val protocol = responseData["protocol"]?.jsonPrimitive?.content ?: "ws"
                                val host = responsePacket.address.hostAddress ?: continue

                                AppLogger.d(TAG, "Processing UDP discovered server: $serverName ($serverId) at $host:$port")
                                val server = fetchServerInfo(host, port, protocol, serverId, serverName)
                                if (server != null) {
                                    AppLogger.d(TAG, "Adding UDP discovered server: ${server.serverName}")
                                    addDiscoveredServer(server.copy(source = DiscoverySource.UDP))
                                } else {
                                    AppLogger.w(TAG, "Failed to fetch server info for UDP discovered server")
                                }
                            } else {
                                AppLogger.d(TAG, "Received UDP packet with unexpected type: ${responseData["type"]?.jsonPrimitive?.content}")
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            // Timeout normal, continuer à écouter
                            // Ne pas logger pour éviter le spam
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "Error receiving UDP response", e)
                            kotlinx.coroutines.delay(1000) // Attendre un peu avant de réessayer
                        }
                    }
                } finally {
                    requestJob.cancel()
                    udpSocket.close()
                    AppLogger.d(TAG, "UDP discovery socket closed")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "UDP discovery failed", e)
                logger.logNetworkError("UDP discovery failed", e)
            }
        }
    }

    /**
     * Arrêter la découverte UDP
     */
    private fun stopUdpDiscovery() {
        udpListenerJob?.cancel()
        udpListenerJob = null
        udpSocket?.close()
        udpSocket = null
    }

    /**
     * Récupérer les informations complètes d'un serveur via l'endpoint /discovery
     */
    private suspend fun fetchServerInfo(
        host: String,
        port: Int,
        protocol: String,
        serverId: String,
        serverName: String
    ): DiscoveredServer? {
        return try {
            val url = "${if (protocol == "wss") "https" else "http"}://$host:$port/discovery"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val discoveryData = json.parseToJsonElement(body).jsonObject

                val capabilities = discoveryData["capabilities"]?.jsonObject?.let {
                    ServerCapabilities(
                        tls = it["tls"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                        websocket = it["websocket"]?.jsonPrimitive?.content?.toBoolean() ?: true,
                        profiles = it["profiles"]?.jsonPrimitive?.content?.toBoolean() ?: true,
                        plugins = it["plugins"]?.jsonPrimitive?.content?.toBoolean() ?: true
                    )
                }

                DiscoveredServer(
                    serverId = discoveryData["serverId"]?.jsonPrimitive?.content ?: serverId,
                    serverName = discoveryData["serverName"]?.jsonPrimitive?.content ?: serverName,
                    host = discoveryData["host"]?.jsonPrimitive?.content ?: host,
                    port = discoveryData["port"]?.jsonPrimitive?.content?.toIntOrNull() ?: port,
                    protocol = discoveryData["protocol"]?.jsonPrimitive?.content ?: protocol,
                    version = discoveryData["version"]?.jsonPrimitive?.content ?: "1.0.0",
                    capabilities = capabilities
                )
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to fetch server info", e)
            null
        }
    }

    /**
     * Ajouter un serveur découvert avec gestion améliorée des doublons.
     *
     * Cette méthode :
     * - Met à jour le cache temporaire (30 secondes) pour éviter de perdre les serveurs
     * - Détecte et met à jour les serveurs existants au lieu de créer des doublons
     * - Gère les changements d'IP en mettant à jour le serveur existant
     * - Privilégie les sources de découverte plus fiables (NSD > UDP)
     *
     * @param server Le serveur découvert à ajouter ou mettre à jour
     */
    private fun addDiscoveredServer(server: DiscoveredServer) {
        val current = _discoveredServers.value.toMutableList()
        val now = System.currentTimeMillis()

        // Mettre à jour le cache
        serverCache[server.serverId] = Pair(server, now)

        // Nettoyer le cache des entrées expirées
        serverCache.entries.removeAll { (_, value) ->
            now - value.second > CACHE_DURATION_MS
        }

        // Chercher un serveur existant avec le même serverId
        val existingIndex = current.indexOfFirst { it.serverId == server.serverId }

        if (existingIndex >= 0) {
            // Serveur existant : mettre à jour si l'IP a changé ou si c'est une source plus fiable
            val existing = current[existingIndex]
            val shouldUpdate = when {
                // Mettre à jour si l'IP a changé
                existing.host != server.host || existing.port != server.port -> {
                    AppLogger.d(TAG, "Updating server ${server.serverId}: IP changed from ${existing.host}:${existing.port} to ${server.host}:${server.port}")
                    true
                }
                // Mettre à jour si la source est plus fiable (NSD > UDP)
                server.source == DiscoverySource.NSD && existing.source != DiscoverySource.NSD -> {
                    AppLogger.d(TAG, "Updating server ${server.serverId}: better source (NSD)")
                    true
                }
                else -> false
            }

            if (shouldUpdate) {
                current[existingIndex] = server
                _discoveredServers.value = current
                AppLogger.d(TAG, "Server updated: ${server.serverName} (${server.serverId})")
            }
        } else {
            // Nouveau serveur : l'ajouter
            current.add(server)
            _discoveredServers.value = current
            AppLogger.d(TAG, "Server added: ${server.serverName} (${server.serverId})")
            logger.logInfo("Server discovered", mapOf(
                "serverId" to server.serverId,
                "serverName" to server.serverName,
                "source" to server.source.name
            ))
        }
    }

    /**
     * Supprimer un serveur de la liste
     */
    private fun removeServer(serverName: String) {
        val current = _discoveredServers.value.toMutableList()
        val removed = current.removeAll { it.serverName == serverName }
        if (removed) {
            _discoveredServers.value = current
            AppLogger.d(TAG, "Server removed: $serverName")
        }
    }

    /**
     * Récupérer les serveurs du cache (pour éviter de perdre les serveurs pendant les transitions)
     */
    fun getCachedServers(): List<DiscoveredServer> {
        val now = System.currentTimeMillis()
        return serverCache.values
            .filter { now - it.second <= CACHE_DURATION_MS }
            .map { it.first }
    }

    companion object {
        private const val TAG = "ServerDiscoveryManager"
        private const val SERVICE_TYPE = "_control-deck._tcp"
    }
}

