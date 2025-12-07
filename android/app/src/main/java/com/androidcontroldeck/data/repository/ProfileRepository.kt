package com.androidcontroldeck.data.repository

import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.templates.ProfileTemplates
import com.androidcontroldeck.data.storage.ProfileSerializer
import com.androidcontroldeck.data.storage.ProfileStorage
import com.androidcontroldeck.data.storage.ProfileValidator
import com.androidcontroldeck.data.storage.AssetCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.os.SystemClock
import com.androidcontroldeck.network.WebSocketClient
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.network.model.AckPayload
import java.util.UUID
import kotlin.math.pow

class ProfileRepository(
    private val storage: ProfileStorage,
    private val serializer: ProfileSerializer = ProfileSerializer(),
    private val validator: ProfileValidator = ProfileValidator(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val stringProvider: (Int) -> String,
    private val webSocketClient: WebSocketClient? = null
) {
    private val json = Json { encodeDefaults = true }
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    // État de synchronisation du profil actuel
    private val _syncState = MutableStateFlow<ProfileSyncState?>(null)
    val syncState: StateFlow<ProfileSyncState?> = _syncState.asStateFlow()

    // Map pour suivre les messages en attente de confirmation
    private val pendingProfileSyncs = mutableMapOf<String, ProfileSyncAttempt>()

    data class ProfileSyncAttempt(
        val profileId: String,
        val messageId: String,
        val profile: Profile,
        val sentAt: Long,
        val retryCount: Int = 0
    )

    init {
        scope.launch { load() }

        // Écouter les ACK de profil pour confirmer la synchronisation
        webSocketClient?.let { client ->
            scope.launch {
                client.profileAckEvents.collect { ack ->
                    if (ack.type == "profile:select:ack") {
                        handleProfileSelectAck(ack)
                    }
                }
            }
        }
    }

    fun refresh() {
        scope.launch {
            storage.invalidateCache()
            load()
        }
    }

    suspend fun prime(assetCache: AssetCache? = null) {
        load()
        assetCache?.preload(_currentProfile.value)
    }

    private suspend fun load() {
        val savedProfiles = storage.loadProfiles()
        val finalProfiles: List<Profile>

        // IDs fixes pour les profils par défaut (pour éviter les duplications)
        val defaultProfileIds = mapOf(
            "Utilisateur" to "profile_default_user",
            "Gamer" to "profile_default_gaming",
            "Streamer" to "profile_default_streaming",
            "Discord" to "profile_default_discord"
        )

        if (savedProfiles.isEmpty()) {
            // Créer plusieurs profils par défaut basés sur les templates
            val defaultProfiles = createDefaultProfiles()
            defaultProfiles.forEach { storage.save(it) }
            finalProfiles = defaultProfiles
        } else {
            // Vérifier si les profils par défaut existent (par ID ou par nom)
            val existingIds = savedProfiles.map { it.id }.toSet()
            val existingNames = savedProfiles.map { it.name }.toSet()
            val defaultProfileNames = setOf("Utilisateur", "Gamer", "Streamer", "Discord")

            // Trouver les profils par défaut manquants (vérifier par ID ET par nom)
            val missingDefaults = defaultProfileNames.filter { name ->
                val expectedId = defaultProfileIds[name]
                // Manquant si ni l'ID ni le nom n'existent
                expectedId != null && expectedId !in existingIds && name !in existingNames
            }

            if (missingDefaults.isNotEmpty()) {
                // Créer uniquement les profils par défaut manquants avec des IDs fixes
                val defaultProfiles = createDefaultProfiles()
                val profilesToAdd = defaultProfiles.filter { profile ->
                    profile.name in missingDefaults && defaultProfileIds[profile.name] == profile.id
                }
                profilesToAdd.forEach { storage.save(it) }
                finalProfiles = savedProfiles + profilesToAdd
            } else {
                finalProfiles = savedProfiles
            }
        }

        // Dédupliquer les profils par ID (au cas où il y aurait des doublons)
        // Aussi dédupliquer par nom si plusieurs profils ont le même nom (garder le plus récent)
        val uniqueProfilesById = finalProfiles.distinctBy { it.id }
        val uniqueProfiles = uniqueProfilesById.groupBy { it.name }
            .mapValues { (_, profiles) ->
                // Si plusieurs profils ont le même nom, garder celui avec l'ID par défaut ou le plus récent
                profiles.sortedByDescending { profile ->
                    // Prioriser les IDs par défaut
                    if (profile.id.startsWith("profile_default_")) 1 else 0
                }.first()
            }
            .values
            .toList()

        // Mettre à jour les StateFlow
        _profiles.value = uniqueProfiles

        // Sélectionner le premier profil si aucun n'est sélectionné ou si le profil actuel n'existe plus
        val currentId = _currentProfile.value?.id
        if (currentId == null || uniqueProfiles.none { it.id == currentId }) {
            _currentProfile.value = uniqueProfiles.firstOrNull()
        }
    }

    /**
     * Crée les profils par défaut basés sur les templates
     * Ces profils sont créés au premier démarrage de l'application
     * Utilise des IDs fixes pour éviter les duplications lors des mises à jour
     */
    private fun createDefaultProfiles(): List<Profile> {
        val nameProvider: (String) -> String = { key ->
            // Utiliser les clés de ressources ou les noms par défaut
            when (key) {
                "template_user_name" -> "Utilisateur"
                "template_gaming_name" -> "Gamer"
                "template_streaming_name" -> "Streamer"
                "template_discord_name" -> "Discord"
                "template_audio_production_name" -> "Production Audio"
                "template_productivity_name" -> "Productivité"
                else -> key
            }
        }

        // Mapping des templates vers leurs IDs fixes
        val templateToIdMap = mapOf(
            "template_user" to "profile_default_user",
            "template_gaming" to "profile_default_gaming",
            "template_streaming" to "profile_default_streaming",
            "template_discord" to "profile_default_discord"
        )

        // Créer les 4 profils par défaut principaux
        val templates = listOf(
            ProfileTemplates.userTemplate(nameProvider),
            ProfileTemplates.gamingTemplate(nameProvider),
            ProfileTemplates.streamingTemplate(nameProvider),
            ProfileTemplates.discordTemplate(nameProvider)
        )

        // Convertir les templates en profils avec des IDs fixes (pas de timestamp)
        // Cela évite les duplications lors des mises à jour
        return templates.map { template ->
            val fixedId = templateToIdMap[template.id]
                ?: "profile_default_${template.id.replace("template_", "")}"
            val profile = template.copy(
                id = fixedId,
                name = template.name
            )
            profile.copy(checksum = serializer.checksum(profile))
        }
    }

    fun selectProfile(id: String) {
        scope.launch {
            val selected = _profiles.value.firstOrNull { it.id == id }
            _currentProfile.value = selected

            // Mettre à jour l'état de synchronisation
            _syncState.value = ProfileSyncState(
                profileId = id,
                status = ProfileSyncState.SyncStatus.IDLE
            )

            // Envoyer le profil complet au serveur pour activer les mappings
            if (selected != null && webSocketClient != null) {
                sendProfileToServer(selected, retryCount = 0)
            }
        }
    }

    /**
     * Envoie un profil au serveur avec retry automatique en cas d'échec
     */
    private suspend fun sendProfileToServer(profile: Profile, retryCount: Int = 0) {
        val maxRetries = 3
        val baseDelayMs = 1000L // 1 seconde de base

        // Vérifier que le WebSocket est connecté
        val connectionState = webSocketClient?.state?.value
        if (connectionState !is ConnectionState.Connected) {
            _syncState.value = ProfileSyncState(
                profileId = profile.id,
                status = ProfileSyncState.SyncStatus.ERROR,
                lastSyncAttempt = System.currentTimeMillis(),
                errorMessage = "WebSocket non connecté",
                retryCount = retryCount
            )

            // Retry automatique si la connexion se rétablit
            if (retryCount < maxRetries) {
                scope.launch {
                    // Attendre que la connexion soit rétablie (max 10 secondes)
                    var waited = 0L
                    while (waited < 10000 && webSocketClient?.state?.value !is ConnectionState.Connected) {
                        delay(500)
                        waited += 500
                    }

                    if (webSocketClient?.state?.value is ConnectionState.Connected) {
                        sendProfileToServer(profile, retryCount + 1)
                    }
                }
            }
            return
        }

        val messageId = UUID.randomUUID().toString()
        val sentAt = SystemClock.elapsedRealtime()

        // Mettre à jour l'état de synchronisation
        _syncState.value = ProfileSyncState(
            profileId = profile.id,
            status = if (retryCount > 0) ProfileSyncState.SyncStatus.RETRYING else ProfileSyncState.SyncStatus.SYNCING,
            lastSyncAttempt = System.currentTimeMillis(),
            retryCount = retryCount
        )

        // Stocker la tentative en attente
        pendingProfileSyncs[messageId] = ProfileSyncAttempt(
            profileId = profile.id,
            messageId = messageId,
            profile = profile,
            sentAt = sentAt,
            retryCount = retryCount
        )

        // Sérialiser le profil en JSON string
        val profileJsonString = serializer.export(profile)
        // Créer le payload avec le profil comme objet JSON imbriqué
        val payload = """
        {
            "kind": "profile:select",
            "profileId": "${profile.id}",
            "profile": $profileJsonString,
            "resetState": true,
            "messageId": "$messageId",
            "sentAt": $sentAt
        }
        """.trimIndent()

        webSocketClient?.send(payload, messageId = messageId)

        // Timeout : si pas de réponse après 5 secondes, retry
        scope.launch {
            delay(5000) // 5 secondes de timeout

            // Vérifier si l'ACK a été reçu
            if (pendingProfileSyncs.containsKey(messageId)) {
                // Pas de réponse, retry si possible
                pendingProfileSyncs.remove(messageId)

                if (retryCount < maxRetries) {
                    val delayMs = (baseDelayMs * 2.0.pow(retryCount)).toLong().coerceAtMost(10000)
                    delay(delayMs)
                    sendProfileToServer(profile, retryCount + 1)
                } else {
                    // Max retries atteint
                    _syncState.value = ProfileSyncState(
                        profileId = profile.id,
                        status = ProfileSyncState.SyncStatus.ERROR,
                        lastSyncAttempt = System.currentTimeMillis(),
                        errorMessage = "Timeout après ${maxRetries + 1} tentatives",
                        retryCount = retryCount
                    )
                }
            }
        }
    }

    /**
     * Gère la réception d'un ACK de sélection de profil
     */
    private fun handleProfileSelectAck(ack: AckPayload) {
        scope.launch {
            val attempt = pendingProfileSyncs.remove(ack.messageId)

            if (attempt != null) {
                when (ack.status) {
                    "ok" -> {
                        // Synchronisation réussie
                        _syncState.value = ProfileSyncState(
                            profileId = attempt.profileId,
                            status = ProfileSyncState.SyncStatus.SUCCESS,
                            lastSyncAttempt = System.currentTimeMillis(),
                            lastSyncSuccess = System.currentTimeMillis(),
                            retryCount = attempt.retryCount
                        )
                    }
                    "error" -> {
                        // Erreur de synchronisation
                        val errorMsg = ack.error ?: "Erreur inconnue"

                        // Retry automatique si possible
                        if (attempt.retryCount < 3) {
                            val baseDelayMs = 1000L
                            val delayMs = (baseDelayMs * 2.0.pow(attempt.retryCount)).toLong().coerceAtMost(10000)
                            delay(delayMs)
                            sendProfileToServer(attempt.profile, attempt.retryCount + 1)
                        } else {
                            // Max retries atteint
                            _syncState.value = ProfileSyncState(
                                profileId = attempt.profileId,
                                status = ProfileSyncState.SyncStatus.ERROR,
                                lastSyncAttempt = System.currentTimeMillis(),
                                errorMessage = errorMsg,
                                retryCount = attempt.retryCount
                            )
                        }
                    }
                }
            }
        }
    }

    fun upsertProfile(profile: Profile) {
        scope.launch {
            val updated = _profiles.value.toMutableList()
            val existingIndex = updated.indexOfFirst { it.id == profile.id }
            if (existingIndex >= 0) {
                updated[existingIndex] = profile
            } else {
                updated.add(profile)
            }
            storage.save(profile.copy(checksum = serializer.checksum(profile)))
            _profiles.value = updated
            _currentProfile.value = profile
        }
    }

    fun updateControl(control: Control) {
        scope.launch {
            val profile = _currentProfile.value ?: return@launch
            val updatedControls = profile.controls.toMutableList()
            val index = updatedControls.indexOfFirst { it.id == control.id }
            if (index >= 0) updatedControls[index] = control else updatedControls.add(control)
            val updatedProfile = profile.copy(controls = updatedControls)
            upsertProfile(updatedProfile)
        }
    }

    fun removeControl(controlId: String) {
        scope.launch {
            val profile = _currentProfile.value ?: return@launch
            val updatedProfile = profile.copy(controls = profile.controls.filterNot { it.id == controlId })
            upsertProfile(updatedProfile)
        }
    }

    /**
     * Valide un profil avant import
     */
    suspend fun validateProfile(jsonPayload: String): com.androidcontroldeck.data.storage.ProfileValidationResult {
        // Valider la syntaxe JSON d'abord
        val (isValidJson, jsonError) = validator.validateJsonSyntax(jsonPayload)
        if (!isValidJson) {
            return com.androidcontroldeck.data.storage.ProfileValidationResult(
                isValid = false,
                errors = listOf(
                    com.androidcontroldeck.data.storage.ValidationError(
                        field = "json",
                        message = jsonError ?: "JSON invalide"
                    )
                )
            )
        }

        // Parser et valider le profil
        return try {
            val profile = serializer.import(jsonPayload)
            validator.validate(profile)
        } catch (e: Exception) {
            com.androidcontroldeck.data.storage.ProfileValidationResult(
                isValid = false,
                errors = listOf(
                    com.androidcontroldeck.data.storage.ValidationError(
                        field = "profile",
                        message = "Erreur lors du parsing du profil: ${e.message}"
                    )
                )
            )
        }
    }

    suspend fun importProfile(jsonPayload: String): Profile {
        // Valider avant d'importer
        val validationResult = validateProfile(jsonPayload)
        if (!validationResult.isValid) {
            val errorMessages = validationResult.errors.joinToString("\n") { "${it.field}: ${it.message}" }
            throw IllegalArgumentException("Le profil n'est pas valide:\n$errorMessages")
        }

        val profile = serializer.import(jsonPayload)
        upsertProfile(profile)
        return profile
    }

    /**
     * Importe un profil avec gestion des conflits
     */
    suspend fun importProfileWithConflictResolution(
        jsonPayload: String,
        onConflict: (existing: Profile, imported: Profile) -> ImportResolution
    ): Profile {
        val validationResult = validateProfile(jsonPayload)
        if (!validationResult.isValid) {
            val errorMessages = validationResult.errors.joinToString("\n") { "${it.field}: ${it.message}" }
            throw IllegalArgumentException("Le profil n'est pas valide:\n$errorMessages")
        }

        val importedProfile = serializer.import(jsonPayload)
        val existingProfile = _profiles.value.firstOrNull { it.id == importedProfile.id }

        return if (existingProfile != null) {
            when (val resolution = onConflict(existingProfile, importedProfile)) {
                is ImportResolution.Replace -> {
                    upsertProfile(importedProfile)
                    importedProfile
                }
                is ImportResolution.Rename -> {
                    val renamedProfile = importedProfile.copy(id = resolution.newId, name = resolution.newName)
                    upsertProfile(renamedProfile)
                    renamedProfile
                }
                is ImportResolution.Merge -> {
                    // Fusionner les contrôles (simple: ajouter les nouveaux contrôles)
                    val mergedControls = existingProfile.controls.toMutableList()
                    val existingIds = existingProfile.controls.map { it.id }.toSet()
                    importedProfile.controls.forEach { control ->
                        if (control.id !in existingIds) {
                            mergedControls.add(control)
                        }
                    }
                    val mergedProfile = existingProfile.copy(controls = mergedControls)
                    upsertProfile(mergedProfile)
                    mergedProfile
                }
                is ImportResolution.Cancel -> {
                    throw IllegalStateException("Import annulé par l'utilisateur")
                }
            }
        } else {
            upsertProfile(importedProfile)
            importedProfile
        }
    }

    sealed class ImportResolution {
        data object Replace : ImportResolution()
        data class Rename(val newId: String, val newName: String) : ImportResolution()
        data object Merge : ImportResolution()
        data object Cancel : ImportResolution()
    }

    suspend fun exportProfile(profileId: String): String? {
        val profile = _profiles.value.firstOrNull { it.id == profileId } ?: return null
        return serializer.export(profile)
    }

    fun deleteProfile(profileId: String) {
        scope.launch {
            val profile = _profiles.value.firstOrNull { it.id == profileId } ?: return@launch
            storage.delete(profileId)
            val updated = _profiles.value.filterNot { it.id == profileId }
            _profiles.value = updated
            // Si le profil supprimé était le profil actuel, sélectionner le premier disponible
            if (_currentProfile.value?.id == profileId) {
                _currentProfile.value = updated.firstOrNull()
            }
        }
    }

    fun duplicateProfile(profile: Profile) {
        scope.launch {
            val duplicated = ProfileDuplicationUtils.duplicateProfile(profile)
            upsertProfile(duplicated)
        }
    }

    private fun defaultProfile(): Profile {
        val controls = listOf(
            Control(
                id = "btn_obs_start",
                type = ControlType.BUTTON,
                row = 0,
                col = 0,
                label = stringProvider(R.string.default_control_start_stream),
                colorHex = "#FF5722",
                icon = null,
                action = Action(type = ActionType.OBS, payload = "StartStreaming")
            ),
            Control(
                id = "fader_audio",
                type = ControlType.FADER,
                row = 1,
                col = 0,
                label = stringProvider(R.string.default_control_mic_aux),
                minValue = 0f,
                maxValue = 100f,
                action = Action(type = ActionType.AUDIO, payload = "mic")
            ),
            Control(
                id = "pad_scene",
                type = ControlType.PAD,
                row = 0,
                col = 1,
                rowSpan = 2,
                colSpan = 2,
                label = stringProvider(R.string.default_control_scene),
                colorHex = "#03A9F4",
                action = Action(type = ActionType.OBS, payload = "SwitchScene")
            )
        )
        val baseProfile = Profile(
            id = "default_profile",
            name = stringProvider(R.string.default_profile_name),
            rows = 3,
            cols = 5,
            controls = controls,
            version = 1
        )
        return baseProfile.copy(checksum = serializer.checksum(baseProfile))
    }
}
