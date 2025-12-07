# Système de Plugins Control Deck

Le système de plugins permet d'étendre les fonctionnalités du serveur Control Deck avec des actions personnalisées.

## Structure d'un Plugin

Un plugin est un dossier contenant :

```
my-plugin/
├── plugin.json      # Manifest du plugin
└── index.js         # Code principal du plugin
```

### plugin.json

```json
{
  "name": "my-plugin",
  "version": "1.0.0",
  "description": "Description du plugin",
  "author": "Votre nom",
  "main": "index.js",
  "actions": {
    "my_action": {
      "description": "Description de l'action",
      "payload": {
        "type": "object",
        "properties": {
          "param1": { "type": "string" }
        }
      }
    }
  },
  "config": {
    "enabled": true,
    "apiKey": ""
  }
}
```

### index.js

```javascript
export default function createPlugin({ name, version, config, logger, pluginDir }) {
  return {
    // Appelé lors du chargement du plugin
    async onLoad() {
      logger.info(`Plugin ${name} loaded`)
      // Initialisation
    },

    // Appelé lors du déchargement du plugin
    async onUnload() {
      logger.info(`Plugin ${name} unloaded`)
      // Nettoyage
    },

    // Gère les actions
    async handleAction(action, payload) {
      switch (action) {
        case 'my_action':
          return await handleMyAction(payload)
        default:
          throw new Error(`Unknown action: ${action}`)
      }
    },

    // Appelé lors de la mise à jour de la configuration
    onConfigUpdate(newConfig) {
      config = newConfig
      // Réagir aux changements de configuration
    }
  }
}

async function handleMyAction(payload) {
  // Implémentation de l'action
  return { success: true }
}
```

## Installation d'un Plugin

1. Créer un dossier dans `server/plugins/`
2. Ajouter `plugin.json` et `index.js`
3. Redémarrer le serveur

## API du Plugin

### Paramètres fournis

- `name`: Nom du plugin
- `version`: Version du plugin
- `config`: Configuration du plugin
- `logger`: Logger Winston pour les logs
- `pluginDir`: Chemin vers le dossier du plugin

### Méthodes disponibles

- `onLoad()`: Appelé au chargement
- `onUnload()`: Appelé au déchargement
- `handleAction(action, payload)`: Gère les actions
- `onConfigUpdate(config)`: Appelé lors de la mise à jour de la config

## Exemples

Voir les dossiers `examples/` pour des exemples de plugins.





