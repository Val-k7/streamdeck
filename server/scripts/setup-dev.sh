#!/bin/bash

# Script de configuration de l'environnement de développement

set -e

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🛠️  Setting up Control Deck development environment...${NC}"
echo ""

# Vérifier les dépendances
echo -e "${YELLOW}📦 Checking dependencies...${NC}"
node scripts/check-dependencies.js

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠  Installing dependencies...${NC}"
    npm install
fi

# Créer les répertoires de développement
echo ""
echo -e "${YELLOW}📁 Creating development directories...${NC}"
mkdir -p logs
mkdir -p config
mkdir -p profiles
mkdir -p plugins
mkdir -p data
mkdir -p tests

# Configuration de développement
echo ""
echo -e "${YELLOW}⚙️  Setting up development configuration...${NC}"

if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        # Modifier pour le développement
        sed -i.bak 's/NODE_ENV=production/NODE_ENV=development/' .env 2>/dev/null || \
        sed -i '' 's/NODE_ENV=production/NODE_ENV=development/' .env 2>/dev/null || true
        sed -i.bak 's/LOG_LEVEL=info/LOG_LEVEL=debug/' .env 2>/dev/null || \
        sed -i '' 's/LOG_LEVEL=info/LOG_LEVEL=debug/' .env 2>/dev/null || true
        rm -f .env.bak 2>/dev/null || true
        echo -e "${GREEN}✓ .env created for development${NC}"
    fi
fi

if [ ! -f "config/server.config.json" ]; then
    if [ -f "config/server.config.sample.json" ]; then
        cp config/server.config.sample.json config/server.config.json
        echo -e "${GREEN}✓ Configuration file created${NC}"
    fi
fi

# Installer les outils de développement (optionnel)
echo ""
read -p "Install development tools (nodemon, etc.)? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}📦 Installing development tools...${NC}"
    npm install --save-dev nodemon 2>/dev/null || echo "nodemon already installed or failed"
fi

# Valider la configuration
echo ""
echo -e "${YELLOW}✅ Validating configuration...${NC}"
npm run validate || echo "Validation completed with warnings"

# Résumé
echo ""
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   Development Setup Complete! 🎉      ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "1. Review ${YELLOW}.env${NC} and ${YELLOW}config/server.config.json${NC}"
echo -e "2. Start development server: ${GREEN}npm start${NC}"
echo -e "3. Or use nodemon: ${GREEN}npx nodemon index.js${NC}"
echo ""





