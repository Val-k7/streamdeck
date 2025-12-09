# TODO - Réorganisation Control Deck

## 🎯 Objectif

Migrer de Node.js vers Python et réorganiser l'architecture pour que :

- Le **serveur Python** serve directement la **Web UI React**
- L'**app Android** devienne un simple **WebView** affichant l'URL du serveur

---

## 📁 Structure Cible

```
streamdeck/
├── server/                      # Tout le backend + frontend ensemble
│   ├── backend/                 # Serveur Python
│   │   ├── app/
│   │   │   ├── __init__.py
│   │   │   ├── main.py          # Point d'entrée FastAPI
│   │   │   ├── config.py        # Configuration Pydantic
│   │   │   ├── websocket.py     # Handler WebSocket
│   │   │   ├── actions/         # Actions système
│   │   │   │   ├── __init__.py
│   │   │   │   ├── keyboard.py
│   │   │   │   ├── audio.py
│   │   │   │   ├── obs.py
│   │   │   │   ├── scripts.py
│   │   │   │   ├── system.py
│   │   │   │   ├── clipboard.py
│   │   │   │   ├── screenshot.py
│   │   │   │   └── processes.py
│   │   │   ├── routes/          # Routes REST API
│   │   │   │   ├── __init__.py
│   │   │   │   ├── profiles.py
│   │   │   │   ├── tokens.py
│   │   │   │   ├── discovery.py
│   │   │   │   ├── plugins.py
│   │   │   │   └── health.py
│   │   │   ├── plugins/         # Système de plugins
│   │   │   │   ├── __init__.py
│   │   │   │   ├── base.py
│   │   │   │   ├── manager.py
│   │   │   │   ├── discord/
│   │   │   │   ├── obs/
│   │   │   │   └── spotify/
│   │   │   └── utils/           # Utilitaires
│   │   │       ├── __init__.py
│   │   │       ├── logger.py
│   │   │       ├── token_manager.py
│   │   │       ├── rate_limiter.py
│   │   │       ├── cache_manager.py
│   │   │       ├── discovery.py
│   │   │       ├── pairing.py
│   │   │       └── profile_manager.py
│   │   ├── tests/
│   │   │   ├── __init__.py
│   │   │   ├── test_actions/
│   │   │   ├── test_routes/
│   │   │   └── test_websocket.py
│   │   ├── requirements.txt
│   │   └── pyproject.toml
│   ├── frontend/                # UI React (depuis android/web-ui)
│   │   ├── src/
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   ├── pages/
│   │   │   ├── lib/
│   │   │   ├── styles/
│   │   │   ├── App.tsx
│   │   │   └── main.tsx
│   │   ├── public/
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   ├── tsconfig.json
│   │   └── tailwind.config.ts
│   ├── static/                  # UI buildée (généré par vite build)
│   ├── config/                  # Configuration runtime
│   │   ├── server.config.json
│   │   └── mappings/
│   ├── profiles/                # Profils utilisateur
│   ├── logs/                    # Logs serveur
│   ├── Dockerfile
│   └── docker-compose.yml
├── android/                     # Client Android simplifié
│   ├── app/
│   │   └── src/main/
│   │       ├── java/.../
│   │       │   ├── MainActivity.kt
│   │       │   ├── WebViewScreen.kt
│   │       │   └── SettingsScreen.kt
│   │       └── res/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── tests/e2e/                   # Tests end-to-end
├── docs/                        # Documentation
├── AGENTS
├── AGENTS.md
└── README.md
```

---

## ✅ Phase 1 : Préparation

- [x] **1.1** Créer branche `refactor/python-server`
- [x] **1.2** Backup des données existantes (config, profiles, tokens)
  - Copier hors repo (non versionné) : `server/config`, `server/profiles`, `server/logs`, `server/plugins`, `server/config/tokens.json`, `server/config/server.id`.
  - Vérifier droits/secret : ne pas stocker dans Git. Conserver l’arborescence et les permissions.
- [x] **1.3** Documenter les endpoints API actuels à conserver
  - REST : `GET /health`, `GET /discovery`, `POST /pairing/request`, `POST /pairing/confirm`, `GET /pairing/servers`, `POST /handshake`, `POST /handshake/revoke`, `GET /profiles`, `GET /profiles/:id`, `POST /profiles/:id`, `GET /diagnostics`, `GET /performance`, `GET /errors`, `GET /tokens/info`, `POST /tokens/rotate`, `POST /tokens/revoke`, `GET /plugins`, `POST /plugins/:name/enable`, `POST /plugins/:name/disable`, `GET /plugins/:name/config`, `POST /plugins/:name/config`
  - WebSocket : `WS /ws` (auth via token, bypass localhost/same-origin)
