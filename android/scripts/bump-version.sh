#!/bin/bash
# Script pour incrémenter la version Android

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_GRADLE="$SCRIPT_DIR/app/build.gradle.kts"
VERSION_FILE="$SCRIPT_DIR/../VERSION"

# Lire la version actuelle
CURRENT_VERSION=$(grep -E "versionName\s*=" "$BUILD_GRADLE" | sed -E 's/.*versionName\s*=\s*"([^"]+)".*/\1/')
CURRENT_VERSION_CODE=$(grep -E "versionCode\s*=" "$BUILD_GRADLE" | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/')

echo "Version actuelle: $CURRENT_VERSION (code: $CURRENT_VERSION_CODE)"
echo ""
echo "Type de mise à jour:"
echo "1. Patch (1.0.0 -> 1.0.1)"
echo "2. Minor (1.0.0 -> 1.1.0)"
echo "3. Major (1.0.0 -> 2.0.0)"
echo "4. Version personnalisée"
read -p "Choix [1-4]: " choice

case $choice in
    1)
        # Patch
        IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
        MAJOR=${VERSION_PARTS[0]}
        MINOR=${VERSION_PARTS[1]}
        PATCH=${VERSION_PARTS[2]}
        NEW_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
        NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
        ;;
    2)
        # Minor
        IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
        MAJOR=${VERSION_PARTS[0]}
        MINOR=${VERSION_PARTS[1]}
        NEW_VERSION="$MAJOR.$((MINOR + 1)).0"
        NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 10))
        ;;
    3)
        # Major
        IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
        MAJOR=${VERSION_PARTS[0]}
        NEW_VERSION="$((MAJOR + 1)).0.0"
        NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 100))
        ;;
    4)
        read -p "Nouvelle version (ex: 1.2.3): " NEW_VERSION
        read -p "Nouveau version code (ex: 123): " NEW_VERSION_CODE
        ;;
    *)
        echo "Choix invalide"
        exit 1
        ;;
esac

echo ""
echo "Nouvelle version: $NEW_VERSION (code: $NEW_VERSION_CODE)"
read -p "Confirmer? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Annulé."
    exit 0
fi

# Mettre à jour build.gradle.kts
sed -i.bak "s/versionCode = [0-9]\+/versionCode = $NEW_VERSION_CODE/" "$BUILD_GRADLE"
sed -i.bak "s/versionName = \"[^\"]\+\"/versionName = \"$NEW_VERSION\"/" "$BUILD_GRADLE"
rm -f "$BUILD_GRADLE.bak"

# Mettre à jour VERSION si le fichier existe
if [ -f "$VERSION_FILE" ]; then
    echo "$NEW_VERSION" > "$VERSION_FILE"
fi

echo "✅ Version mise à jour: $NEW_VERSION (code: $NEW_VERSION_CODE)"


