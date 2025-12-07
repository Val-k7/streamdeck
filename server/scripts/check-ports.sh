#!/bin/bash

# Script de vérification des ports pour Control Deck Server

set -e

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PORT=${PORT:-4455}

echo -e "${BLUE}🔍 Checking ports for Control Deck Server...${NC}"
echo ""

# Vérifier le port principal
echo -e "${YELLOW}Checking port ${PORT}...${NC}"

if command -v netstat &> /dev/null; then
    if netstat -an | grep -q ":${PORT}"; then
        echo -e "${RED}  ❌ Port ${PORT} is already in use${NC}"
        echo -e "${YELLOW}  💡 Change the port in .env or config/server.config.json${NC}"
        exit 1
    else
        echo -e "${GREEN}  ✅ Port ${PORT} is available${NC}"
    fi
elif command -v ss &> /dev/null; then
    if ss -an | grep -q ":${PORT}"; then
        echo -e "${RED}  ❌ Port ${PORT} is already in use${NC}"
        echo -e "${YELLOW}  💡 Change the port in .env or config/server.config.json${NC}"
        exit 1
    else
        echo -e "${GREEN}  ✅ Port ${PORT} is available${NC}"
    fi
elif command -v lsof &> /dev/null; then
    if lsof -i :${PORT} &> /dev/null; then
        echo -e "${RED}  ❌ Port ${PORT} is already in use${NC}"
        echo -e "${YELLOW}  💡 Change the port in .env or config/server.config.json${NC}"
        exit 1
    else
        echo -e "${GREEN}  ✅ Port ${PORT} is available${NC}"
    fi
else
    echo -e "${YELLOW}  ⚠️  Cannot check port (netstat/ss/lsof not available)${NC}"
    echo -e "${YELLOW}  💡 Make sure port ${PORT} is not in use${NC}"
fi

echo ""
echo -e "${GREEN}✅ Port check complete!${NC}"





