# Exemples de Plugins Control Deck

Ce répertoire contient des exemples de plugins pour vous aider à créer vos propres plugins.

## Plugins disponibles

### 1. Hello World (`hello-world`)
Un plugin simple qui démontre les fonctionnalités de base :
- Chargement/déchargement
- Gestion d'actions
- Configuration

**Utilisation :**
```json
{
  "action": "hello",
  "payload": {
    "name": "Control Deck"
  }
}
```

### 2. Webhook (`webhook`)
Un plugin pour envoyer des requêtes HTTP vers des webhooks :
- Support GET, POST, PUT, DELETE
- Timeout configurable
- Gestion d'erreurs

**Utilisation :**
```json
{
  "action": "send",
  "payload": {
    "url": "https://example.com/webhook",
    "method": "POST",
    "body": {
      "message": "Hello from Control Deck"
    }
  }
}
```

## Structure d'un plugin

Un plugin doit contenir :
- `plugin.json` : Manifest du plugin
- `index.js` : Code principal du plugin

### Format du manifest (`plugin.json`)

```json
{
  "name": "plugin-name",
  "version": "1.0.0",
  "description": "Description du plugin",
  "main": "index.js",
  "author": "Votre nom",
  "license": "MIT",
  "actions": [
    {
      "name": "action-name",
      "description": "Description de l'action",
      "parameters": {
        "param1": {
          "type": "string",
          "required": true,
          "description": "Description du paramètre"
        }
      }
    }
  ],
  "config": {
    "enabled": true,
    "setting1": "default_value"
  }
}
```

### Structure du code (`index.js`)

```javascript
export default function createPlugin({ name, version, config, logger, pluginDir }) {
  async function onLoad() {
    // Initialisation
  }

  async function onUnload() {
    // Nettoyage
  }

  async function handleAction(action, payload) {
    // Gestion des actions
  }

  function onConfigUpdate(newConfig) {
    // Mise à jour de la configuration
  }

  return {
    onLoad,
    onUnload,
    handleAction,
    onConfigUpdate, // Optionnel
  }
}
```

## Créer votre propre plugin

1. Copiez le template depuis `../template/`
2. Modifiez `plugin.json` avec vos informations
3. Implémentez vos actions dans `index.js`
4. Placez votre plugin dans le répertoire `plugins/`
5. Redémarrez le serveur

## Bonnes pratiques

- Gérer les erreurs proprement
- Logger les actions importantes
- Valider les entrées
- Utiliser la configuration pour la personnalisation
- Documenter vos actions et paramètres





