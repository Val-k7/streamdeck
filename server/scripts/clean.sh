#!/bin/bash

# Script de nettoyage pour Control Deck Server

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🧹 Cleaning Control Deck Server...${NC}"
echo ""

# Demander confirmation
read -p "This will delete build files, logs, and cache. Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Cancelled.${NC}"
    exit 0
fi

# Nettoyer les répertoires de build
echo -e "${YELLOW}🗑️  Removing build directories...${NC}"
rm -rf dist/ build/ 2>/dev/null || true
echo -e "${GREEN}✓ Build directories removed${NC}"

# Nettoyer les logs (garder les 7 derniers jours)
echo -e "${YELLOW}📋 Cleaning old logs...${NC}"
find logs/ -name "*.log" -mtime +7 -delete 2>/dev/null || true
find logs/ -name "*.gz" -mtime +30 -delete 2>/dev/null || true
echo -e "${GREEN}✓ Old logs cleaned${NC}"

# Nettoyer node_modules (optionnel)
read -p "Remove node_modules? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}📦 Removing node_modules...${NC}"
    rm -rf node_modules/ 2>/dev/null || true
    echo -e "${GREEN}✓ node_modules removed${NC}"
    echo -e "${YELLOW}⚠  Run 'npm install' to restore dependencies${NC}"
fi

# Nettoyer le cache npm
echo -e "${YELLOW}🗄️  Cleaning npm cache...${NC}"
npm cache clean --force 2>/dev/null || true
echo -e "${GREEN}✓ npm cache cleaned${NC}"

# Nettoyer les fichiers temporaires
echo -e "${YELLOW}🧹 Removing temporary files...${NC}"
find . -name "*.tmp" -delete 2>/dev/null || true
find . -name "*.swp" -delete 2>/dev/null || true
find . -name ".DS_Store" -delete 2>/dev/null || true
find . -name "Thumbs.db" -delete 2>/dev/null || true
echo -e "${GREEN}✓ Temporary files removed${NC}"

# Afficher l'espace libéré
echo ""
echo -e "${GREEN}✅ Cleanup complete!${NC}"
echo ""
echo -e "${BLUE}To restore:${NC}"
echo -e "  npm install"
echo -e "  npm start"





