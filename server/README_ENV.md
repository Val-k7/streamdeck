# Configuration des Variables d'Environnement

Ce document décrit comment configurer le serveur Control Deck avec des variables d'environnement.

## Fichier .env

Créez un fichier `.env` à la racine du répertoire `server/` en copiant `.env.example` :

```bash
cp .env.example .env
```

## Variables Disponibles

### Configuration de Base

- **PORT** (défaut: 4455) : Port d'écoute du serveur
- **HOST** (défaut: 0.0.0.0) : Adresse d'écoute (0.0.0.0 pour toutes les interfaces)
- **DECK_DATA_DIR** : Répertoire de données (profils, config, logs). Par défaut, utilise le répertoire d'exécution.

### Sécurité

- **DECK_TOKEN** : Token par défaut pour l'authentification. **OBLIGATOIRE EN PRODUCTION**
  - Si non défini, un token sécurisé sera généré automatiquement au démarrage
  - Pour générer un token sécurisé : `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`
  - ⚠️ **NE JAMAIS** utiliser "change-me" en production

- **HANDSHAKE_SECRET** : Secret pour le handshake (par défaut utilise DECK_TOKEN)
  - Utilisé pour l'authentification initiale avant l'obtention d'un token

### Découverte

- **SERVER_NAME** : Nom du serveur pour la découverte mDNS
  - Par défaut : `ControlDeck-{hostname}`

### TLS/HTTPS (Optionnel)

- **TLS_KEY_PATH** : Chemin vers la clé privée TLS
- **TLS_CERT_PATH** : Chemin vers le certificat TLS
  - Si les deux sont définis, le serveur utilisera HTTPS/WSS

### Logging

- **LOG_LEVEL** (défaut: info) : Niveau de log
  - Valeurs possibles : `error`, `warn`, `info`, `debug`, `verbose`
  - En production, utiliser `info` ou `warn`

- **NODE_ENV** : Environnement Node.js
  - `production` : Mode production (optimisations, moins de logs)
  - `development` : Mode développement

### OBS WebSocket (Optionnel)

- **OBS_WS_URL** (défaut: ws://localhost:4455) : URL du serveur OBS WebSocket
- **OBS_WS_PASSWORD** : Mot de passe OBS WebSocket (si configuré)

## Exemple de Configuration Production

```env
# Production
NODE_ENV=production
PORT=4455
HOST=0.0.0.0

# Sécurité - GÉNÉRER DES TOKENS SÉCURISÉS
DECK_TOKEN=your-secure-token-here-64-chars-minimum
HANDSHAKE_SECRET=your-handshake-secret-here

# TLS (recommandé en production)
TLS_KEY_PATH=/path/to/private.key
TLS_CERT_PATH=/path/to/certificate.crt

# Logging
LOG_LEVEL=info

# Nom du serveur
SERVER_NAME=ControlDeck-Production
```

## Exemple de Configuration Développement

```env
# Développement
NODE_ENV=development
PORT=4455
HOST=0.0.0.0

# Sécurité (tokens de test)
DECK_TOKEN=dev-token-change-in-production
HANDSHAKE_SECRET=dev-secret

# Logging (plus verbeux)
LOG_LEVEL=debug
```

## Gestion des Secrets

### ⚠️ Sécurité

1. **NE JAMAIS** commiter le fichier `.env` dans Git
2. **NE JAMAIS** utiliser des tokens par défaut ("change-me") en production
3. **TOUJOURS** générer des tokens sécurisés avec `crypto.randomBytes()`
4. **TOUJOURS** utiliser TLS/HTTPS en production
5. **TOUJOURS** limiter l'accès au fichier `.env` (permissions 600)

### Génération de Tokens Sécurisés

```bash
# Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"

# OpenSSL
openssl rand -hex 32

# Python
python -c "import secrets; print(secrets.token_hex(32))"
```

### Rotation des Tokens

Les tokens peuvent être révoqués via l'API `/handshake/revoke` ou `/tokens/revoke`.

Pour générer un nouveau token :

1. Générer un nouveau token sécurisé
2. Mettre à jour `DECK_TOKEN` dans `.env`
3. Redémarrer le serveur
4. Révoquer les anciens tokens si nécessaire

## Dépannage

### Le serveur ne démarre pas

- Vérifier que le port n'est pas déjà utilisé
- Vérifier les permissions sur les fichiers de configuration
- Vérifier les logs pour les erreurs

### Erreurs d'authentification

- Vérifier que `DECK_TOKEN` est défini et correct
- Vérifier que `HANDSHAKE_SECRET` correspond
- Vérifier que les clients utilisent le bon token

### Problèmes TLS

- Vérifier que les chemins vers les certificats sont corrects
- Vérifier que les certificats sont valides et non expirés
- Vérifier les permissions sur les fichiers de clé privée


