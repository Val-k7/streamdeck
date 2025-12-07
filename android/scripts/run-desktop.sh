#!/bin/bash
# Script pour lancer l'application desktop de débogage
# Usage: ./scripts/run-desktop.sh

echo "Building and running Control Deck Desktop..."

# Aller dans le répertoire android
cd "$(dirname "$0")/.."

# Construire et lancer l'application desktop
./gradlew :desktopApp:run


