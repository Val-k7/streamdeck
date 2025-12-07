#!/bin/bash
# Script de vérification finale pour la production

set -e

echo "🔍 Vérification Production Ready - Control Deck"
echo "================================================"
echo ""

ERRORS=0
WARNINGS=0

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
    else
        echo -e "${RED}❌ $1${NC}"
        ERRORS=$((ERRORS + 1))
    fi
}

warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    WARNINGS=$((WARNINGS + 1))
}

# Vérifier la structure des tests
echo "📋 Vérification des tests..."
if [ -d "android/app/src/test" ]; then
    TEST_COUNT=$(find android/app/src/test -name "*Test.kt" | wc -l)
    if [ $TEST_COUNT -gt 0 ]; then
        check "Tests Android trouvés ($TEST_COUNT fichiers)"
    else
        warn "Aucun test Android trouvé"
    fi
else
    warn "Dossier de tests Android manquant"
fi

if [ -d "server/__tests__" ]; then
    SERVER_TEST_COUNT=$(find server/__tests__ -name "*.test.js" | wc -l)
    if [ $SERVER_TEST_COUNT -gt 0 ]; then
        check "Tests serveur trouvés ($SERVER_TEST_COUNT fichiers)"
    else
        warn "Aucun test serveur trouvé"
    fi
else
    warn "Dossier de tests serveur manquant"
fi

if [ -d "web/src" ] && find web/src -name "*.test.ts*" -o -name "*.test.tsx" | grep -q .; then
    WEB_TEST_COUNT=$(find web/src -name "*.test.ts*" -o -name "*.test.tsx" | wc -l)
    check "Tests web trouvés ($WEB_TEST_COUNT fichiers)"
else
    warn "Aucun test web trouvé"
fi

# Vérifier la sécurité
echo ""
echo "🔒 Vérification de la sécurité..."

# Vérifier les logs
if grep -r "console\.log\|console\.warn\|console\.error" server/index.js server/actions/*.js 2>/dev/null | grep -v "test\|node_modules" | grep -q .; then
    warn "console.* trouvé dans les fichiers serveur critiques"
else
    check "Aucun console.* dans les fichiers serveur critiques"
fi

# Vérifier les tokens
if grep -r "change-me" server/config/*.json 2>/dev/null | grep -v "sample\|example" | grep -q .; then
    warn "Token 'change-me' trouvé dans la configuration"
else
    check "Aucun token 'change-me' dans la configuration"
fi

# Vérifier la configuration Android
if [ -f "android/app/src/release/res/xml/network_security_config.xml" ]; then
    check "Configuration réseau release Android trouvée"
else
    warn "Configuration réseau release Android manquante"
fi

# Vérifier la documentation
echo ""
echo "📚 Vérification de la documentation..."

DOCS=(
    "GUIDE_INSTALLATION_PRODUCTION.md"
    "GUIDE_DEPLOIEMENT.md"
    "README_TESTING.md"
    "README_ENV.md"
    "OPTIMIZATIONS.md"
    "GUIDE_NETTOYAGE.md"
)

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ] || [ -f "server/$doc" ]; then
        check "Documentation $doc trouvée"
    else
        warn "Documentation $doc manquante"
    fi
done

# Vérifier les scripts
echo ""
echo "🛠️  Vérification des scripts..."

SCRIPTS=(
    "android/scripts/generate-keystore.sh"
    "android/scripts/bump-version.sh"
    "android/scripts/build-release.sh"
    "server/scripts/audit-security.sh"
)

for script in "${SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        check "Script $script trouvé"
    else
        warn "Script $script manquant"
    fi
done

# Résumé
echo ""
echo "================================================"
echo "📊 Résumé"
echo "================================================"
echo -e "${GREEN}✅ Vérifications réussies${NC}"
echo -e "${YELLOW}⚠️  Avertissements: $WARNINGS${NC}"
if [ $ERRORS -gt 0 ]; then
    echo -e "${RED}❌ Erreurs: $ERRORS${NC}"
    exit 1
else
    echo -e "${GREEN}✅ Aucune erreur critique${NC}"
    exit 0
fi


