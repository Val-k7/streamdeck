package com.androidcontroldeck.network

import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.data.preferences.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val securePreferences: SecurePreferences,
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun performHandshake(baseUrl: String, secret: String, certificatePinner: CertificatePinner?): String {
        val handshakeUrl = "$baseUrl/handshake"
        AppLogger.d("AuthRepository", "Performing handshake to: $handshakeUrl")
        val body = """{"secret":"$secret"}""".toRequestBody("application/json".toMediaType())
        val client = baseClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply { certificatePinner?.let { certificatePinner(it) } }
            .build()
        val request = Request.Builder()
            .url(handshakeUrl)
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                AppLogger.d("AuthRepository", "Handshake response code: ${response.code}")
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    AppLogger.e("AuthRepository", "Handshake failed with code ${response.code}: $errorBody")
                    throw IOException("Handshake failed: ${response.code} - $errorBody")
                }
                val payload = response.body?.string() ?: throw IOException("Empty handshake response")
                AppLogger.d("AuthRepository", "Handshake response body: $payload")
                val token = json.parseToJsonElement(payload).jsonObject["token"]?.jsonPrimitive?.content
                    ?: throw IOException("Token missing from handshake")
                securePreferences.saveAuthToken(token)
                securePreferences.saveHandshakeSecret(secret)
                AppLogger.d("AuthRepository", "Handshake successful, token saved")
                token
            } catch (e: java.net.SocketTimeoutException) {
                AppLogger.e("AuthRepository", "Handshake timeout: Le serveur n'a pas répondu dans les délais. Vérifiez que le serveur est démarré et accessible à $handshakeUrl", e)
                throw IOException("Timeout de connexion: Le serveur à $handshakeUrl n'a pas répondu. Vérifiez que le serveur est démarré et que l'adresse IP/port sont corrects.", e)
            } catch (e: java.net.ConnectException) {
                AppLogger.e("AuthRepository", "Handshake connection refused: Le serveur refuse la connexion à $handshakeUrl. Vérifiez que le serveur est démarré.", e)
                throw IOException("Connexion refusée: Impossible de se connecter à $handshakeUrl. Vérifiez que le serveur est démarré et écoute sur le port ${handshakeUrl.substringAfterLast(":")}", e)
            } catch (e: java.net.UnknownHostException) {
                AppLogger.e("AuthRepository", "Handshake unknown host: L'adresse $handshakeUrl n'est pas résolvable.", e)
                throw IOException("Hôte inconnu: Impossible de résoudre l'adresse $handshakeUrl. Vérifiez l'adresse IP.", e)
            } catch (e: Exception) {
                AppLogger.e("AuthRepository", "Handshake exception: ${e.javaClass.simpleName} - ${e.message}", e)
                throw IOException("Erreur de connexion: ${e.message ?: "Erreur inconnue"}", e)
            }
        }
    }

    fun getToken(): String? = securePreferences.getAuthToken()

    fun saveToken(token: String) = securePreferences.saveAuthToken(token)

    fun clearToken() = securePreferences.clearAuthToken()

    fun saveHandshakeSecret(secret: String) = securePreferences.saveHandshakeSecret(secret)

    fun getHandshakeSecret(): String? = securePreferences.getHandshakeSecret()
}
