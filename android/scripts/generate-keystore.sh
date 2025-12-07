#!/bin/bash
# Script pour générer un keystore pour le signing Android

set -e

KEYSTORE_DIR="$(cd "$(dirname "$0")/.." && pwd)/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/control-deck-release.jks"
KEY_ALIAS="control-deck-key"

echo "🔐 Génération du keystore Android"
echo ""

# Créer le répertoire keystore s'il n'existe pas
mkdir -p "$KEYSTORE_DIR"

# Vérifier si le keystore existe déjà
if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Le keystore existe déjà: $KEYSTORE_FILE"
    read -p "Voulez-vous le remplacer? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Annulé."
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

# Demander les informations
echo "Entrez les informations pour le keystore:"
read -sp "Mot de passe du keystore (min 6 caractères): " STORE_PASSWORD
echo
read -sp "Confirmez le mot de passe du keystore: " STORE_PASSWORD_CONFIRM
echo

if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Les mots de passe ne correspondent pas"
    exit 1
fi

if [ ${#STORE_PASSWORD} -lt 6 ]; then
    echo "❌ Le mot de passe doit contenir au moins 6 caractères"
    exit 1
fi

read -sp "Mot de passe de la clé (peut être identique au keystore): " KEY_PASSWORD
echo
read -sp "Confirmez le mot de passe de la clé: " KEY_PASSWORD_CONFIRM
echo

if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
    echo "❌ Les mots de passe ne correspondent pas"
    exit 1
fi

read -p "Nom complet (CN): " CN
read -p "Organisation (O): " OU
read -p "Unité organisationnelle (OU): " OU_UNIT
read -p "Ville (L): " L
read -p "État/Province (ST): " ST
read -p "Code pays (C) [FR]: " C
C=${C:-FR}

# Générer le keystore
echo ""
echo "Génération du keystore..."
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$CN, OU=$OU_UNIT, O=$OU, L=$L, ST=$ST, C=$C"

echo ""
echo "✅ Keystore généré avec succès: $KEYSTORE_FILE"
echo ""
echo "📝 Créez le fichier keystore.properties avec:"
echo "   storeFile=$KEYSTORE_FILE"
echo "   storePassword=$STORE_PASSWORD"
echo "   keyAlias=$KEY_ALIAS"
echo "   keyPassword=$KEY_PASSWORD"
echo ""
echo "⚠️  IMPORTANT:"
echo "   - Gardez le keystore et les mots de passe en sécurité"
echo "   - Ne commitez JAMAIS keystore.properties dans Git"
echo "   - Ajoutez keystore/ et keystore.properties à .gitignore"


