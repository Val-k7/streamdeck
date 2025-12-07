# Control Deck Server - Windows

Serveur Windows autonome pour Control Deck. Ce serveur gère les actions système, les profils et la communication WebSocket avec l'application Android.

## 📋 Prérequis

- **Windows** : Windows 10 ou supérieur
- **Node.js** : Version 18+ ([Télécharger Node.js](https://nodejs.org/))
- **PowerShell** : Version 5.1+ (inclus dans Windows 10+)

## 🚀 Installation rapide

### Option 1 : Installation automatique (recommandé)

Ouvrez PowerShell en tant qu'administrateur et exécutez :

```powershell
cd server
.\scripts\install.ps1
npm run setup
```

L'assistant interactif vous guidera pour :
- Choisir le port du serveur (défaut : 4455)
- Configurer le token d'authentification
- Sélectionner le fichier de mapping

### Option 2 : Installation manuelle

1. **Installer les dépendances** :
   ```powershell
   cd server
   npm install
   ```

2. **Configurer le serveur** :
   ```powershell
   npm run setup
   ```

3. **Démarrer le serveur** :
   ```powershell
   npm start
   ```

## ⚙️ Configuration

### Configuration initiale

Lors de la première exécution, le serveur crée automatiquement :
- `config/server.config.json` : Configuration principale
- `config/mappings.json` : Mappings des actions
- `profiles/` : Répertoire des profils
- `logs/` : Répertoire des logs

### Variables d'environnement

Vous pouvez configurer le serveur via des variables d'environnement :

| Variable | Description | Défaut |
|----------|-------------|--------|
| `PORT` | Port du serveur WebSocket | `4455` |
| `HOST` | Interface d'écoute | `0.0.0.0` |
| `DECK_TOKEN` | Token d'authentification par défaut | `change-me` |
| `HANDSHAKE_SECRET` | Secret pour le handshake | Valeur de `DECK_TOKEN` |
| `DECK_DATA_DIR` | Répertoire des données | Répertoire d'exécution |
| `LOG_LEVEL` | Niveau de log (error, warn, info, debug) | `info` |
| `TLS_KEY_PATH` | Chemin vers la clé TLS (optionnel) | - |
| `TLS_CERT_PATH` | Chemin vers le certificat TLS (optionnel) | - |

### Configuration via fichier

Éditez `config/server.config.json` :

```json
{
  "port": 4455,
  "host": "0.0.0.0",
  "defaultToken": "votre-token-securise",
  "handshakeSecret": "votre-secret-handshake"
}
```

## 🎮 Utilisation

### Démarrer le serveur

```powershell
npm start
```

Le serveur démarre sur `http://0.0.0.0:4455` (accessible depuis toutes les interfaces réseau).

### Vérifier l'état du serveur

```powershell
npm run health
```

Ou visitez `http://localhost:4455/health` dans votre navigateur.

### Diagnostics

```powershell
npm run diagnostics
```

Affiche les informations sur :
- État du serveur
- Connexions WebSocket actives
- Plugins chargés
- Métriques de performance

## 🔧 Scripts disponibles

| Script | Description |
|--------|-------------|
| `npm start` | Démarrer le serveur |
| `npm run setup` | Configuration interactive initiale |
| `npm run health` | Vérifier l'état du serveur |
| `npm run validate` | Valider la configuration |
| `npm run check-deps` | Vérifier les dépendances |
| `npm run package` | Créer un exécutable Windows |

## 🔒 Sécurité

### Authentification

Le serveur utilise un système de tokens pour l'authentification :

1. **Handshake initial** : L'application Android effectue un handshake avec le secret configuré
2. **Token temporaire** : Le serveur génère un token valide 24 heures
3. **Validation** : Toutes les connexions WebSocket nécessitent un token valide

### Configuration sécurisée

- Changez le `defaultToken` et `handshakeSecret` dans la configuration
- Utilisez TLS/HTTPS en production (configurez `TLS_KEY_PATH` et `TLS_CERT_PATH`)
- Limitez l'accès réseau via le firewall Windows

## 📁 Structure des répertoires

```
server/
├── actions/          # Actions système (clavier, audio, OBS, etc.)
├── config/           # Configuration (mappings, plugins, tokens)
├── logs/             # Logs du serveur
├── profiles/         # Profils de contrôle
├── plugins/           # Plugins personnalisés
├── scripts/          # Scripts utilitaires
├── utils/            # Utilitaires (logging, rate limiting, etc.)
├── index.js          # Point d'entrée principal
└── package.json      # Dépendances et scripts
```

## 🐛 Dépannage

### Le serveur ne démarre pas

1. Vérifiez que le port n'est pas déjà utilisé :
   ```powershell
   npm run check-ports
   ```

2. Vérifiez les logs dans `logs/security-*.log`

3. Validez la configuration :
   ```powershell
   npm run validate
   ```

### L'application Android ne se connecte pas

1. Vérifiez que le serveur est démarré et accessible
2. Vérifiez l'adresse IP du serveur (utilisez `ipconfig` dans PowerShell)
3. Vérifiez le token d'authentification
4. Vérifiez le firewall Windows (port 4455 doit être ouvert)

### Les actions ne fonctionnent pas

1. Vérifiez `config/mappings.json` pour les mappings des actions
2. Consultez les logs dans `logs/` pour les erreurs
3. Vérifiez que les outils système requis sont installés

## 📚 Documentation

- **Actions disponibles** : Voir `actions/` pour la liste des actions système
- **Plugins** : Voir `plugins/README.md` pour développer des plugins
- **API WebSocket** : Voir la documentation dans le code source

## 🔄 Mise à jour

```powershell
npm run update:server
```

Ou manuellement :

```powershell
git pull
npm install
npm start
```

## 📝 Logs

Les logs sont stockés dans `logs/` avec rotation quotidienne :
- `security-YYYY-MM-DD.log` : Logs de sécurité et d'authentification
- `audit/audit-YYYY-MM-DD.log` : Logs d'audit

## ⚠️ Notes importantes

- Le serveur doit être démarré avant de lancer l'application Android
- Le serveur écoute sur toutes les interfaces réseau (`0.0.0.0`) par défaut
- Les profils sont sauvegardés automatiquement dans `profiles/`
- Les tokens expirent après 24 heures par défaut

## 🤝 Support

Pour plus d'aide, consultez :
- Le README principal du projet
- Les issues GitHub
- La documentation dans `docs/`


