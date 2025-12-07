#!/bin/bash

# Script d'installation pour Control Deck Server

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Control Deck Server Installation   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

# Vérifier Node.js
echo -e "${YELLOW}📦 Checking Node.js...${NC}"
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    echo -e "${YELLOW}Please install Node.js 18+ from https://nodejs.org/${NC}"
    exit 1
fi

NODE_VERSION=$(node -v)
NODE_MAJOR=$(echo $NODE_VERSION | cut -d'v' -f2 | cut -d'.' -f1)

if [ "$NODE_MAJOR" -lt 18 ]; then
    echo -e "${RED}❌ Node.js version 18+ is required (found: $NODE_VERSION)${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Node.js version: ${NODE_VERSION}${NC}"

# Vérifier npm
echo -e "${YELLOW}📦 Checking npm...${NC}"
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm is not installed${NC}"
    exit 1
fi

NPM_VERSION=$(npm -v)
echo -e "${GREEN}✓ npm version: ${NPM_VERSION}${NC}"

# Installer les dépendances
echo ""
echo -e "${YELLOW}📥 Installing dependencies...${NC}"
npm install

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to install dependencies${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Dependencies installed${NC}"

# Créer les répertoires nécessaires
echo ""
echo -e "${YELLOW}📁 Creating directories...${NC}"
mkdir -p logs
mkdir -p config
mkdir -p profiles
mkdir -p plugins
mkdir -p data

echo -e "${GREEN}✓ Directories created${NC}"

# Configuration
echo ""
echo -e "${YELLOW}⚙️  Setting up configuration...${NC}"

if [ ! -f "config/server.config.json" ]; then
    if [ -f "config/server.config.sample.json" ]; then
        cp config/server.config.sample.json config/server.config.json
        echo -e "${GREEN}✓ Configuration file created from sample${NC}"
        echo -e "${YELLOW}⚠  Please edit config/server.config.json with your settings${NC}"
    else
        echo -e "${YELLOW}⚠  No sample config found. Using defaults.${NC}"
    fi
else
    echo -e "${GREEN}✓ Configuration file already exists${NC}"
fi

# Fichier .env
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo -e "${GREEN}✓ .env file created from example${NC}"
        echo -e "${YELLOW}⚠  Please edit .env with your settings${NC}"
    fi
fi

# Rendre les scripts exécutables
echo ""
echo -e "${YELLOW}🔧 Making scripts executable...${NC}"
chmod +x scripts/*.sh 2>/dev/null || true
echo -e "${GREEN}✓ Scripts are executable${NC}"

# Résumé
echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║      Installation Complete! 🎉         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "1. Edit ${YELLOW}config/server.config.json${NC} with your settings"
echo -e "2. Edit ${YELLOW}.env${NC} if needed"
echo -e "3. Start the server with: ${GREEN}./scripts/start.sh${NC}"
echo -e "   or: ${GREEN}npm start${NC}"
echo ""
echo -e "${BLUE}Documentation:${NC}"
echo -e "- Quick Start: ${YELLOW}QUICK_START.md${NC}"
echo -e "- Full README: ${YELLOW}README.md${NC}"
echo ""
