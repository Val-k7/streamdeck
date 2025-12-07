# Guide de Déploiement Production - Control Deck Server

Ce guide décrit les étapes avancées pour déployer le serveur Control Deck en production.

## Architecture Recommandée

```
┌─────────────────┐
│   Load Balancer │ (optionnel)
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼───┐
│Server1│ │Server2│ (optionnel - haute disponibilité)
└───────┘ └───────┘
```

## Déploiement sur Linux

### 1. Préparation du système

```bash
# Mettre à jour le système
sudo apt-get update && sudo apt-get upgrade -y

# Installer Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Créer un utilisateur dédié
sudo useradd -r -s /bin/false controldeck
sudo mkdir -p /opt/control-deck
sudo chown controldeck:controldeck /opt/control-deck
```

### 2. Installation de l'application

```bash
cd /opt/control-deck
sudo -u controldeck git clone <repository-url> server
cd server
sudo -u controldeck npm install --production
```

### 3. Configuration

```bash
# Copier les fichiers de configuration
sudo -u controldeck cp config/server.config.sample.json config/server.config.json
sudo -u controldeck cp .env.example .env

# Éditer la configuration
sudo -u controldeck nano .env
```

### 4. Configuration systemd

Créer `/etc/systemd/system/control-deck.service` :

```ini
[Unit]
Description=Control Deck Server
Documentation=https://github.com/your-repo/control-deck
After=network.target

[Service]
Type=simple
User=controldeck
Group=controldeck
WorkingDirectory=/opt/control-deck/server
Environment="NODE_ENV=production"
EnvironmentFile=/opt/control-deck/server/.env
ExecStart=/usr/bin/node index.js
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=control-deck

# Sécurité
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/control-deck/server/logs /opt/control-deck/server/profiles /opt/control-deck/server/config

[Install]
WantedBy=multi-user.target
```

Activer et démarrer :

```bash
sudo systemctl daemon-reload
sudo systemctl enable control-deck
sudo systemctl start control-deck
sudo systemctl status control-deck
```

## Déploiement sur Windows

### 1. Installation de Node.js

Télécharger et installer Node.js 20.x depuis nodejs.org

### 2. Installation de l'application

```powershell
# Créer le répertoire
New-Item -ItemType Directory -Path "C:\Program Files\Control Deck\Server"
cd "C:\Program Files\Control Deck\Server"

# Cloner ou copier les fichiers
git clone <repository-url> .

# Installer les dépendances
npm install --production
```

### 3. Configuration avec NSSM

```powershell
# Télécharger NSSM depuis nssm.cc
# Extraire dans C:\nssm

# Installer le service
C:\nssm\win64\nssm.exe install ControlDeck "C:\Program Files\nodejs\node.exe" "C:\Program Files\Control Deck\Server\index.js"

# Configurer
C:\nssm\win64\nssm.exe set ControlDeck AppDirectory "C:\Program Files\Control Deck\Server"
C:\nssm\win64\nssm.exe set ControlDeck AppEnvironmentExtra "NODE_ENV=production"
C:\nssm\win64\nssm.exe set ControlDeck DisplayName "Control Deck Server"
C:\nssm\win64\nssm.exe set ControlDeck Description "Control Deck Server - Remote control server"
C:\nssm\win64\nssm.exe set ControlDeck Start SERVICE_AUTO_START
C:\nssm\win64\nssm.exe set ControlDeck AppStdout "C:\Program Files\Control Deck\Server\logs\stdout.log"
C:\nssm\win64\nssm.exe set ControlDeck AppStderr "C:\Program Files\Control Deck\Server\logs\stderr.log"

# Démarrer
C:\nssm\win64\nssm.exe start ControlDeck
```

## Déploiement avec Docker

### Docker Compose

Créer `docker-compose.yml` :

```yaml
version: '3.8'

services:
  control-deck:
    build: .
    container_name: control-deck
    restart: unless-stopped
    ports:
      - "4455:4455"
      - "4456:4456/udp"
    environment:
      - NODE_ENV=production
      - PORT=4455
      - HOST=0.0.0.0
    env_file:
      - .env
    volumes:
      - ./profiles:/app/profiles
      - ./config:/app/config
      - ./logs:/app/logs
    networks:
      - control-deck-network

networks:
  control-deck-network:
    driver: bridge
```

Démarrer :

```bash
docker-compose up -d
```

## Reverse Proxy (Nginx)

### Configuration Nginx

```nginx
upstream control_deck {
    server 127.0.0.1:4455;
}

server {
    listen 80;
    server_name controldeck.example.com;

    # Redirection HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name controldeck.example.com;

    ssl_certificate /etc/letsencrypt/live/controldeck.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/controldeck.example.com/privkey.pem;

    # WebSocket upgrade
    location /ws {
        proxy_pass http://control_deck;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }

    # API endpoints
    location / {
        proxy_pass http://control_deck;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring

### Health Checks

```bash
# Script de monitoring simple
#!/bin/bash
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4455/health)
if [ $response -ne 200 ]; then
    echo "Server is down!"
    systemctl restart control-deck
fi
```

### Logs

```bash
# Surveiller les logs en temps réel
journalctl -u control-deck -f  # Linux systemd
Get-Content logs\security-*.log -Tail 50 -Wait  # Windows
```

## Sauvegardes

### Script de sauvegarde automatique

```bash
#!/bin/bash
BACKUP_DIR="/backups/control-deck"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Sauvegarder les profils
tar -czf $BACKUP_DIR/profiles_$DATE.tar.gz /opt/control-deck/server/profiles

# Sauvegarder la configuration
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /opt/control-deck/server/config

# Nettoyer les sauvegardes de plus de 30 jours
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete
```

Ajouter au crontab :

```bash
0 2 * * * /opt/control-deck/scripts/backup.sh
```

## Haute Disponibilité

### Load Balancing

Utiliser un load balancer (Nginx, HAProxy) pour distribuer la charge entre plusieurs instances.

### Synchronisation des données

Synchroniser les profils entre instances avec :
- Partage de fichiers réseau (NFS, SMB)
- Base de données partagée
- Réplication automatique

## Sécurité Avancée

### Fail2Ban

Protéger contre les attaques par force brute :

```ini
# /etc/fail2ban/jail.local
[control-deck]
enabled = true
port = 4455
filter = control-deck
logpath = /opt/control-deck/server/logs/security-*.log
maxretry = 5
bantime = 3600
```

### Rate Limiting

Le serveur inclut déjà un rate limiter. Configurer dans `server.config.json` :

```json
{
  "rateLimiting": {
    "windowMs": 60000,
    "max": 100
  }
}
```

## Mise à l'Échelle

### Horizontal Scaling

1. Déployer plusieurs instances
2. Utiliser un load balancer
3. Synchroniser les données entre instances

### Vertical Scaling

1. Augmenter les ressources CPU/RAM
2. Optimiser la configuration Node.js
3. Utiliser un process manager (PM2, cluster mode)

## Troubleshooting Avancé

### Performance

```bash
# Analyser les performances
node --prof index.js
node --prof-process isolate-*.log > profile.txt
```

### Mémoire

```bash
# Vérifier les fuites mémoire
node --inspect index.js
# Utiliser Chrome DevTools pour analyser
```

### Réseau

```bash
# Tester la connectivité
netstat -tulpn | grep 4455
ss -tulpn | grep 4455
```

## Support

Pour plus d'aide :
- Documentation : Voir les autres fichiers README
- Issues : GitHub Issues
- Logs : Vérifier `logs/security-*.log`


