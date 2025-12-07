#!/bin/bash
# Script pour construire l'APK/AAB de release

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

echo "🔨 Build de release Android"
echo ""

# Vérifier que le keystore est configuré
if [ ! -f "keystore.properties" ]; then
    echo "⚠️  keystore.properties non trouvé"
    echo "   Créez-le en copiant keystore.properties.example"
    echo "   Ou exécutez scripts/generate-keystore.sh"
    exit 1
fi

# Nettoyer les builds précédents
echo "🧹 Nettoyage..."
./gradlew clean

# Construire l'APK de release
echo ""
echo "📦 Construction de l'APK de release..."
./gradlew assembleRelease

# Construire l'AAB (pour Google Play)
echo ""
echo "📦 Construction de l'AAB (Android App Bundle)..."
./gradlew bundleRelease

echo ""
echo "✅ Build terminé!"
echo ""
echo "APK: app/build/outputs/apk/release/app-release.apk"
echo "AAB: app/build/outputs/bundle/release/app-release.aab"


