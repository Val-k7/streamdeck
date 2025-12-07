#!/bin/bash
# Script d'audit de sécurité des dépendances Android

set -e

echo "🔒 Audit de sécurité Android Control Deck"
echo ""

cd "$(dirname "$0")/.."

# Vérifier les dépendances avec dependency-check
if command -v dependency-check &> /dev/null; then
    echo "📦 Audit des dépendances avec OWASP Dependency-Check..."
    ./gradlew dependencyCheckAnalyze
else
    echo "⚠️  OWASP Dependency-Check non installé"
    echo "   Installez-le avec: brew install dependency-check (macOS)"
    echo "   ou téléchargez depuis: https://owasp.org/www-project-dependency-check/"
fi

# Vérifier les versions de dépendances
echo ""
echo "📋 Vérification des versions de dépendances..."
./gradlew dependencies --configuration releaseRuntimeClasspath | grep -E "(\+\-\-|FAILED)" || true

# Vérifier ProGuard
echo ""
echo "🛡️  Vérification ProGuard..."
if [ -f "app/proguard-rules.pro" ]; then
    echo "✅ Fichier proguard-rules.pro trouvé"
    # Vérifier que les règles sont complètes
    if grep -q "keep" app/proguard-rules.pro; then
        echo "✅ Règles ProGuard présentes"
    else
        echo "⚠️  Aucune règle 'keep' trouvée dans ProGuard"
    fi
else
    echo "⚠️  Fichier proguard-rules.pro non trouvé"
fi

# Vérifier les permissions
echo ""
echo "🔐 Vérification des permissions AndroidManifest..."
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
    echo "✅ Permission INTERNET trouvée"
else
    echo "❌ Permission INTERNET manquante"
fi

echo ""
echo "✅ Audit terminé"