- [x] **1.4** Lister toutes les actions Node.js à porter
  - `audio-windows.js`, `audio.js`, `clipboard-windows.js`, `files-windows.js`, `keyboard.js`, `media-windows.js`, `obs.js`, `processes.js`, `screenshot-windows.js`, `scripts.js`, `system-windows.js` (mapping via `/ws` + plugins)

---

## ✅ Phase 2 : Structure serveur Python

### 2.1 Configuration initiale

- [x] Créer `server/backend/pyproject.toml`
- [x] Créer `server/backend/requirements.txt`
- [x] Créer structure de dossiers `server/backend/app/`
- [x] Configurer Python 3.11+ comme version cible

### 2.2 Dépendances Python

```txt
fastapi>=0.109.0
uvicorn[standard]>=0.27.0
websockets>=12.0
pydantic>=2.5.0
pydantic-settings>=2.1.0
python-multipart>=0.0.6
aiofiles>=23.2.0
python-jose[cryptography]>=3.3.0
passlib[bcrypt]>=1.7.4
loguru>=0.7.0
pynput>=1.7.6
pyautogui>=0.9.54
pycaw>=20230407
pillow>=10.0.0
psutil>=5.9.0
zeroconf>=0.131.0
qrcode>=7.4.0
httpx>=0.26.0
```

### 2.3 Fichiers core backend

- [x] `app/__init__.py`
- [x] `app/main.py` - Application FastAPI
- [x] `app/config.py` - Settings Pydantic
- [x] `app/websocket.py` - WebSocket handler (placeholder echo)

---

## ✅ Phase 3 : Porter les Actions

- [x] `actions/keyboard.py` - Simulation clavier (pyautogui hotkey)
- [x] `actions/audio.py` - Contrôle volume (pycaw) (Windows master volume, mute)
- [x] `actions/obs.py` - Intégration OBS WebSocket (HTTP RPC, actions avancées)
- [x] `actions/scripts.py` - Exécution scripts (subprocess)
- [x] `actions/system.py` - Lock/shutdown/restart (Windows)
- [x] `actions/clipboard.py` - Copier/coller (pyperclip requis)
- [x] `actions/screenshot.py` - Capture écran (PIL.ImageGrab)
- [x] `actions/processes.py` - Gestion processus (psutil)

### 3.2 Correspondance Node.js → Python

| Node.js                 | Python                    |
| ----------------------- | ------------------------- |
| `keyboard.js`           | `pynput.keyboard`         |
| `audio-windows.js`      | `pycaw`                   |
| `obs.js`                | `obsws-python` ou `httpx` |
| `scripts.js`            | `subprocess.run()`        |
| `system-windows.js`     | `os`, `ctypes`            |
| `clipboard-windows.js`  | `pyperclip`               |
| `screenshot-windows.js` | `PIL.ImageGrab`           |
| `processes.js`          | `psutil`                  |

---

## ✅ Phase 4 : Porter les Utilitaires

### 4.1 Utils (depuis server/utils/\*.js)

- [x] `utils/logger.py` - Logging avec loguru
- [x] `utils/token_manager.py` - JWT avec python-jose
- [x] `utils/rate_limiter.py` - Rate limiting async
- [x] `utils/cache_manager.py` - Cache LRU
- [x] `utils/discovery.py` - mDNS avec zeroconf
- [x] `utils/pairing.py` - QR code avec qrcode
- [x] `utils/profile_manager.py` - CRUD profils JSON

---

## ✅ Phase 5 : Routes REST API

### 5.1 Endpoints (depuis server/routes/ et index.js)

- [x] `routes/health.py` - GET /health, /diagnostics
- [x] `routes/profiles.py` - CRUD /profiles/\*
- [x] `routes/tokens.py` - /handshake, /tokens/\*
- [x] `routes/discovery.py` - /discovery, /pairing/\*
- [x] `routes/plugins.py` - /plugins/\* (placeholder)

### 5.2 WebSocket endpoint

- [x] `/ws` - Handler principal WebSocket
  - [x] Authentification token (basique)
  - [x] Heartbeat/ping-pong
  - [x] Dispatch actions (basique)
  - [x] Broadcast events (basique)

---

