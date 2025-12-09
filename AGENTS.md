# AGENTS.md

Ce depot utilise le fichier `AGENTS` (YAML) comme source de verite pour briefer assistants IA/humains sur la stack et les commandes clefs.

# Directives à respecter impérativement

- Ne jamais créer de scripts si un problème concerne du code existant.
- Toujours essayer de corriger, optimiser ou compléter le code existant plutôt que d’écrire un nouveau script.
- Ne jamais utiliser de mocks, même pour les tests.
- Si une solution nécessite des mocks -> proposer une alternative réelle sans mock.
- Toujours donner la solution la plus simple possible.
- Toujours demander plus de contexte si besoin avant de générer du code.

- **Fichier principal** : `AGENTS` (format YAML minimal) - source de verite.
- **Ce fichier** : resume humain + renvoi vers les sections detaillees.
- **Stack** : Python 3.11+ (FastAPI/WebSocket), Vite/React/TypeScript servie par le backend, Android (Kotlin/Compose) client WebView.
- **Entrees clefs** : composants (`server/backend`, `server/frontend`, `android/app`, `tests/e2e`), chemins generes a ignorer (`server/config`, `server/logs`, `server/profiles`, `server/static`, `server/backend/.venv`, `server/frontend/node_modules`, `android/app/build`, `android/.gradle`), commandes (install/build/test/audit/production gate), variables serveur (`PORT`, `HOST`, `DECK_TOKEN`, `HANDSHAKE_SECRET`, `DECK_DATA_DIR`, `LOG_LEVEL`, `TLS_*`).
- **Securite** : ne pas versionner tokens, cles TLS, keystores, contenus de `server/config|logs|profiles` ; garder `tests/e2e` sur un environnement non prod.

Pour plus de details (routines, scripts, references docs), voir le fichier `AGENTS`.
