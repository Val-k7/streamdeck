#!/bin/bash
# Script bash pour copier l'UI buildée dans les assets Android

set -e

BUILD=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --build)
      BUILD=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
WEB_DIR="$ROOT_DIR/web"
WEB_DIST_DIR="$WEB_DIR/dist"
ANDROID_ASSETS_DIR="$ROOT_DIR/android/app/src/main/assets/web"

echo "📦 Copying UI assets to Android..."

# Builder l'UI si demandé
if [ "$BUILD" = true ]; then
  echo "🔨 Building UI for Android..."
  cd "$WEB_DIR"
  export VITE_ANDROID_BUILD=true
  npm run build
  unset VITE_ANDROID_BUILD
  cd "$ROOT_DIR"
fi

# Vérifier que le dossier dist existe
if [ ! -d "$WEB_DIST_DIR" ]; then
  echo "❌ Web dist directory not found: $WEB_DIST_DIR"
  echo "💡 Run with --build flag to build the UI first"
  exit 1
fi

# Créer le dossier assets/web s'il n'existe pas
mkdir -p "$ANDROID_ASSETS_DIR"

# Nettoyer l'ancien contenu
echo "🧹 Cleaning old assets..."
rm -rf "$ANDROID_ASSETS_DIR"/*

# Copier les fichiers
echo "📋 Copying files..."
cp -r "$WEB_DIST_DIR"/* "$ANDROID_ASSETS_DIR"/

echo "✅ UI assets copied successfully!"
echo "   Source: $WEB_DIST_DIR"
echo "   Destination: $ANDROID_ASSETS_DIR"

