package com.androidcontroldeck.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val SERVER_IP = stringPreferencesKey("server_ip")
private val SERVER_PORT = intPreferencesKey("server_port")
private val HEARTBEAT_INTERVAL = intPreferencesKey("heartbeat_interval")
private val LATENCY = intPreferencesKey("latency_target")
private val THEME = stringPreferencesKey("theme")
private val LANGUAGE = stringPreferencesKey("language")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val serverIp: String = "192.168.0.2",
    val serverPort: Int = 4455,
    val heartbeatIntervalMs: Int = 5_000,
    val latencyMs: Int = 16,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "fr"
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<SettingsState> = context.settingsDataStore.data.map { prefs ->
        SettingsState(
            serverIp = prefs[SERVER_IP] ?: "192.168.0.2",
            serverPort = prefs[SERVER_PORT] ?: 4455,
            heartbeatIntervalMs = prefs[HEARTBEAT_INTERVAL] ?: 5_000,
            latencyMs = prefs[LATENCY] ?: 16,
            themeMode = prefs[THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            language = prefs[LANGUAGE] ?: "fr"
        )
    }

    suspend fun update(transform: SettingsState.() -> SettingsState) {
        context.settingsDataStore.edit { prefs: Preferences ->
            val current = settingsDataFromPrefs(prefs)
            val updated = current.transform()
            prefs[SERVER_IP] = updated.serverIp
            prefs[SERVER_PORT] = updated.serverPort
            prefs[HEARTBEAT_INTERVAL] = updated.heartbeatIntervalMs
            prefs[LATENCY] = updated.latencyMs
            prefs[THEME] = updated.themeMode.name
            prefs[LANGUAGE] = updated.language
        }
    }

    private fun settingsDataFromPrefs(prefs: Preferences) = SettingsState(
        serverIp = prefs[SERVER_IP] ?: "192.168.0.2",
        serverPort = prefs[SERVER_PORT] ?: 4455,
        heartbeatIntervalMs = prefs[HEARTBEAT_INTERVAL] ?: 5_000,
        latencyMs = prefs[LATENCY] ?: 16,
        themeMode = prefs[THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
        language = prefs[LANGUAGE] ?: "fr"
    )
}
