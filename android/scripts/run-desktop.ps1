# Script pour lancer l'application desktop de débogage
# Usage: .\scripts\run-desktop.ps1

Write-Host "Building and running Control Deck Desktop..." -ForegroundColor Green

# Aller dans le répertoire android
Set-Location $PSScriptRoot\..

# Construire et lancer l'application desktop
.\gradlew.bat :desktopApp:run


