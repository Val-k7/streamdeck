package com.androidcontroldeck.data.storage

import android.content.Context
import com.androidcontroldeck.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * Cache hors ligne pour les profils et les données
 */
class OfflineCache(
    private val context: Context,
    private val serializer: ProfileSerializer = ProfileSerializer()
) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "offline").apply { mkdirs() }
    }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Sauvegarde un profil dans le cache hors ligne
     */
    suspend fun cacheProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, "profile_${profile.id}.json")
                val jsonContent = serializer.export(profile)
                cacheFile.writeText(jsonContent)
            } catch (e: Exception) {
                // Ignorer les erreurs de cache
            }
        }
    }

    /**
     * Sauvegarde tous les profils dans le cache
     */
    suspend fun cacheAllProfiles(profiles: List<Profile>) {
        withContext(Dispatchers.IO) {
            profiles.forEach { cacheProfile(it) }

            // Sauvegarder aussi la liste des profils
            val listFile = File(cacheDir, "profiles_list.json")
            val profileIds = profiles.map { it.id }
            listFile.writeText(json.encodeToString(profileIds))
        }
    }

    /**
     * Récupère un profil depuis le cache hors ligne
     */
    suspend fun getCachedProfile(profileId: String): Profile? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, "profile_$profileId.json")
                if (!cacheFile.exists()) return@withContext null

                val jsonContent = cacheFile.readText()
                serializer.import(jsonContent)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Récupère tous les profils en cache
     */
    suspend fun getAllCachedProfiles(): List<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val listFile = File(cacheDir, "profiles_list.json")
                if (!listFile.exists()) return@withContext emptyList()

                val profileIds = json.decodeFromString<List<String>>(listFile.readText())
                profileIds.mapNotNull { getCachedProfile(it) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Vérifie si un profil est en cache
     */
    suspend fun isProfileCached(profileId: String): Boolean {
        return withContext(Dispatchers.IO) {
            File(cacheDir, "profile_$profileId.json").exists()
        }
    }

    /**
     * Supprime un profil du cache
     */
    suspend fun removeCachedProfile(profileId: String) {
        withContext(Dispatchers.IO) {
            try {
                File(cacheDir, "profile_$profileId.json").delete()
            } catch (e: Exception) {
                // Ignorer les erreurs
            }
        }
    }

    /**
     * Vide tout le cache
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                // Ignorer les erreurs
            }
        }
    }

    /**
     * Obtient la taille du cache
     */
    suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }

    /**
     * Nettoie les anciens fichiers de cache (plus de 7 jours)
     */
    suspend fun cleanupOldCache(maxAgeDays: Int = 7) {
        withContext(Dispatchers.IO) {
            try {
                val maxAge = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
                cacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < maxAge) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignorer les erreurs
            }
        }
    }
}





