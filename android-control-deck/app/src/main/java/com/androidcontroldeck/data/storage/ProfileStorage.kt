package com.androidcontroldeck.data.storage

import android.content.Context
import com.androidcontroldeck.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ProfileStorage(
    private val context: Context,
    private val serializer: ProfileSerializer = ProfileSerializer(Json { prettyPrint = true })
) {
    private val profileDir: File by lazy { File(context.filesDir, "profiles").apply { mkdirs() } }

    suspend fun save(profile: Profile) = withContext(Dispatchers.IO) {
        val payload = serializer.export(profile)
        File(profileDir, "${profile.id}.json").writeText(payload)
    }

    suspend fun loadProfiles(): List<Profile> = withContext(Dispatchers.IO) {
        profileDir.listFiles { file -> file.extension == "json" }?.mapNotNull { file ->
            runCatching { serializer.import(file.readText()) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun importFromJson(jsonPayload: String): Profile = withContext(Dispatchers.IO) {
        val profile = serializer.import(jsonPayload)
        save(profile)
        profile
    }

    suspend fun exportToJson(profile: Profile): String = withContext(Dispatchers.IO) {
        serializer.export(profile)
    }
}
