# Déploiement rapide - Control Deck Server

## Démarrage rapide

### Option 1 : Installation locale

```bash
npm install
npm start
```

### Option 2 : Docker

```bash
docker-compose up -d
```

### Option 3 : Scripts

```bash
./scripts/install.sh
./scripts/start.sh
```

## Configuration

1. Copiez `.env.example` vers `.env`
2. Modifiez les valeurs selon vos besoins
3. Copiez `config/server.config.sample.json` vers `config/server.config.json`
4. Modifiez la configuration si nécessaire

## Vérification

```bash
npm run health
```

Le serveur devrait répondre sur `http://localhost:4455`

## Documentation complète

Voir `DEPLOYMENT.md` pour le guide complet de déploiement.





