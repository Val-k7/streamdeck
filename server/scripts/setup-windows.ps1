#!/usr/bin/env pwsh
# Script de configuration Windows pour Control Deck Server

param(
    [string]$ProjectDir = (Resolve-Path "$PSScriptRoot/..")
)

Write-Host ""
Write-Host "⚙️  Configuration de Control Deck Server" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $ProjectDir

# Vérifier que Node.js est installé
$node = Get-Command node -ErrorAction SilentlyContinue
if (-not $node) {
    Write-Host "❌ Node.js n'est pas installé." -ForegroundColor Red
    Write-Host "   Installez Node.js depuis https://nodejs.org" -ForegroundColor Yellow
    exit 1
}

# Exécuter le script de setup Node.js
Write-Host "Lancement de l'assistant de configuration..." -ForegroundColor Cyan
Write-Host ""

node tools/setup.js

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Configuration terminée!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 Prochaines étapes:" -ForegroundColor Cyan
    Write-Host "   1. Vérifiez la configuration dans config/server.config.json" -ForegroundColor White
    Write-Host "   2. Démarrez le serveur avec 'npm start'" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Erreur lors de la configuration" -ForegroundColor Red
    exit 1
}


