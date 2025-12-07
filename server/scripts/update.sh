#!/bin/bash

# Script de mise à jour pour Control Deck Server

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔄 Updating Control Deck Server...${NC}"
echo ""

# Sauvegarder la configuration actuelle
echo -e "${YELLOW}💾 Backing up configuration...${NC}"
if [ -f "config/server.config.json" ]; then
    cp config/server.config.json config/server.config.json.backup
    echo -e "${GREEN}✓ Configuration backed up${NC}"
fi

if [ -f ".env" ]; then
    cp .env .env.backup
    echo -e "${GREEN}✓ .env backed up${NC}"
fi

# Mettre à jour depuis Git (si c'est un repo Git)
if [ -d ".git" ]; then
    echo -e "${YELLOW}📥 Pulling latest changes...${NC}"
    git pull
    echo -e "${GREEN}✓ Code updated${NC}"
fi

# Mettre à jour les dépendances
echo ""
echo -e "${YELLOW}📦 Updating dependencies...${NC}"
npm install

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to update dependencies${NC}"
    echo -e "${YELLOW}Restoring backups...${NC}"
    if [ -f "config/server.config.json.backup" ]; then
        mv config/server.config.json.backup config/server.config.json
    fi
    if [ -f ".env.backup" ]; then
        mv .env.backup .env
    fi
    exit 1
fi

echo -e "${GREEN}✓ Dependencies updated${NC}"

# Vérifier les nouveaux fichiers de configuration
echo ""
echo -e "${YELLOW}⚙️  Checking configuration files...${NC}"

if [ -f "config/server.config.sample.json" ] && [ ! -f "config/server.config.json" ]; then
    cp config/server.config.sample.json config/server.config.json
    echo -e "${YELLOW}⚠  New config file created. Please review and update.${NC}"
fi

if [ -f ".env.example" ] && [ ! -f ".env" ]; then
    cp .env.example .env
    echo -e "${YELLOW}⚠  New .env file created. Please review and update.${NC}"
fi

# Nettoyer les backups
echo ""
echo -e "${YELLOW}🧹 Cleaning up...${NC}"
rm -f config/server.config.json.backup .env.backup
echo -e "${GREEN}✓ Cleanup complete${NC}"

# Résumé
echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║       Update Complete! ✅             ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}The server has been updated successfully.${NC}"
echo -e "${YELLOW}Please restart the server to apply changes.${NC}"
echo ""





