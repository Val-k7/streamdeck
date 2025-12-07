# Guide d'Installation Production - Control Deck Server

Ce guide décrit comment installer et configurer le serveur Control Deck en production.

## Prérequis

- **Node.js** : Version 20.x ou supérieure
- **Système d'exploitation** : Windows, Linux, ou macOS
- **Réseau** : Accès réseau local (pour la découverte)
- **Ports** :
  - 4455 (HTTP/WebSocket par défaut)
  - 4456 (UDP discovery)

## Installation

### Option 1 : Installation depuis les sources

```bash
# Cloner le dépôt
git clone <repository-url>
cd streamdeck/server

# Installer les dépendances
npm install --production

# Copier la configuration
cp config/server.config.sample.json config/server.config.json

# Créer le fichier .env
cp .env.example .env
# Éditer .env et configurer les variables (voir README_ENV.md)
```

### Option 2 : Installation via package

```bash
# Télécharger le package
# Décompresser
# Exécuter install.sh (Linux/macOS) ou install-windows.ps1 (Windows)
```

## Configuration Initiale

### 1. Configurer les variables d'environnement

Éditez le fichier `.env` :

```env
# Production
NODE_ENV=production
PORT=4455
HOST=0.0.0.0

# Sécurité - GÉNÉRER DES TOKENS SÉCURISÉS
DECK_TOKEN=<générer-un-token-sécurisé>
HANDSHAKE_SECRET=<générer-un-secret-sécurisé>

# TLS (recommandé en production)
TLS_KEY_PATH=/path/to/private.key
TLS_CERT_PATH=/path/to/certificate.crt

# Logging
LOG_LEVEL=info

# Nom du serveur
SERVER_NAME=ControlDeck-Production
```

### 2. Générer des tokens sécurisés

```bash
# Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"

# OpenSSL
openssl rand -hex 32

# Python
python -c "import secrets; print(secrets.token_hex(32))"
```

### 3. Configurer TLS (optionnel mais recommandé)

#### Générer un certificat auto-signé (développement)

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

#### Utiliser un certificat Let's Encrypt (production)

```bash
# Installer certbot
sudo apt-get install certbot

# Obtenir un certificat
sudo certbot certonly --standalone -d votre-domaine.com

# Les certificats seront dans /etc/letsencrypt/live/votre-domaine.com/
```

### 4. Configurer le firewall

```bash
# Linux (ufw)
sudo ufw allow 4455/tcp
sudo ufw allow 4456/udp

# Windows (PowerShell en admin)
New-NetFirewallRule -DisplayName "Control Deck HTTP" -Direction Inbound -LocalPort 4455 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "Control Deck UDP" -Direction Inbound -LocalPort 4456 -Protocol UDP -Action Allow
```

## Démarrage

### Mode développement

```bash
npm start
```

### Mode production (recommandé)

#### Linux/macOS avec systemd

Créer `/etc/systemd/system/control-deck.service` :

```ini
[Unit]
Description=Control Deck Server
After=network.target

[Service]
Type=simple
User=controldeck
WorkingDirectory=/opt/control-deck/server
Environment="NODE_ENV=production"
ExecStart=/usr/bin/node index.js
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Activer le service :

```bash
sudo systemctl daemon-reload
sudo systemctl enable control-deck
sudo systemctl start control-deck
```

#### Windows avec NSSM

```powershell
# Télécharger NSSM
# Installer le service
nssm install ControlDeck "C:\Program Files\nodejs\node.exe" "C:\path\to\server\index.js"
nssm set ControlDeck AppDirectory "C:\path\to\server"
nssm set ControlDeck AppEnvironmentExtra "NODE_ENV=production"
nssm start ControlDeck
```

#### Docker

```bash
docker-compose up -d
```

## Vérification

### Vérifier que le serveur fonctionne

```bash
# Health check
curl http://localhost:4455/health

# Discovery endpoint
curl http://localhost:4455/discovery
```

### Vérifier les logs

```bash
# Linux/macOS
tail -f logs/security-*.log

# Windows
Get-Content logs\security-*.log -Tail 50 -Wait
```

## Mise à Jour

```bash
# Arrêter le service
sudo systemctl stop control-deck  # Linux
# ou
nssm stop ControlDeck  # Windows

# Sauvegarder les données
./scripts/backup.sh  # Linux/macOS
.\scripts\backup.ps1  # Windows

# Mettre à jour le code
git pull
npm install --production

# Redémarrer
sudo systemctl start control-deck  # Linux
nssm start ControlDeck  # Windows
```

## Dépannage

### Le serveur ne démarre pas

1. Vérifier que le port n'est pas déjà utilisé :
   ```bash
   # Linux/macOS
   lsof -i :4455

   # Windows
   netstat -ano | findstr :4455
   ```

2. Vérifier les logs :
   ```bash
   tail -f logs/security-*.log
   ```

3. Vérifier les permissions sur les fichiers de configuration

### Erreurs de connexion

1. Vérifier le firewall
2. Vérifier que le serveur écoute sur la bonne interface (0.0.0.0 pour toutes)
3. Vérifier les tokens dans `.env`

### Problèmes TLS

1. Vérifier que les chemins vers les certificats sont corrects
2. Vérifier que les certificats sont valides et non expirés
3. Vérifier les permissions sur les fichiers de clé privée (600)

## Sécurité Production

### Checklist

- [ ] Tokens sécurisés générés (pas "change-me")
- [ ] TLS/HTTPS configuré
- [ ] Firewall configuré
- [ ] Logs configurés (niveau info ou warn)
- [ ] Service configuré pour redémarrage automatique
- [ ] Sauvegardes configurées
- [ ] Monitoring configuré

### Bonnes Pratiques

1. **Ne jamais** commiter `.env` dans Git
2. **Toujours** utiliser TLS en production
3. **Limiter** l'accès réseau au serveur
4. **Monitorer** les logs régulièrement
5. **Mettre à jour** régulièrement les dépendances
6. **Sauvegarder** régulièrement les profils et configuration

## Support

Pour plus d'aide, consultez :
- `README_ENV.md` : Configuration des variables d'environnement
- `README_DEPLOYMENT.md` : Guide de déploiement avancé
- Issues GitHub : Pour signaler des problèmes


