# AGENTS.md

Ce depot utilise le fichier `AGENTS` (YAML) comme source de verite pour briefer assistants IA/humains sur la stack et les commandes clefs.

- **Fichier principal** : `AGENTS` (format YAML minimal) - source de verite.
- **Ce fichier** : resume humain + renvoi vers les sections detaillees.
- **Stack** : Android (Kotlin/Compose), Node.js 18 serveur WebSocket/REST, Vite/React/TypeScript web UI.
- **Entrees clefs** : composants (`server`, `android/app`, `web`, `tests/e2e`), chemins generes a ignorer (`server/config`, `server/logs`, `server/profiles`, `android/app/build`, `web/node_modules`, `web/dist`), commandes (install/build/test/audit/production gate), variables serveur (`PORT`, `HOST`, `DECK_TOKEN`, `HANDSHAKE_SECRET`, `DECK_DATA_DIR`, `LOG_LEVEL`, `TLS_*`).
- **Securite** : ne pas versionner tokens, cles TLS, keystores, contenus de `server/config|logs|profiles` ; garder `tests/e2e` sur un environnement non prod.

Pour plus de details (routines, scripts, references docs), voir le fichier `AGENTS`.
