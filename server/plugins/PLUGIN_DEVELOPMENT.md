# Guide de Développement de Plugins pour Control Deck

Ce guide vous explique comment créer des plugins pour Control Deck.

## Table des matières

1. [Introduction](#introduction)
2. [Structure d'un plugin](#structure-dun-plugin)
3. [Création d'un plugin](#création-dun-plugin)
4. [API du plugin](#api-du-plugin)
5. [Exemples](#exemples)
6. [Bonnes pratiques](#bonnes-pratiques)
7. [Sécurité](#sécurité)

## Introduction

Les plugins permettent d'étendre les fonctionnalités de Control Deck avec des actions personnalisées. Chaque plugin est un module JavaScript qui peut être chargé dynamiquement par le serveur.

## Structure d'un plugin

Un plugin doit avoir la structure suivante :

```
mon-plugin/
├── plugin.json    # Manifest du plugin
└── index.js       # Code principal du plugin
```

### plugin.json

Le manifest définit les métadonnées du plugin :

```json
{
  "name": "mon-plugin",
  "version": "1.0.0",
  "description": "Description du plugin",
  "main": "index.js",
  "author": "Votre nom",
  "license": "MIT",
  "actions": [
    {
      "name": "mon-action",
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

### index.js

Le fichier principal exporte une fonction par défaut qui crée l'instance du plugin :

```javascript
export default function createPlugin({ name, version, config, logger, pluginDir, sandbox }) {
  // Votre code ici
}
```

## Création d'un plugin

### 1. Copier le template

Copiez le dossier `plugins/template/` et renommez-le avec le nom de votre plugin.

### 2. Modifier le manifest

Éditez `plugin.json` avec les informations de votre plugin.

### 3. Implémenter les actions

Dans `index.js`, implémentez la fonction `handleAction` :

```javascript
async function handleAction(action, payload) {
  switch (action) {
    case 'mon-action':
      return await maFonction(payload)
    default:
      throw new Error(`Unknown action: ${action}`)
  }
}
```

### 4. Tester le plugin

Placez votre plugin dans le répertoire `plugins/` et redémarrez le serveur.

## API du plugin

### Paramètres de création

- `name` : Nom du plugin
- `version` : Version du plugin
- `config` : Configuration du plugin
- `logger` : Logger Winston pour les logs
- `pluginDir` : Chemin du répertoire du plugin
- `sandbox` : Instance du sandbox (pour sécurité)

### Méthodes requises

#### `onLoad()`
Appelée lors du chargement du plugin. Utilisez cette méthode pour initialiser le plugin.

#### `onUnload()`
Appelée lors du déchargement du plugin. Utilisez cette méthode pour nettoyer les ressources.

#### `handleAction(action, payload)`
Gère les actions du plugin. Doit retourner une Promise.

### Méthodes optionnelles

#### `onConfigUpdate(newConfig)`
Appelée lors de la mise à jour de la configuration.

## Exemples

### Hello World

Voir `plugins/examples/hello-world/` pour un exemple simple.

### Webhook

Voir `plugins/examples/webhook/` pour un exemple d'envoi de requêtes HTTP.

## Bonnes pratiques

1. **Gestion d'erreurs** : Toujours gérer les erreurs proprement
2. **Logging** : Utiliser le logger fourni pour les messages importants
3. **Validation** : Valider les entrées avant de les utiliser
4. **Configuration** : Utiliser la configuration pour la personnalisation
5. **Documentation** : Documenter vos actions et paramètres

## Sécurité

Les plugins sont exécutés dans un sandbox qui :
- Limite l'utilisation de la mémoire
- Impose des timeouts
- Valide les entrées
- Sanitize les chaînes
- Limite les erreurs

### Limitations

- Pas d'accès direct au système de fichiers (sauf via API)
- Pas d'accès réseau direct (sauf via API)
- Timeouts sur toutes les opérations
- Limite de mémoire

### Validation des entrées

Utilisez le sandbox pour valider les entrées :

```javascript
const validated = sandbox.validateInput(payload, schema)
```

## Ressources

- Template : `plugins/template/`
- Exemples : `plugins/examples/`
- Documentation API : Voir le code source de `PluginManager.js`





