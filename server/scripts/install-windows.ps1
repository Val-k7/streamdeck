#!/usr/bin/env pwsh
# Script d'installation Windows pour Control Deck Server

param(
    [string]$ProjectDir = (Resolve-Path "$PSScriptRoot/.."),
    [string]$ServiceName = "ControlDeckServer",
    [switch]$SkipService = $false
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "🚀 Installation de Control Deck Server pour Windows" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier les privilèges administrateur
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin -and -not $SkipService) {
    Write-Host "⚠️  Attention: Les privilèges administrateur sont requis pour créer un service Windows." -ForegroundColor Yellow
    Write-Host "   Exécutez ce script en tant qu'administrateur ou utilisez -SkipService" -ForegroundColor Yellow
    Write-Host ""
}

function Ensure-Node {
    Write-Host "📦 Vérification de Node.js..." -ForegroundColor Cyan
    $node = Get-Command node -ErrorAction SilentlyContinue
    if ($node) {
        $version = (& node -v) -replace "v([0-9]+).*", '$1'
        $fullVersion = & node -v
        if ([int]$version -lt 18) {
            Write-Host "❌ Node.js 18+ requis, version détectée: $fullVersion" -ForegroundColor Red
            Write-Host "   Veuillez mettre à jour Node.js depuis https://nodejs.org" -ForegroundColor Yellow
            exit 1
        } else {
            Write-Host "   ✓ Node.js $fullVersion détecté" -ForegroundColor Green
            return
        }
    }

    Write-Host "   Node.js non trouvé. Tentative d'installation..." -ForegroundColor Yellow

    if (Get-Command winget -ErrorAction SilentlyContinue) {
        Write-Host "   Installation via winget..." -ForegroundColor Gray
        winget install -e --id OpenJS.NodeJS.LTS -h
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✓ Node.js installé via winget" -ForegroundColor Green
            # Rafraîchir le PATH
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            return
        }
    } elseif (Get-Command choco -ErrorAction SilentlyContinue) {
        Write-Host "   Installation via Chocolatey..." -ForegroundColor Gray
        choco install -y nodejs-lts
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✓ Node.js installé via Chocolatey" -ForegroundColor Green
            return
        }
    }

    Write-Host "❌ Impossible d'installer Node.js automatiquement." -ForegroundColor Red
    Write-Host "   Veuillez installer Node.js manuellement depuis https://nodejs.org" -ForegroundColor Yellow
    Write-Host "   Puis relancez ce script." -ForegroundColor Yellow
    exit 1
}

# Vérifier Node.js
Ensure-Node

# Changer vers le répertoire du projet
Set-Location $ProjectDir
Write-Host ""
Write-Host "📂 Répertoire de travail: $ProjectDir" -ForegroundColor Cyan
Write-Host ""

# Installer les dépendances
Write-Host "📦 Installation des dépendances npm..." -ForegroundColor Cyan
if (Test-Path "package-lock.json") {
    npm ci --omit=dev
} else {
    npm install --omit=dev
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de l'installation des dépendances" -ForegroundColor Red
    exit 1
}

Write-Host "   ✓ Dépendances installées" -ForegroundColor Green
Write-Host ""

# Créer les répertoires nécessaires
Write-Host "📁 Création des répertoires..." -ForegroundColor Cyan
$dirs = @("config", "profiles", "logs", "plugins")
foreach ($dir in $dirs) {
    $dirPath = Join-Path $ProjectDir $dir
    if (-not (Test-Path $dirPath)) {
        New-Item -ItemType Directory -Path $dirPath -Force | Out-Null
        Write-Host "   ✓ Créé: $dir" -ForegroundColor Green
    }
}
Write-Host ""

# Configuration initiale
Write-Host "⚙️  Configuration initiale..." -ForegroundColor Cyan
Write-Host "   Exécutez 'npm run setup' pour configurer le serveur" -ForegroundColor Yellow
Write-Host ""

# Créer le service Windows (si demandé et si admin)
if (-not $SkipService -and $isAdmin) {
    Write-Host "🔧 Création du service Windows..." -ForegroundColor Cyan

    $nodePath = (Get-Command node).Source
    $scriptPath = Join-Path $ProjectDir "index.js"
    $binPath = "`"$nodePath`" `"$scriptPath`""

    # Arrêter et supprimer le service existant s'il existe
    $existing = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "   Arrêt du service existant..." -ForegroundColor Yellow
        Stop-Service $ServiceName -ErrorAction SilentlyContinue
        sc.exe delete $ServiceName | Out-Null
        Start-Sleep -Seconds 2
    }

    # Créer le service
    sc.exe create $ServiceName binPath= $binPath start= auto DisplayName= "Control Deck Server" | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✓ Service Windows créé: $ServiceName" -ForegroundColor Green
        Write-Host ""
        Write-Host "   Pour démarrer le service:" -ForegroundColor Yellow
        Write-Host "     Start-Service $ServiceName" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   Pour arrêter le service:" -ForegroundColor Yellow
        Write-Host "     Stop-Service $ServiceName" -ForegroundColor Gray
    } else {
        Write-Host "   ⚠️  Erreur lors de la création du service" -ForegroundColor Yellow
        Write-Host "   Vous pouvez démarrer le serveur manuellement avec 'npm start'" -ForegroundColor Yellow
    }
} elseif (-not $SkipService) {
    Write-Host "⚠️  Service Windows non créé (privilèges insuffisants)" -ForegroundColor Yellow
    Write-Host "   Vous pouvez démarrer le serveur manuellement avec 'npm start'" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ Installation terminée!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Prochaines étapes:" -ForegroundColor Cyan
Write-Host "   1. Exécutez 'npm run setup' pour configurer le serveur" -ForegroundColor White
Write-Host "   2. Démarrez le serveur avec 'npm start'" -ForegroundColor White
Write-Host "   3. Configurez l'application Android pour se connecter au serveur" -ForegroundColor White
Write-Host ""
Write-Host "📚 Documentation: Voir server/README.md" -ForegroundColor Cyan
Write-Host ""


