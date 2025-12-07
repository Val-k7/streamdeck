package com.androidcontroldeck.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearAuthToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun saveHandshakeSecret(secret: String) {
        prefs.edit().putString(KEY_HANDSHAKE_SECRET, secret).apply()
    }

    fun getHandshakeSecret(): String? = prefs.getString(KEY_HANDSHAKE_SECRET, null)

    fun clearHandshakeSecret() {
        prefs.edit().remove(KEY_HANDSHAKE_SECRET).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_HANDSHAKE_SECRET = "handshake_secret"
    }
}
