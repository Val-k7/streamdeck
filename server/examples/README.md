# Exemples Control Deck

Ce répertoire contient des exemples et des templates pour vous aider à démarrer avec Control Deck.

## Fichiers disponibles

### `example-profile.json`
Un exemple de profil complet avec différents types de contrôles :
- Boutons pour OBS (start/stop stream, changement de scène)
- Fader pour contrôle audio
- Boutons pour plugins (Discord, Spotify)
- Configuration de style et positionnement

### `example-actions.md`
Documentation complète avec des exemples d'actions pour :
- OBS Studio (streaming, scènes, sources)
- Audio (volume, périphériques, mute)
- Clavier (raccourcis, séquences)
- Plugins (Discord, Spotify)
- Système Windows
- Scripts personnalisés
- Webhooks

## Utilisation

### Importer un profil exemple

1. Copiez `example-profile.json` dans le répertoire `profiles/` de votre serveur
2. Ou importez-le via l'application Android

### Créer votre propre profil

1. Utilisez `example-profile.json` comme base
2. Modifiez les contrôles selon vos besoins
3. Référez-vous à `example-actions.md` pour les actions disponibles

## Structure d'un profil

```json
{
  "id": "unique-profile-id",
  "name": "Profile Name",
  "version": 1,
  "description": "Description",
  "grid": {
    "rows": 3,
    "cols": 5
  },
  "controls": [
    {
      "id": "control-id",
      "type": "button|fader|toggle",
      "label": "Control Label",
      "position": {
        "row": 0,
        "col": 0,
        "rowSpan": 1,
        "colSpan": 1
      },
      "action": {
        "type": "obs|audio|keyboard|plugin|script",
        "payload": {}
      },
      "style": {
        "backgroundColor": "#HEX",
        "textColor": "#HEX",
        "icon": "icon_name"
      }
    }
  ]
}
```

## Besoin d'aide ?

Consultez la documentation complète dans le répertoire `docs/` ou ouvrez une issue sur GitHub.





