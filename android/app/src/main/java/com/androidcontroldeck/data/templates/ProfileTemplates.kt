package com.androidcontroldeck.data.templates

import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile

/**
 * Templates de profils prédéfinis pour différents cas d'usage
 */
object ProfileTemplates {

    /**
     * Template pour le streaming avec OBS
     */
    fun streamingTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_streaming",
            name = nameProvider("template_streaming_name"),
            rows = 3,
            cols = 5,
            version = 1,
            controls = listOf(
                // Ligne 1 : Contrôles OBS principaux
                Control(
                    id = "btn_start_stream",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_btn_start_stream"),
                    colorHex = "#FF5722",
                    action = Action(type = ActionType.OBS, payload = "StartStreaming")
                ),
                Control(
                    id = "btn_stop_stream",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_btn_stop_stream"),
                    colorHex = "#F44336",
                    action = Action(type = ActionType.OBS, payload = "StopStreaming")
                ),
                Control(
                    id = "btn_record",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_btn_record"),
                    colorHex = "#E91E63",
                    action = Action(type = ActionType.OBS, payload = "ToggleRecording")
                ),

                // Ligne 2 : Scènes OBS
                Control(
                    id = "pad_scene1",
                    type = ControlType.PAD,
                    row = 1,
                    col = 0,
                    colSpan = 2,
                    label = nameProvider("template_scene_1"),
                    colorHex = "#2196F3",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Scene 1"}}""")
                ),
                Control(
                    id = "pad_scene2",
                    type = ControlType.PAD,
                    row = 1,
                    col = 2,
                    colSpan = 2,
                    label = nameProvider("template_scene_2"),
                    colorHex = "#4CAF50",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Scene 2"}}""")
                ),
                Control(
                    id = "pad_scene3",
                    type = ControlType.PAD,
                    row = 1,
                    col = 4,
                    colSpan = 1,
                    label = nameProvider("template_scene_3"),
                    colorHex = "#FF9800",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Scene 3"}}""")
                ),

                // Ligne 3 : Contrôles audio
                Control(
                    id = "fader_mic",
                    type = ControlType.FADER,
                    row = 2,
                    col = 0,
                    colSpan = 2,
                    label = nameProvider("template_mic_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_VOLUME", "params": {"inputName": "Mic/Aux", "volumeDb": 0}}""")
                ),
                Control(
                    id = "btn_mute_mic",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_mute_mic"),
                    colorHex = "#FFC107",
                    action = Action(type = ActionType.OBS, payload = """{"action": "MUTE", "params": {"inputName": "Mic/Aux"}}""")
                ),
                Control(
                    id = "fader_system",
                    type = ControlType.FADER,
                    row = 2,
                    col = 3,
                    colSpan = 2,
                    label = nameProvider("template_system_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                )
            )
        )
    }

    /**
     * Template pour la production audio
     */
    fun audioProductionTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_audio_production",
            name = nameProvider("template_audio_production_name"),
            rows = 4,
            cols = 4,
            version = 1,
            controls = listOf(
                // Faders de volume pour différentes sources
                Control(
                    id = "fader_master",
                    type = ControlType.FADER,
                    row = 0,
                    col = 0,
                    colSpan = 2,
                    rowSpan = 2,
                    label = nameProvider("template_master_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                ),
                Control(
                    id = "fader_track1",
                    type = ControlType.FADER,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_track_1"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "DAW", "volume": 50}""")
                ),
                Control(
                    id = "fader_track2",
                    type = ControlType.FADER,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_track_2"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "Browser", "volume": 50}""")
                ),
                Control(
                    id = "fader_mic1",
                    type = ControlType.FADER,
                    row = 1,
                    col = 2,
                    label = nameProvider("template_mic_1"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEVICE_VOLUME", "device": "default", "volume": 50}""")
                ),
                Control(
                    id = "fader_mic2",
                    type = ControlType.FADER,
                    row = 1,
                    col = 3,
                    label = nameProvider("template_mic_2"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEVICE_VOLUME", "device": "default", "volume": 50}""")
                ),
                // Mute buttons
                Control(
                    id = "btn_mute_all",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 0,
                    label = nameProvider("template_mute_all"),
                    colorHex = "#F44336",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "MUTE"}""")
                ),
                Control(
                    id = "btn_device_switch",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 1,
                    label = nameProvider("template_switch_device"),
                    colorHex = "#2196F3",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEFAULT_DEVICE", "deviceId": "default", "deviceType": "output"}""")
                ),
                // Knobs pour ajustements fins
                Control(
                    id = "knob_balance",
                    type = ControlType.KNOB,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_balance"),
                    minValue = -100f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_BALANCE", "balance": 0}""")
                ),
                Control(
                    id = "knob_high",
                    type = ControlType.KNOB,
                    row = 2,
                    col = 3,
                    label = nameProvider("template_high"),
                    minValue = -12f,
                    maxValue = 12f
                )
            )
        )
    }

    /**
     * Template pour le gaming
     */
    fun gamingTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_gaming",
            name = nameProvider("template_gaming_name"),
            rows = 2,
            cols = 5,
            version = 1,
            controls = listOf(
                // Raccourcis clavier de jeu
                Control(
                    id = "btn_save",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_save"),
                    colorHex = "#4CAF50",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+S")
                ),
                Control(
                    id = "btn_load",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_load"),
                    colorHex = "#2196F3",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+L")
                ),
                Control(
                    id = "btn_quick_save",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_quick_save"),
                    colorHex = "#FF9800",
                    action = Action(type = ActionType.KEYBOARD, payload = "F5")
                ),
                Control(
                    id = "btn_pause",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_pause"),
                    colorHex = "#9E9E9E",
                    action = Action(type = ActionType.KEYBOARD, payload = "ESC")
                ),
                // Discord controls
                Control(
                    id = "btn_discord_mute",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 0,
                    label = nameProvider("template_discord_mute"),
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "mute", "payload": {"toggle": true}}""")
                ),
                Control(
                    id = "btn_discord_deafen",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 1,
                    label = nameProvider("template_discord_deafen"),
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "deafen", "payload": {"toggle": true}}""")
                ),
                // Media controls
                Control(
                    id = "btn_media_play",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 2,
                    label = nameProvider("template_media_play"),
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "play_pause"}}""")
                ),
                Control(
                    id = "btn_media_next",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 3,
                    label = nameProvider("template_media_next"),
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "next"}}""")
                ),
                Control(
                    id = "fader_game_volume",
                    type = ControlType.FADER,
                    row = 1,
                    col = 4,
                    label = nameProvider("template_game_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "Game", "volume": 50}""")
                )
            )
        )
    }

    /**
     * Template pour la productivité
     */
    fun productivityTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_productivity",
            name = nameProvider("template_productivity_name"),
            rows = 3,
            cols = 4,
            version = 1,
            controls = listOf(
                // Raccourcis bureautique
                Control(
                    id = "btn_new",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_new"),
                    colorHex = "#2196F3",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+N")
                ),
                Control(
                    id = "btn_open",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_open"),
                    colorHex = "#4CAF50",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+O")
                ),
                Control(
                    id = "btn_save",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_save"),
                    colorHex = "#FF9800",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+S")
                ),
                Control(
                    id = "btn_print",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_print"),
                    colorHex = "#9E9E9E",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+P")
                ),
                // Navigation
                Control(
                    id = "btn_copy",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 0,
                    label = nameProvider("template_copy"),
                    colorHex = "#03A9F4",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+C")
                ),
                Control(
                    id = "btn_paste",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 1,
                    label = nameProvider("template_paste"),
                    colorHex = "#00BCD4",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+V")
                ),
                Control(
                    id = "btn_undo",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 2,
                    label = nameProvider("template_undo"),
                    colorHex = "#FF5722",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+Z")
                ),
                Control(
                    id = "btn_redo",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 3,
                    label = nameProvider("template_redo"),
                    colorHex = "#FF9800",
                    action = Action(type = ActionType.KEYBOARD, payload = "CTRL+Y")
                ),
                // Fenêtres
                Control(
                    id = "btn_screenshot",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 0,
                    label = nameProvider("template_screenshot"),
                    colorHex = "#E91E63",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "screenshot", "payload": {"type": "full"}}""")
                ),
                Control(
                    id = "btn_lock",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 1,
                    label = nameProvider("template_lock"),
                    colorHex = "#607D8B",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "system", "payload": {"action": "lock"}}""")
                ),
                Control(
                    id = "toggle_mute",
                    type = ControlType.TOGGLE,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_mute"),
                    colorHex = "#F44336",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "MUTE"}""")
                ),
                Control(
                    id = "fader_volume",
                    type = ControlType.FADER,
                    row = 2,
                    col = 3,
                    label = nameProvider("template_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                )
            )
        )
    }

    /**
     * Template pour utilisateur lambda (usage général)
     */
    fun userTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_user",
            name = nameProvider("template_user_name"),
            rows = 3,
            cols = 4,
            version = 1,
            controls = listOf(
                // Contrôles média
                Control(
                    id = "btn_play_pause",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_play_pause"),
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "play_pause"}}""")
                ),
                Control(
                    id = "btn_prev",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_prev"),
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "previous"}}""")
                ),
                Control(
                    id = "btn_next",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_next"),
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "next"}}""")
                ),
                Control(
                    id = "toggle_mute",
                    type = ControlType.TOGGLE,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_mute"),
                    colorHex = "#F44336",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "MUTE"}""")
                ),
                // Volume
                Control(
                    id = "fader_volume",
                    type = ControlType.FADER,
                    row = 1,
                    col = 0,
                    colSpan = 4,
                    label = nameProvider("template_volume"),
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                ),
                // Raccourcis système
                Control(
                    id = "btn_screenshot",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 0,
                    label = nameProvider("template_screenshot"),
                    colorHex = "#E91E63",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "screenshot", "payload": {"type": "full"}}""")
                ),
                Control(
                    id = "btn_lock",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 1,
                    label = nameProvider("template_lock"),
                    colorHex = "#607D8B",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "system", "payload": {"action": "lock"}}""")
                ),
                Control(
                    id = "btn_brightness",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_brightness"),
                    colorHex = "#FFC107",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "system", "payload": {"action": "brightness"}}""")
                ),
                Control(
                    id = "btn_apps",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 3,
                    label = nameProvider("template_apps"),
                    colorHex = "#2196F3",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "system", "payload": {"action": "open_apps"}}""")
                )
            )
        )
    }

    /**
     * Template pour Discord
     */
    fun discordTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_discord",
            name = nameProvider("template_discord_name"),
            rows = 3,
            cols = 4,
            version = 1,
            controls = listOf(
                // Contrôles Discord principaux
                Control(
                    id = "btn_discord_mute",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_discord_mute"),
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "mute", "payload": {"toggle": true}}""")
                ),
                Control(
                    id = "btn_discord_deafen",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_discord_deafen"),
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "deafen", "payload": {"toggle": true}}""")
                ),
                Control(
                    id = "btn_discord_leave",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_discord_leave"),
                    colorHex = "#F44336",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "leave_voice", "payload": {}}""")
                ),
                Control(
                    id = "btn_discord_join",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_discord_join"),
                    colorHex = "#4CAF50",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "join_voice", "payload": {"channelId": "default"}}""")
                ),
                // Volume Discord
                Control(
                    id = "fader_discord_volume",
                    type = ControlType.FADER,
                    row = 1,
                    col = 0,
                    colSpan = 2,
                    label = nameProvider("template_discord_volume"),
                    minValue = 0f,
                    maxValue = 200f,
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_volume", "payload": {"volume": 100}}""")
                ),
                Control(
                    id = "fader_discord_mic",
                    type = ControlType.FADER,
                    row = 1,
                    col = 2,
                    colSpan = 2,
                    label = nameProvider("template_discord_mic_volume"),
                    minValue = 0f,
                    maxValue = 200f,
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_mic_volume", "payload": {"volume": 100}}""")
                ),
                // Rich Presence
                Control(
                    id = "btn_presence_streaming",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 0,
                    label = nameProvider("template_presence_streaming"),
                    colorHex = "#9146FF",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_presence", "payload": {"preset": "streaming"}}""")
                ),
                Control(
                    id = "btn_presence_gaming",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 1,
                    label = nameProvider("template_presence_gaming"),
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_presence", "payload": {"preset": "gaming"}}""")
                ),
                Control(
                    id = "btn_presence_working",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_presence_working"),
                    colorHex = "#FF9800",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_presence", "payload": {"preset": "working"}}""")
                ),
                Control(
                    id = "btn_presence_idle",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 3,
                    label = nameProvider("template_presence_idle"),
                    colorHex = "#9E9E9E",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "set_presence", "payload": {"preset": "idle"}}""")
                )
            )
        )
    }

    /**
     * Liste tous les templates disponibles
     */
    fun getAllTemplates(nameProvider: (String) -> String): List<Profile> {
        return listOf(
            userTemplate(nameProvider),
            gamingTemplate(nameProvider),
            streamingTemplate(nameProvider),
            discordTemplate(nameProvider),
            audioProductionTemplate(nameProvider),
            productivityTemplate(nameProvider)
        )
    }

    /**
     * Obtient un template par ID
     */
    fun getTemplateById(templateId: String, nameProvider: (String) -> String): Profile? {
        return when (templateId) {
            "template_user" -> userTemplate(nameProvider)
            "template_gaming" -> gamingTemplate(nameProvider)
            "template_streaming" -> streamingTemplate(nameProvider)
            "template_discord" -> discordTemplate(nameProvider)
            "template_audio_production" -> audioProductionTemplate(nameProvider)
            "template_productivity" -> productivityTemplate(nameProvider)
            else -> null
        }
    }
}


