package com.androidcontroldeck.network

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

class AuthRepository(
    private val securePreferences: SecurePreferences,
    private val baseClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun performHandshake(baseUrl: String, secret: String, certificatePinner: CertificatePinner?): String {
        val body = """{"secret":"$secret"}""".toRequestBody("application/json".toMediaType())
        val client = baseClient.newBuilder()
            .apply { certificatePinner?.let { certificatePinner(it) } }
            .build()
        val request = Request.Builder()
            .url("$baseUrl/handshake")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Handshake failed: ${response.code}")
                val payload = response.body?.string() ?: throw IOException("Empty handshake response")
                val token = json.parseToJsonElement(payload).jsonObject["token"]?.jsonPrimitive?.content
                    ?: throw IOException("Token missing from handshake")
                securePreferences.saveAuthToken(token)
                securePreferences.saveHandshakeSecret(secret)
                token
            }
        }
    }

    fun getToken(): String? = securePreferences.getAuthToken()

    fun saveToken(token: String) = securePreferences.saveAuthToken(token)

    fun clearToken() = securePreferences.clearAuthToken()

    fun saveHandshakeSecret(secret: String) = securePreferences.saveHandshakeSecret(secret)

    fun getHandshakeSecret(): String? = securePreferences.getHandshakeSecret()
}
