package com.androidcontroldeck.data.storage

import android.content.Context
import com.androidcontroldeck.data.model.Profile
import android.os.Trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ProfileStorage(
    private val context: Context,
    private val serializer: ProfileSerializer = ProfileSerializer(Json { prettyPrint = true })
) {
    private val profileDir: File by lazy { File(context.filesDir, "profiles").apply { mkdirs() } }
    private var cachedProfiles: List<Profile>? = null

    suspend fun save(profile: Profile) = withContext(Dispatchers.IO) {
        val payload = serializer.export(profile)
        File(profileDir, "${profile.id}.json").writeText(payload)
        cachedProfiles = cachedProfiles.orEmpty()
            .filterNot { it.id == profile.id }
            .plus(profile)
    }

    suspend fun loadProfiles(): List<Profile> = withContext(Dispatchers.IO) {
        cachedProfiles?.let { return@withContext it }
        Trace.beginSection("ProfileCache.load")
        val profiles = try {
            profileDir.listFiles { file -> file.extension == "json" }?.mapNotNull { file ->
                runCatching { serializer.import(file.readText()) }.getOrNull()
            } ?: emptyList()
        } finally {
            Trace.endSection()
        }
        cachedProfiles = profiles
        profiles
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
