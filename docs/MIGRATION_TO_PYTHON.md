# Migration vers le serveur Python

## Pré-requis
- Python 3.11+
- Node 18+ (uniquement pour builder le frontend)
- Sauvegarde des données existantes : `server/config`, `server/profiles`, `server/logs`, `server/plugins`, `server/config/tokens.json`, `server/config/server.id`.

## Étapes
1. **Basculer sur la branche `refactor/python-server`**
2. **Installer le backend**
   ```bash
   cd server/backend
   python -m venv .venv
   .venv/Scripts/activate
   pip install -r requirements.txt
   ```
3. **Builder l'UI** (optionnel si déjà buildée)
   ```bash
   cd ../frontend
   npm install
   npm run build  # génère server/static
   ```
4. **Lancer le backend**
   ```bash
   cd ../backend
   python -m uvicorn app.main:app --host 0.0.0.0 --port 4455
   # ou ./scripts/start.sh (bash) / .\scripts\start.ps1 (PowerShell)
   ```
5. **Mettre à jour la configuration client Android**
   - S'assurer que l'URL pointe vers le backend Python (port 4455 par défaut).

## Notes de compatibilité
- Les profils JSON existants sont conservés (même format).
- Le handshake utilise `HANDSHAKE_SECRET` (ou `DECK_DECK_TOKEN` par défaut) pour émettre des tokens.
- La découverte mDNS peut être désactivée via `DECK_DISABLE_DISCOVERY=1`.

## Nettoyage ancien serveur Node
- Supprimer `server/index.js`, `server/routes/`, `server/actions/`, `server/utils/`, `server/public/`, `server/__tests__/`, `server/scripts/` (Node), `server/package.json`, `server/package-lock.json`, `server/node_modules/`.
- Garder `server/frontend/` (React) pour build de l'UI.

## Déploiement
- Docker : `cd server && docker compose up --build`
- systemd : voir `docs/systemd/control-deck.service`
