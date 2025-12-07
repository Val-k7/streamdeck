# Exemples d'actions Control Deck

Ce document fournit des exemples d'actions pour différents types de contrôles.

## Actions OBS

### Démarrer/Arrêter le streaming

```json
{
  "type": "obs",
  "payload": "START_STREAMING"
}
```

```json
{
  "type": "obs",
  "payload": "STOP_STREAMING"
}
```

### Changer de scène

```json
{
  "type": "obs",
  "payload": {
    "action": "SET_SCENE",
    "params": {
      "sceneName": "Gaming Scene"
    }
  }
}
```

### Contrôle de volume d'une source

```json
{
  "type": "obs",
  "payload": {
    "action": "SET_VOLUME",
    "params": {
      "sourceName": "Microphone",
      "volumeDb": -6.0
    }
  }
}
```

### Contrôle avancé des sources

```json
{
  "type": "obs",
  "payload": {
    "action": "SET_SOURCE_VISIBILITY",
    "params": {
      "sceneName": "Main Scene",
      "sourceName": "Webcam",
      "visible": true
    }
  }
}
```

## Actions Audio

### Contrôle du volume système

```json
{
  "type": "audio",
  "payload": {
    "action": "SET_VOLUME",
    "volume": 75
  }
}
```

### Changer de périphérique audio

```json
{
  "type": "audio",
  "payload": {
    "action": "SET_OUTPUT_DEVICE",
    "deviceName": "Speakers"
  }
}
```

### Mute/Unmute

```json
{
  "type": "audio",
  "payload": {
    "action": "TOGGLE_MUTE"
  }
}
```

## Actions Clavier

### Raccourci clavier simple

```json
{
  "type": "keyboard",
  "payload": {
    "keys": ["Ctrl", "Shift", "M"]
  }
}
```

### Séquence de touches

```json
{
  "type": "keyboard",
  "payload": {
    "keys": ["Ctrl", "C"],
    "delay": 100
  }
}
```

## Actions Plugins

### Discord - Mute/Deafen

```json
{
  "type": "plugin",
  "plugin": "discord",
  "action": "mute",
  "payload": {
    "toggle": true
  }
}
```

```json
{
  "type": "plugin",
  "plugin": "discord",
  "action": "deafen",
  "payload": {
    "toggle": true
  }
}
```

### Discord - Rich Presence

```json
{
  "type": "plugin",
  "plugin": "discord",
  "action": "rich_presence",
  "payload": {
    "preset": "streaming",
    "gameName": "My Game",
    "streamTitle": "Live on Twitch!",
    "streamUrl": "https://twitch.tv/username"
  }
}
```

### Spotify - Contrôle de lecture

```json
{
  "type": "plugin",
  "plugin": "spotify",
  "action": "play_pause"
}
```

```json
{
  "type": "plugin",
  "plugin": "spotify",
  "action": "next"
}
```

```json
{
  "type": "plugin",
  "plugin": "spotify",
  "action": "previous"
}
```

## Actions Système (Windows)

### Verrouiller l'écran

```json
{
  "type": "system",
  "payload": {
    "action": "lock"
  }
}
```

### Mettre en veille

```json
{
  "type": "system",
  "payload": {
    "action": "sleep"
  }
}
```

### Gestion des fenêtres

```json
{
  "type": "system",
  "payload": {
    "action": "minimize_window",
    "windowTitle": "Chrome"
  }
}
```

## Actions Scripts

### Exécuter un script personnalisé

```json
{
  "type": "script",
  "payload": {
    "script": "path/to/script.js",
    "args": ["arg1", "arg2"]
  }
}
```

## Actions Webhook

### Envoyer un webhook

```json
{
  "type": "plugin",
  "plugin": "webhook-sender",
  "action": "send_webhook",
  "payload": {
    "url": "https://webhook.site/your-id",
    "body": {
      "event": "button_pressed",
      "timestamp": "2024-01-01T00:00:00Z"
    }
  }
}
```

## Combinaisons

### Action avec plusieurs étapes

Vous pouvez créer des actions complexes en combinant plusieurs actions dans un script ou en utilisant des actions séquentielles.

### Exemple : Setup de streaming complet

```json
{
  "type": "script",
  "payload": {
    "actions": [
      {
        "type": "obs",
        "payload": "START_STREAMING"
      },
      {
        "type": "plugin",
        "plugin": "discord",
        "action": "rich_presence",
        "payload": {
          "preset": "streaming",
          "gameName": "My Game",
          "streamTitle": "Live Now!"
        }
      },
      {
        "type": "audio",
        "payload": {
          "action": "SET_VOLUME",
          "volume": 80
        }
      }
    ]
  }
}
```





