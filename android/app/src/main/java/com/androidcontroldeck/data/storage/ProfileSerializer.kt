package com.androidcontroldeck.data.storage

import com.androidcontroldeck.data.model.Profile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class ProfileSerializer(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {
    fun export(profile: Profile): String = json.encodeToString(profile)

    fun serialize(profile: Profile, includeMetadata: Boolean = true): Profile {
        // Pour l'instant, on retourne le profil tel quel
        // L'option includeMetadata pourrait être utilisée pour exclure certains champs
        return if (includeMetadata) {
            profile
        } else {
            profile.copy(checksum = null) // Exclure le checksum si metadata non inclus
        }
    }

    fun import(jsonPayload: String): Profile = json.decodeFromString(jsonPayload)

    fun checksum(profile: Profile): String {
        val raw = export(profile)
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
