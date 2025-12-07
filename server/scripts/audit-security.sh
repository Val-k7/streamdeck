#!/bin/bash
# Script d'audit de sécurité pour le serveur

set -e

echo "🔒 Audit de sécurité Control Deck Server"
echo ""

# Vérifier les dépendances
echo "📦 Audit des dépendances npm..."
npm audit --audit-level=moderate

if [ $? -ne 0 ]; then
    echo "⚠️  Des vulnérabilités ont été trouvées"
    echo "   Exécutez 'npm audit fix' pour les corriger automatiquement"
fi

# Vérifier les tokens par défaut
echo ""
echo "🔑 Vérification des tokens..."
if grep -r "change-me" server/config/ 2>/dev/null | grep -v "sample\|example"; then
    echo "❌ Des tokens 'change-me' ont été trouvés dans la configuration"
    echo "   Remplacez-les par des tokens sécurisés"
    exit 1
else
    echo "✅ Aucun token 'change-me' trouvé"
fi

# Vérifier les secrets hardcodés
echo ""
echo "🔍 Recherche de secrets potentiellement exposés..."
if grep -r "password.*=.*['\"].*[^=]" server/ 2>/dev/null | grep -v "sample\|example\|test\|TODO"; then
    echo "⚠️  Des mots de passe potentiels ont été trouvés"
    echo "   Vérifiez qu'ils ne sont pas hardcodés"
fi

# Vérifier les console.log en production
echo ""
echo "📝 Vérification des logs..."
console_logs=$(grep -r "console\.log\|console\.warn\|console\.error" server/index.js server/actions/ server/utils/ 2>/dev/null | grep -v "test\|node_modules" | wc -l)
if [ "$console_logs" -gt 0 ]; then
    echo "⚠️  $console_logs occurrences de console.* trouvées"
    echo "   Remplacez-les par logger en production"
else
    echo "✅ Aucun console.* trouvé dans les fichiers critiques"
fi

# Vérifier la configuration TLS
echo ""
echo "🔐 Vérification TLS..."
if [ -z "$TLS_KEY_PATH" ] && [ -z "$TLS_CERT_PATH" ]; then
    echo "⚠️  TLS non configuré"
    echo "   Configurez TLS_KEY_PATH et TLS_CERT_PATH pour la production"
else
    echo "✅ TLS configuré"
fi

echo ""
echo "✅ Audit terminé"


