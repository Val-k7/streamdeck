#!/bin/bash

# Script de sauvegarde pour Control Deck Server

set -e

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="control-deck-backup-${TIMESTAMP}"

echo -e "${BLUE}💾 Creating backup...${NC}"

# Créer le répertoire de sauvegarde
mkdir -p "${BACKUP_DIR}"

# Créer le répertoire de cette sauvegarde
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_NAME}"
mkdir -p "${BACKUP_PATH}"

# Sauvegarder les profils
if [ -d "profiles" ]; then
    echo -e "${YELLOW}📁 Backing up profiles...${NC}"
    cp -r profiles "${BACKUP_PATH}/"
fi

# Sauvegarder la configuration
if [ -d "config" ]; then
    echo -e "${YELLOW}⚙️  Backing up configuration...${NC}"
    cp -r config "${BACKUP_PATH}/"
fi

# Sauvegarder les plugins
if [ -d "plugins" ]; then
    echo -e "${YELLOW}🔌 Backing up plugins...${NC}"
    cp -r plugins "${BACKUP_PATH}/"
fi

# Sauvegarder les données
if [ -d "data" ]; then
    echo -e "${YELLOW}💿 Backing up data...${NC}"
    cp -r data "${BACKUP_PATH}/"
fi

# Créer une archive
echo -e "${YELLOW}📦 Creating archive...${NC}"
cd "${BACKUP_DIR}"
tar -czf "${BACKUP_NAME}.tar.gz" "${BACKUP_NAME}"
rm -rf "${BACKUP_NAME}"

# Afficher la taille
SIZE=$(du -h "${BACKUP_NAME}.tar.gz" | cut -f1)
echo -e "${GREEN}✅ Backup created: ${BACKUP_NAME}.tar.gz (${SIZE})${NC}"

# Nettoyer les anciennes sauvegardes (garder les 10 dernières)
echo -e "${YELLOW}🧹 Cleaning old backups...${NC}"
ls -t control-deck-backup-*.tar.gz 2>/dev/null | tail -n +11 | xargs rm -f 2>/dev/null || true

echo -e "${GREEN}✅ Backup complete!${NC}"





