#!/bin/bash

# Script de démarrage pour le serveur Control Deck

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Control Deck Server...${NC}"

# Vérifier Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    exit 1
fi

NODE_VERSION=$(node -v)
echo -e "${GREEN}✓ Node.js version: ${NODE_VERSION}${NC}"

# Vérifier les dépendances
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}⚠ Installing dependencies...${NC}"
    npm install
fi

# Créer les répertoires nécessaires
mkdir -p logs
mkdir -p config
mkdir -p profiles
mkdir -p plugins

# Vérifier la configuration
if [ ! -f "config/server.config.json" ]; then
    if [ -f "config/server.config.sample.json" ]; then
        echo -e "${YELLOW}⚠ Creating config from sample...${NC}"
        cp config/server.config.sample.json config/server.config.json
    else
        echo -e "${YELLOW}⚠ No config file found. Using defaults.${NC}"
    fi
fi

# Variables d'environnement
export NODE_ENV=${NODE_ENV:-development}
export PORT=${PORT:-4455}
export LOG_LEVEL=${LOG_LEVEL:-info}

echo -e "${GREEN}✓ Environment: ${NODE_ENV}${NC}"
echo -e "${GREEN}✓ Port: ${PORT}${NC}"
echo -e "${GREEN}✓ Log Level: ${LOG_LEVEL}${NC}"

# Démarrer le serveur
echo -e "\n${GREEN}🎯 Starting server...${NC}\n"

if [ "$NODE_ENV" = "production" ]; then
    node index.js
else
    # En développement, utiliser nodemon si disponible
    if command -v nodemon &> /dev/null; then
        nodemon index.js
    else
        node index.js
    fi
fi





