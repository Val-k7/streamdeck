package com.androidcontroldeck.data.storage

import android.content.Context
import com.androidcontroldeck.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire de sauvegarde automatique pour les profils
 */
class ProfileBackupManager(
    private val context: Context,
    private val serializer: ProfileSerializer = ProfileSerializer()
) {
    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply { mkdirs() }
    }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Crée une sauvegarde automatique d'un profil avant import/export
     */
    suspend fun createAutoBackup(profile: Profile, reason: BackupReason = BackupReason.IMPORT): BackupInfo {
        return withContext(Dispatchers.IO) {
            val timestamp = dateFormat.format(Date())
            val filename = "${profile.id}_${reason.name.lowercase()}_$timestamp.json"
            val backupFile = File(backupDir, filename)

            try {
                val jsonContent = serializer.export(profile)
                backupFile.writeText(jsonContent)

                // Limiter le nombre de sauvegardes (garder les 10 plus récentes)
                cleanupOldBackups(profile.id, reason, maxBackups = 10)

                BackupInfo(
                    profileId = profile.id,
                    reason = reason,
                    timestamp = timestamp,
                    filePath = backupFile.absolutePath,
                    success = true
                )
            } catch (e: Exception) {
                BackupInfo(
                    profileId = profile.id,
                    reason = reason,
                    timestamp = timestamp,
                    filePath = backupFile.absolutePath,
                    success = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Crée une sauvegarde de tous les profils
     */
    suspend fun createFullBackup(profiles: List<Profile>): BackupInfo {
        return withContext(Dispatchers.IO) {
            val timestamp = dateFormat.format(Date())
            val filename = "full_backup_$timestamp.json"
            val backupFile = File(backupDir, filename)

            try {
                val backupData = mapOf(
                    "timestamp" to timestamp,
                    "version" to "1.0",
                    "profiles" to profiles.map { serializer.export(it) }
                )

                val jsonContent = json.encodeToString(backupData)
                backupFile.writeText(jsonContent)

                // Limiter le nombre de sauvegardes complètes
                cleanupOldFullBackups(maxBackups = 5)

                BackupInfo(
                    profileId = "all",
                    reason = BackupReason.FULL_BACKUP,
                    timestamp = timestamp,
                    filePath = backupFile.absolutePath,
                    success = true
                )
            } catch (e: Exception) {
                BackupInfo(
                    profileId = "all",
                    reason = BackupReason.FULL_BACKUP,
                    timestamp = timestamp,
                    filePath = backupFile.absolutePath,
                    success = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Restaure un profil depuis une sauvegarde
     */
    suspend fun restoreBackup(backupInfo: BackupInfo): Profile? {
        return withContext(Dispatchers.IO) {
            try {
                val backupFile = File(backupInfo.filePath)
                if (!backupFile.exists()) {
                    return@withContext null
                }

                val jsonContent = backupFile.readText()
                serializer.import(jsonContent)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Liste toutes les sauvegardes disponibles pour un profil
     */
    suspend fun listBackups(profileId: String? = null): List<BackupInfo> {
        return withContext(Dispatchers.IO) {
            backupDir.listFiles { file ->
                file.extension == "json" && file.nameWithoutExtension.contains("backup")
            }?.mapNotNull { file ->
                parseBackupFileName(file.name)?.copy(filePath = file.absolutePath)
            }?.filter {
                profileId == null || it.profileId == profileId
            }?.sortedByDescending {
                it.timestamp
            } ?: emptyList()
        }
    }

    /**
     * Supprime une sauvegarde
     */
    suspend fun deleteBackup(backupInfo: BackupInfo) {
        withContext(Dispatchers.IO) {
            try {
                File(backupInfo.filePath).delete()
            } catch (e: Exception) {
                // Ignorer les erreurs de suppression
            }
        }
    }

    /**
     * Nettoie les anciennes sauvegardes
     */
    private fun cleanupOldBackups(profileId: String, reason: BackupReason, maxBackups: Int) {
        val backups = backupDir.listFiles { file ->
            file.name.startsWith("${profileId}_${reason.name.lowercase()}_")
        }?.sortedByDescending { it.lastModified() }

        backups?.drop(maxBackups)?.forEach { it.delete() }
    }

    /**
     * Nettoie les anciennes sauvegardes complètes
     */
    private fun cleanupOldFullBackups(maxBackups: Int) {
        val backups = backupDir.listFiles { file ->
            file.name.startsWith("full_backup_")
        }?.sortedByDescending { it.lastModified() }

        backups?.drop(maxBackups)?.forEach { it.delete() }
    }

    /**
     * Parse le nom de fichier de sauvegarde
     */
    private fun parseBackupFileName(filename: String): BackupInfo? {
        return try {
            // Format: {profileId}_{reason}_{timestamp}.json
            val parts = filename.removeSuffix(".json").split("_")
            if (parts.size < 3) return null

            val timestampParts = parts.takeLast(3) // yyyy-MM-dd_HH-mm-ss
            val timestamp = timestampParts.joinToString("_")

            val reasonIndex = parts.indexOfLast { it in BackupReason.values().map { r -> r.name.lowercase() } }
            if (reasonIndex == -1) return null

            val reason = BackupReason.valueOf(parts[reasonIndex].uppercase())
            val profileId = parts.take(reasonIndex).joinToString("_")

            BackupInfo(
                profileId = profileId,
                reason = reason,
                timestamp = timestamp,
                filePath = "", // Sera rempli plus tard
                success = true
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtient l'espace disque utilisé par les sauvegardes
     */
    suspend fun getBackupSize(): Long {
        return withContext(Dispatchers.IO) {
            backupDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }
}

/**
 * Raison de la sauvegarde
 */
enum class BackupReason {
    IMPORT,
    EXPORT,
    EDIT,
    DELETE,
    FULL_BACKUP
}

/**
 * Informations sur une sauvegarde
 */
data class BackupInfo(
    val profileId: String,
    val reason: BackupReason,
    val timestamp: String,
    val filePath: String,
    val success: Boolean,
    val error: String? = null
)





