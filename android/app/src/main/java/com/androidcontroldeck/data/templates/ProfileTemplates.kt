package com.androidcontroldeck.data.templates

import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile

/**
 * Templates de profils prédéfinis pour différents cas d'usage
 * Icônes disponibles: Play, Pause, SkipBack, SkipForward, Volume2, VolumeX, Mic, MicOff,
 * Monitor, Camera, Radio, Save, FolderOpen, Printer, Copy, Clipboard, Undo2, Redo2,
 * Lock, Sun, AppWindow, Gamepad2, Headphones, Music, Film, Settings, Power, RefreshCw
 */
object ProfileTemplates {

    /**
     * Template Mixage Audio Windows - Contrôle du volume par application
     * Premier template car le plus utile pour un usage quotidien
     */
    fun mixerTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_mixer",
            name = nameProvider("template_mixer_name"),
            rows = 2,
            cols = 6,
            version = 1,
            controls = listOf(
                // Ligne 1 : Faders de volume par application
                Control(
                    id = "fader_master",
                    type = ControlType.FADER,
                    row = 0,
                    col = 0,
                    rowSpan = 2,
                    label = nameProvider("template_master"),
                    icon = "Volume2",
                    colorHex = "#22C55E",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                ),
                Control(
                    id = "fader_app1",
                    type = ControlType.FADER,
                    row = 0,
                    col = 1,
                    rowSpan = 2,
                    label = nameProvider("template_app_1"),
                    icon = "AppWindow",
                    colorHex = "#3B82F6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "chrome.exe", "volume": 50}""")
                ),
                Control(
                    id = "fader_app2",
                    type = ControlType.FADER,
                    row = 0,
                    col = 2,
                    rowSpan = 2,
                    label = nameProvider("template_app_2"),
                    icon = "Music",
                    colorHex = "#8B5CF6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "spotify.exe", "volume": 50}""")
                ),
                Control(
                    id = "fader_app3",
                    type = ControlType.FADER,
                    row = 0,
                    col = 3,
                    rowSpan = 2,
                    label = nameProvider("template_app_3"),
                    icon = "Headphones",
                    colorHex = "#5865F2",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "discord.exe", "volume": 50}""")
                ),
                Control(
                    id = "fader_app4",
                    type = ControlType.FADER,
                    row = 0,
                    col = 4,
                    rowSpan = 2,
                    label = nameProvider("template_app_4"),
                    icon = "Gamepad2",
                    colorHex = "#EF4444",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "Game", "volume": 50}""")
                ),
                Control(
                    id = "btn_mute_master",
                    type = ControlType.TOGGLE,
                    row = 0,
                    col = 5,
                    label = nameProvider("template_mute"),
                    icon = "VolumeX",
                    colorHex = "#EF4444",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "MUTE"}""")
                ),
                Control(
                    id = "btn_switch_output",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 5,
                    label = nameProvider("template_output"),
                    icon = "RefreshCw",
                    colorHex = "#06B6D4",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEFAULT_DEVICE", "deviceId": "default", "deviceType": "output"}""")
                )
            )
        )
    }

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
                    label = nameProvider("template_go_live"),
                    icon = "Radio",
                    colorHex = "#EF4444",
                    action = Action(type = ActionType.OBS, payload = "StartStreaming")
                ),
                Control(
                    id = "btn_stop_stream",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_end"),
                    icon = "Power",
                    colorHex = "#6B7280",
                    action = Action(type = ActionType.OBS, payload = "StopStreaming")
                ),
                Control(
                    id = "btn_record",
                    type = ControlType.TOGGLE,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_rec"),
                    icon = "Film",
                    colorHex = "#F97316",
                    action = Action(type = ActionType.OBS, payload = "ToggleRecording")
                ),
                Control(
                    id = "btn_mute_mic",
                    type = ControlType.TOGGLE,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_mic"),
                    icon = "MicOff",
                    colorHex = "#FBBF24",
                    action = Action(type = ActionType.OBS, payload = """{"action": "MUTE", "params": {"inputName": "Mic/Aux"}}""")
                ),
                Control(
                    id = "btn_mute_desktop",
                    type = ControlType.TOGGLE,
                    row = 0,
                    col = 4,
                    label = nameProvider("template_desktop"),
                    icon = "VolumeX",
                    colorHex = "#FBBF24",
                    action = Action(type = ActionType.OBS, payload = """{"action": "MUTE", "params": {"inputName": "Desktop Audio"}}""")
                ),

                // Ligne 2 : Scènes OBS
                Control(
                    id = "pad_scene1",
                    type = ControlType.PAD,
                    row = 1,
                    col = 0,
                    label = nameProvider("template_cam"),
                    icon = "Camera",
                    colorHex = "#3B82F6",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Camera"}}""")
                ),
                Control(
                    id = "pad_scene2",
                    type = ControlType.PAD,
                    row = 1,
                    col = 1,
                    label = nameProvider("template_screen"),
                    icon = "Monitor",
                    colorHex = "#22C55E",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Screen"}}""")
                ),
                Control(
                    id = "pad_scene3",
                    type = ControlType.PAD,
                    row = 1,
                    col = 2,
                    label = nameProvider("template_game"),
                    icon = "Gamepad2",
                    colorHex = "#8B5CF6",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Game"}}""")
                ),
                Control(
                    id = "pad_scene4",
                    type = ControlType.PAD,
                    row = 1,
                    col = 3,
                    label = nameProvider("template_brb"),
                    icon = "Clock",
                    colorHex = "#F97316",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "BRB"}}""")
                ),
                Control(
                    id = "pad_scene5",
                    type = ControlType.PAD,
                    row = 1,
                    col = 4,
                    label = nameProvider("template_end_screen"),
                    icon = "X",
                    colorHex = "#6B7280",
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_SCENE", "params": {"sceneName": "Ending"}}""")
                ),

                // Ligne 3 : Contrôles audio
                Control(
                    id = "fader_mic",
                    type = ControlType.FADER,
                    row = 2,
                    col = 0,
                    colSpan = 2,
                    label = nameProvider("template_mic"),
                    icon = "Mic",
                    colorHex = "#22C55E",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_VOLUME", "params": {"inputName": "Mic/Aux", "volumeDb": 0}}""")
                ),
                Control(
                    id = "fader_music",
                    type = ControlType.FADER,
                    row = 2,
                    col = 2,
                    colSpan = 2,
                    label = nameProvider("template_music"),
                    icon = "Music",
                    colorHex = "#8B5CF6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "spotify.exe", "volume": 50}""")
                ),
                Control(
                    id = "fader_alerts",
                    type = ControlType.FADER,
                    row = 2,
                    col = 4,
                    label = nameProvider("template_alerts"),
                    icon = "Bell",
                    colorHex = "#FBBF24",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.OBS, payload = """{"action": "SET_VOLUME", "params": {"inputName": "Alerts", "volumeDb": 0}}""")
                )
            )
        )
    }

    /**
     * Template pour la production audio - Table de mixage complète
     */
    fun audioProductionTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_audio_production",
            name = nameProvider("template_audio_production_name"),
            rows = 3,
            cols = 5,
            version = 1,
            controls = listOf(
                // Faders de volume principaux
                Control(
                    id = "fader_master",
                    type = ControlType.FADER,
                    row = 0,
                    col = 0,
                    rowSpan = 3,
                    label = nameProvider("template_master"),
                    icon = "Volume2",
                    colorHex = "#22C55E",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                ),
                Control(
                    id = "fader_track1",
                    type = ControlType.FADER,
                    row = 0,
                    col = 1,
                    rowSpan = 2,
                    label = nameProvider("template_ch_1"),
                    icon = "Music",
                    colorHex = "#3B82F6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "DAW", "volume": 50}""")
                ),
                Control(
                    id = "fader_track2",
                    type = ControlType.FADER,
                    row = 0,
                    col = 2,
                    rowSpan = 2,
                    label = nameProvider("template_ch_2"),
                    icon = "AppWindow",
                    colorHex = "#8B5CF6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "Browser", "volume": 50}""")
                ),
                Control(
                    id = "fader_mic1",
                    type = ControlType.FADER,
                    row = 0,
                    col = 3,
                    rowSpan = 2,
                    label = nameProvider("template_mic"),
                    icon = "Mic",
                    colorHex = "#EF4444",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEVICE_VOLUME", "device": "default", "volume": 50}""")
                ),
                Control(
                    id = "knob_balance",
                    type = ControlType.KNOB,
                    row = 0,
                    col = 4,
                    label = nameProvider("template_pan"),
                    icon = "Settings",
                    colorHex = "#06B6D4",
                    minValue = -100f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_BALANCE", "balance": 0}""")
                ),
                Control(
                    id = "knob_gain",
                    type = ControlType.KNOB,
                    row = 1,
                    col = 4,
                    label = nameProvider("template_gain"),
                    icon = "Settings",
                    colorHex = "#F97316",
                    minValue = -12f,
                    maxValue = 12f
                ),
                // Boutons de contrôle
                Control(
                    id = "btn_mute_all",
                    type = ControlType.TOGGLE,
                    row = 2,
                    col = 1,
                    label = nameProvider("template_mute"),
                    icon = "VolumeX",
                    colorHex = "#EF4444",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "MUTE"}""")
                ),
                Control(
                    id = "btn_device_switch",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 2,
                    label = nameProvider("template_output"),
                    icon = "RefreshCw",
                    colorHex = "#3B82F6",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEFAULT_DEVICE", "deviceId": "default", "deviceType": "output"}""")
                ),
                Control(
                    id = "btn_input_switch",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 3,
                    label = nameProvider("template_input"),
                    icon = "Mic",
                    colorHex = "#8B5CF6",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_DEFAULT_DEVICE", "deviceId": "default", "deviceType": "input"}""")
                ),
                Control(
                    id = "btn_reset",
                    type = ControlType.BUTTON,
                    row = 2,
                    col = 4,
                    label = nameProvider("template_reset"),
                    icon = "RotateCcw",
                    colorHex = "#6B7280",
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_VOLUME", "volume": 50}""")
                )
            )
        )
    }

    /**
     * Template pour le gaming - Raccourcis et Discord
     */
    fun gamingTemplate(nameProvider: (String) -> String): Profile {
        return Profile(
            id = "template_gaming",
            name = nameProvider("template_gaming_name"),
            rows = 2,
            cols = 5,
            version = 1,
            controls = listOf(
                // Ligne 1 : Raccourcis de jeu
                Control(
                    id = "btn_quick_save",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = nameProvider("template_f5"),
                    icon = "Save",
                    colorHex = "#22C55E",
                    action = Action(type = ActionType.KEYBOARD, payload = "F5")
                ),
                Control(
                    id = "btn_quick_load",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 1,
                    label = nameProvider("template_f9"),
                    icon = "FolderOpen",
                    colorHex = "#3B82F6",
                    action = Action(type = ActionType.KEYBOARD, payload = "F9")
                ),
                Control(
                    id = "btn_screenshot",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 2,
                    label = nameProvider("template_capture"),
                    icon = "Camera",
                    colorHex = "#EC4899",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "screenshot", "payload": {"type": "full"}}""")
                ),
                Control(
                    id = "btn_pause",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 3,
                    label = nameProvider("template_esc"),
                    icon = "Pause",
                    colorHex = "#6B7280",
                    action = Action(type = ActionType.KEYBOARD, payload = "ESC")
                ),
                Control(
                    id = "fader_game_volume",
                    type = ControlType.FADER,
                    row = 0,
                    col = 4,
                    rowSpan = 2,
                    label = nameProvider("template_game"),
                    icon = "Gamepad2",
                    colorHex = "#8B5CF6",
                    minValue = 0f,
                    maxValue = 100f,
                    action = Action(type = ActionType.AUDIO, payload = """{"action": "SET_APPLICATION_VOLUME", "application": "Game", "volume": 50}""")
                ),
                // Ligne 2 : Discord + Media
                Control(
                    id = "btn_discord_mute",
                    type = ControlType.TOGGLE,
                    row = 1,
                    col = 0,
                    label = nameProvider("template_mic"),
                    icon = "MicOff",
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "mute", "payload": {"toggle": true}}""")
                ),
                Control(
                    id = "btn_discord_deafen",
                    type = ControlType.TOGGLE,
                    row = 1,
                    col = 1,
                    label = nameProvider("template_deaf"),
                    icon = "Headphones",
                    colorHex = "#5865F2",
                    action = Action(type = ActionType.CUSTOM, payload = """{"plugin": "discord", "action": "deafen", "payload": {"toggle": true}}""")
                ),
                Control(
                    id = "btn_media_play",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 2,
                    label = nameProvider("template_play"),
                    icon = "Play",
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "play_pause"}}""")
                ),
                Control(
                    id = "btn_media_next",
                    type = ControlType.BUTTON,
                    row = 1,
                    col = 3,
                    label = nameProvider("template_next"),
                    icon = "SkipForward",
                    colorHex = "#1DB954",
                    action = Action(type = ActionType.CUSTOM, payload = """{"action": "media", "payload": {"action": "next"}}""")
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


