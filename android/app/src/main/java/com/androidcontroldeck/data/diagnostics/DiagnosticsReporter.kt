package com.androidcontroldeck.data.diagnostics

import com.androidcontroldeck.data.preferences.SecurePreferences
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.network.ConnectionManager
import com.androidcontroldeck.network.NetworkStatusMonitor
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticsSnapshot(val summary: String)

class DiagnosticsReporter(
    private val settingsRepository: SettingsRepository,
    private val securePreferences: SecurePreferences,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val connectionManager: ConnectionManager,
) {
    suspend fun collect(includeLogs: Boolean): DiagnosticsSnapshot {
        val settings = settingsRepository.settings.first()
        val isOnline = networkStatusMonitor.isOnline.first()
        val isSocketConnected = connectionManager.isConnected.first()
        val tokenPresent = !securePreferences.getAuthToken().isNullOrBlank()
        val handshakePresent = !securePreferences.getHandshakeSecret().isNullOrBlank()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val baseSummary = buildString {
            appendLine("Horodatage: $timestamp")
            appendLine("Réseau disponible: ${if (isOnline) "oui" else "non"}")
            appendLine("WebSocket connectée: ${if (isSocketConnected) "oui" else "non"}")
            appendLine("Serveur: ${settings.serverIp}:${settings.serverPort} (TLS=${settings.useTls})")
            appendLine("Heartbeat: ${settings.heartbeatIntervalMs}ms | Latence cible: ${settings.latencyMs}ms")
            appendLine("Certificat pin: ${settings.pinnedCertSha256?.takeIf { it.isNotBlank() } ?: "non"}")
            appendLine("Token présent: ${if (tokenPresent) "oui" else "non"}")
            appendLine("Secret handshake enregistré: ${if (handshakePresent) "oui" else "non"}")
            appendLine("Langue: ${settings.language} | Thème: ${settings.themeMode}")
        }

        val extraLogs = if (includeLogs) detailedNetworkHints(settings, isOnline, isSocketConnected) else "Journaux non inclus."
        return DiagnosticsSnapshot(summary = baseSummary + "\n" + extraLogs)
    }

    private fun detailedNetworkHints(
        settings: SettingsState,
        isOnline: Boolean,
        isSocketConnected: Boolean,
    ): String {
        val checklist = buildList {
            add("Wi-Fi/4G disponible: ${if (isOnline) "OK" else "À vérifier"}")
            add("Canal temps réel: ${if (isSocketConnected) "connecté" else "déconnecté"}")
            add("IP cible: ${settings.serverIp} | Port: ${settings.serverPort}")
            add("TLS: ${if (settings.useTls) "activé" else "désactivé"}")
            add(
                "Certificat pin: " +
                    (settings.pinnedCertSha256?.takeIf { it.isNotBlank() } ?: "aucun")
            )
        }
        return "Journal détaillé:\n" + checklist.joinToString(separator = "\n")
    }
}