## ✅ Phase 6 : Système de Plugins

- [x] `plugins/base.py` - Classe abstraite BasePlugin
- [x] `plugins/manager.py` - Chargement dynamique (stub)
- [x] Porter plugin Discord (mute/deafen via raccourcis Windows)
- [x] Porter plugin OBS (wrap HTTP RPC)
- [x] Porter plugin Spotify (media keys Windows)

---

## ✅ Phase 7 : Migration Frontend

### 7.1 Déplacer la Web UI

- [x] Copier `android/web-ui/` → `server/frontend/`
- [x] Adapter `vite.config.ts` :
  ```ts
  export default defineConfig({
    build: {
      outDir: "../static",
      emptyOutDir: true,
    },
  });
  ```

### 7.2 Adapter le code React

- [x] Simplifier `useWebSocket.ts` (retirer Android bridge)
- [x] URLs relatives pour API (`/api/...`, `/ws`)
- [x] Retirer toutes références à `window.Android`
- [x] Adapter `useConnectionSettings.ts`

### 7.3 Build frontend

- [x] `cd server/frontend && npm run build`
- [x] Vérifier que `server/static/` contient l'UI

---

## ✅ Phase 8 : Simplifier Android

### 8.1 Supprimer le code obsolète

- [x] Supprimer logique WebSocket Kotlin (WebSocketClient, ControlEventSender, ConnectionManager, tests)
- [x] Supprimer bridge JavaScript (NativeBridge dans ServerWebViewScreen)
- [x] Supprimer `android/web-ui/` (déplacé vers server)

### 8.2 Nouveau client minimal

- [x] `MainActivity.kt` - Écran principal
- [x] `WebViewScreen.kt` - WebView plein écran
- [x] `SettingsScreen.kt` - Configuration serveur
- [ ] `ServerDiscovery.kt` - Découverte mDNS (optionnel)

### 8.3 Fonctionnalités WebView

- [x] Charger URL du serveur configuré
- [x] JavaScript activé
- [x] Gestion erreurs réseau
- [x] Pull-to-refresh
- [x] Scanner QR code pour pairing

---

## ✅ Phase 9 : Tests

### 9.1 Tests backend Python

- [ ] Tests unitaires actions
- [ ] Tests unitaires utils
- [x] Tests routes API (pytest + httpx)
- [x] Tests WebSocket

### 9.2 Tests E2E

- [x] Adapter `tests/e2e/` pour nouveau serveur
- [x] Scénarios connexion/déconnexion
- [x] Scénarios profils
- [x] Scénarios actions

---

## ✅ Phase 10 : Déploiement

### 10.1 Docker

- [x] `server/Dockerfile` multi-stage :
  - Stage 1: Build frontend (Node.js)
  - Stage 2: Runtime Python + static files
- [x] `server/docker-compose.yml`

### 10.2 Scripts

- [x] Script démarrage Windows (PowerShell)
- [x] Script démarrage Linux/Mac
- [x] Service systemd (Linux)

### 10.3 Documentation

- [x] Mettre à jour `README.md`
- [x] Mettre à jour `AGENTS` / `AGENTS.md`
- [x] Guide migration utilisateurs existants
- [x] Documentation API (auto-générée OpenAPI)

---

## ✅ Phase 11 : Nettoyage

- [x] Supprimer ancien code Node.js (`server/*.js`)
- [x] Supprimer `server/package.json`
- [x] Supprimer `server/node_modules/`
- [x] Archiver ou supprimer fichiers obsolètes
- [ ] Merge branche `refactor/python-server` → `main`

---

## 📊 Progression

| Phase | Description      | Statut |
| ----- | ---------------- | ------ |
| 1     | Préparation      | ✅     |
| 2     | Structure Python | ✅     |
| 3     | Actions          | ✅     |
| 4     | Utilitaires      | ✅     |
| 5     | Routes API       | ✅     |
| 6     | Plugins          | ✅     |
| 7     | Frontend         | ✅     |
| 8     | Android          | ⬜     |
| 9     | Tests            | 🟡     |
| 10    | Déploiement      | ✅     |
| 11    | Nettoyage        | 🟡     |

**Légende** : ⬜ À faire | 🟡 En cours | ✅ Terminé

---

## 📝 Notes

- **Python** : 3.11+ requis
- **Node.js** : Uniquement pour build frontend
- **Compatibilité** : Garder format JSON profils existants
- **Migration** : Prévoir script pour migrer config existante
