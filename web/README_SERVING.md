# Comment servir l'UI Control Deck

L'interface web de Control Deck doit être servie séparément du serveur Windows. Ce document explique les différentes méthodes pour servir l'UI.

## 🚀 Méthodes de service

### Option 1 : Serveur de développement (développement)

Pour le développement, utilisez le serveur de développement Vite :

```bash
cd ui_update
npm install
npm run dev
```

L'UI sera accessible sur `http://localhost:5173` (ou le port indiqué par Vite).

### Option 2 : Build de production + serveur web

#### Étape 1 : Build de production

```bash
cd ui_update
npm install
npm run build
```

Cela génère les fichiers statiques dans `ui_update/dist/`.

#### Étape 2 : Servir avec un serveur web

**Avec Python (simple)** :
```bash
cd ui_update/dist
python -m http.server 8080
```

**Avec Node.js (http-server)** :
```bash
npm install -g http-server
cd ui_update/dist
http-server -p 8080
```

**Avec nginx** :
```nginx
server {
    listen 8080;
    server_name localhost;
    root /chemin/vers/ui_update/dist;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**Avec Apache** :
```apache
<VirtualHost *:8080>
    DocumentRoot /chemin/vers/ui_update/dist
    <Directory /chemin/vers/ui_update/dist>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>
</VirtualHost>
```

### Option 3 : Intégration avec le serveur Windows (non recommandé)

> ⚠️ **Note** : Cette méthode n'est plus supportée. Le serveur Windows ne sert plus l'UI pour des raisons de sécurité et de séparation des responsabilités.

Si vous devez absolument servir l'UI depuis le même domaine que le serveur, utilisez un reverse proxy (nginx, Apache) qui sert l'UI et fait du proxy vers le serveur WebSocket.

## 🔧 Configuration

### Variables d'environnement

Créez un fichier `.env` dans `ui_update/` pour configurer l'URL du serveur :

```env
VITE_SERVER_URL=http://localhost:4455
VITE_WS_URL=ws://localhost:4455
```

### Build avec configuration personnalisée

```bash
VITE_SERVER_URL=http://192.168.1.100:4455 npm run build
```

## 📝 Notes importantes

1. **CORS** : Assurez-vous que le serveur Windows autorise les requêtes depuis l'origine de l'UI (si elles sont sur des domaines différents).

2. **WebSocket** : L'UI doit se connecter au serveur WebSocket. Configurez l'URL WebSocket dans les variables d'environnement.

3. **HTTPS** : En production, servez l'UI via HTTPS pour la sécurité.

4. **Routing** : L'UI utilise le routing côté client (SPA). Assurez-vous que votre serveur web redirige toutes les routes vers `index.html`.

## 🐛 Dépannage

### L'UI ne se connecte pas au serveur

1. Vérifiez que le serveur Windows est démarré
2. Vérifiez l'URL du serveur dans les variables d'environnement
3. Vérifiez les règles CORS du serveur
4. Vérifiez la console du navigateur pour les erreurs

### Erreurs 404 sur les routes

Assurez-vous que votre serveur web redirige toutes les routes vers `index.html` (voir exemples ci-dessus).

### Problèmes de build

```bash
# Nettoyer et rebuilder
rm -rf node_modules dist
npm install
npm run build
```

