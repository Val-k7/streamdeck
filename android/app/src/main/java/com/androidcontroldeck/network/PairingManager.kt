package com.androidcontroldeck.network

import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.network.model.PairedServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire de pairing avec les serveurs Control Deck
 */
class PairingManager(
    private val httpClient: OkHttpClient,
    private val logger: UnifiedLogger
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Demander un code de pairing pour un serveur
     * @param server Serveur découvert
     * @param clientId ID du client (optionnel)
     * @return PairingResponse avec le code et les données QR
     */
    suspend fun requestPairingCode(
        server: DiscoveredServer,
        clientId: String? = null
    ): PairingResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "${server.baseUrl}/pairing/request"
            val body = json.encodeToString(
                PairingRequest.serializer(),
                PairingRequest(clientId = clientId)
            )

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                val pairingData = json.parseToJsonElement(responseBody).jsonObject

                val code = pairingData["code"]?.jsonPrimitive?.content
                val expiresAt = pairingData["expiresAt"]?.jsonPrimitive?.content?.toLongOrNull()

                if (code != null && expiresAt != null) {
                    AppLogger.d(TAG, "Pairing code received: $code")
                    logger.logInfo("Pairing code received", mapOf(
                        "serverId" to server.serverId,
                        "code" to code
                    ))

                    return@withContext PairingResponse(
                        code = code,
                        expiresAt = expiresAt,
                        server = server
                    )
                }
            } else {
                AppLogger.e(TAG, "Pairing request failed: ${response.code}")
                logger.logNetworkError("Pairing request failed", Exception("HTTP ${response.code}"))
            }

            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error requesting pairing code", e)
            logger.logNetworkError("Pairing request exception", e)
            null
        }
    }

    /**
     * Confirmer le pairing avec un code
     * @param server Serveur à appairer
     * @param code Code de pairing
     * @param fingerprint Fingerprint du certificat (si TLS)
     * @return PairedServer si le pairing réussit, null sinon
     */
    suspend fun confirmPairing(
        server: DiscoveredServer,
        code: String,
        fingerprint: String? = null
    ): PairedServer? = withContext(Dispatchers.IO) {
        try {
            val url = "${server.baseUrl}/pairing/confirm"
            val body = json.encodeToString(
                PairingConfirm.serializer(),
                PairingConfirm(
                    code = code,
                    serverId = server.serverId,
                    fingerprint = fingerprint
                )
            )

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                val confirmData = json.parseToJsonElement(responseBody).jsonObject

                if (confirmData["status"]?.jsonPrimitive?.content == "paired") {
                    AppLogger.d(TAG, "Pairing confirmed for server: ${server.serverId}")
                    logger.logInfo("Pairing confirmed", mapOf(
                        "serverId" to server.serverId,
                        "serverName" to server.serverName
                    ))

                    return@withContext PairedServer(
                        serverId = server.serverId,
                        serverName = server.serverName,
                        host = server.host,
                        port = server.port,
                        protocol = server.protocol,
                        fingerprint = fingerprint,
                        pairedAt = System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis(),
                        isAutoConnect = true
                    )
                }
            } else {
                val errorBody = response.body?.string()
                AppLogger.e(TAG, "Pairing confirmation failed: ${response.code}, $errorBody")
                logger.logNetworkError("Pairing confirmation failed", Exception("HTTP ${response.code}: $errorBody"))
            }

            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error confirming pairing", e)
            logger.logNetworkError("Pairing confirmation exception", e)
            null
        }
    }

    companion object {
        private const val TAG = "PairingManager"
    }
}

@kotlinx.serialization.Serializable
private data class PairingRequest(
    val clientId: String? = null
)

@kotlinx.serialization.Serializable
private data class PairingConfirm(
    val code: String,
    val serverId: String,
    val fingerprint: String? = null
)

data class PairingResponse(
    val code: String,
    val expiresAt: Long,
    val server: DiscoveredServer
) {
    /**
     * Vérifier si le code est expiré
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    /**
     * Temps restant avant expiration (en millisecondes)
     */
    fun timeRemaining(): Long = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}

