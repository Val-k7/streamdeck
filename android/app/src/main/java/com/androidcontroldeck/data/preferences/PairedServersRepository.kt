package com.androidcontroldeck.data.preferences

import android.content.Context
import com.androidcontroldeck.network.model.PairedServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.pairedServersDataStore by preferencesDataStore(name = "paired_servers")

private val PAIRED_SERVERS_KEY = stringPreferencesKey("paired_servers_list")

@Serializable
private data class PairedServersData(
    val servers: List<PairedServer> = emptyList()
)

/**
 * Repository pour gérer les serveurs appairés
 * Utilise DataStore pour le stockage persistant
 */
class PairedServersRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Flow des serveurs appairés
     */
    val pairedServers: Flow<List<PairedServer>> = context.pairedServersDataStore.data.map { prefs ->
        val serversJson = prefs[PAIRED_SERVERS_KEY] ?: "{\"servers\":[]}"
        try {
            val data = json.decodeFromString<PairedServersData>(serversJson)
            data.servers
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Ajouter ou mettre à jour un serveur appairé
     */
    suspend fun addOrUpdateServer(server: PairedServer) {
        context.pairedServersDataStore.edit { prefs ->
            val currentJson = prefs[PAIRED_SERVERS_KEY] ?: "{\"servers\":[]}"
            val currentData = try {
                json.decodeFromString<PairedServersData>(currentJson)
            } catch (e: Exception) {
                PairedServersData()
            }

            val updatedServers = currentData.servers.toMutableList()
            val existingIndex = updatedServers.indexOfFirst { it.serverId == server.serverId }

            if (existingIndex >= 0) {
                // Mettre à jour le serveur existant
                updatedServers[existingIndex] = server.copy(
                    pairedAt = updatedServers[existingIndex].pairedAt // Conserver la date de pairing originale
                )
            } else {
                // Ajouter un nouveau serveur
                updatedServers.add(server)
            }

            val updatedData = PairedServersData(servers = updatedServers)
            prefs[PAIRED_SERVERS_KEY] = json.encodeToString(updatedData)
        }
    }

    /**
     * Supprimer un serveur appairé
     */
    suspend fun removeServer(serverId: String) {
        context.pairedServersDataStore.edit { prefs ->
            val currentJson = prefs[PAIRED_SERVERS_KEY] ?: "{\"servers\":[]}"
            val currentData = try {
                json.decodeFromString<PairedServersData>(currentJson)
            } catch (e: Exception) {
                PairedServersData()
            }

            val updatedServers = currentData.servers.filter { it.serverId != serverId }
            val updatedData = PairedServersData(servers = updatedServers)
            prefs[PAIRED_SERVERS_KEY] = json.encodeToString(updatedData)
        }
    }

    /**
     * Obtenir un serveur appairé par son ID
     */
    suspend fun getServer(serverId: String): PairedServer? {
        val prefs = context.pairedServersDataStore.data.first()
        val serversJson = prefs[PAIRED_SERVERS_KEY] ?: return null
        return try {
            val data = json.decodeFromString<PairedServersData>(serversJson)
            data.servers.firstOrNull { it.serverId == serverId }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mettre à jour la dernière vue d'un serveur
     */
    suspend fun updateLastSeen(serverId: String) {
        context.pairedServersDataStore.edit { prefs ->
            val currentJson = prefs[PAIRED_SERVERS_KEY] ?: "{\"servers\":[]}"
            val currentData = try {
                json.decodeFromString<PairedServersData>(currentJson)
            } catch (e: Exception) {
                PairedServersData()
            }

            val updatedServers = currentData.servers.map { server ->
                if (server.serverId == serverId) {
                    server.copy(lastSeen = System.currentTimeMillis())
                } else {
                    server
                }
            }

            val updatedData = PairedServersData(servers = updatedServers)
            prefs[PAIRED_SERVERS_KEY] = json.encodeToString(updatedData)
        }
    }

    /**
     * Obtenir le dernier serveur connecté (pour reconnexion automatique)
     */
    suspend fun getLastConnectedServer(): PairedServer? {
        val prefs = context.pairedServersDataStore.data.first()
        val serversJson = prefs[PAIRED_SERVERS_KEY] ?: return null
        return try {
            val data = json.decodeFromString<PairedServersData>(serversJson)
            data.servers
                .filter { it.isAutoConnect }
                .maxByOrNull { it.lastSeen }
        } catch (e: Exception) {
            null
        }
    }
}

