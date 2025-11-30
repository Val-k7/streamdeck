package com.androidcontroldeck.data.repository

import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.ProfileSerializer
import com.androidcontroldeck.data.storage.ProfileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileRepository(
    private val storage: ProfileStorage,
    private val serializer: ProfileSerializer = ProfileSerializer(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    init {
        scope.launch { load() }
    }

    fun refresh() {
        scope.launch { load() }
    }

    private suspend fun load() {
        val savedProfiles = storage.loadProfiles()
        if (savedProfiles.isEmpty()) {
            val default = defaultProfile()
            storage.save(default)
            _profiles.value = listOf(default)
            _currentProfile.value = default
        } else {
            _profiles.value = savedProfiles
            _currentProfile.value = savedProfiles.firstOrNull()
        }
    }

    fun selectProfile(id: String) {
        scope.launch {
            val selected = _profiles.value.firstOrNull { it.id == id }
            _currentProfile.value = selected
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

    suspend fun importProfile(jsonPayload: String): Profile {
        val profile = serializer.import(jsonPayload)
        upsertProfile(profile)
        return profile
    }

    suspend fun exportProfile(profileId: String): String? {
        val profile = _profiles.value.firstOrNull { it.id == profileId } ?: return null
        return serializer.export(profile)
    }

    private fun defaultProfile(): Profile {
        val controls = listOf(
            Control(
                id = "btn_obs_start",
                type = ControlType.BUTTON,
                row = 0,
                col = 0,
                label = "Start Stream",
                colorHex = "#FF5722",
                action = Action(type = ActionType.OBS, payload = "StartStreaming")
            ),
            Control(
                id = "fader_audio",
                type = ControlType.FADER,
                row = 1,
                col = 0,
                label = "Mic/Aux",
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
                label = "Scene",
                colorHex = "#03A9F4",
                action = Action(type = ActionType.OBS, payload = "SwitchScene")
            )
        )
        val baseProfile = Profile(
            id = "default_profile",
            name = "My Deck",
            rows = 3,
            cols = 5,
            controls = controls,
            version = 1
        )
        return baseProfile.copy(checksum = serializer.checksum(baseProfile))
    }
}
