# Script PowerShell pour copier l'UI buildée dans les assets Android

param(
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$WebDir = Join-Path $RootDir "web"
$WebDistDir = Join-Path $WebDir "dist"
$AndroidAssetsDir = Join-Path $RootDir "android\app\src\main\assets\web"

Write-Host "Copying UI assets to Android..." -ForegroundColor Cyan

# Builder l'UI si demandé
if ($Build) {
    Write-Host "Building UI for Android..." -ForegroundColor Yellow
    Push-Location $WebDir
    $env:VITE_ANDROID_BUILD = "true"
    npm run build
    Remove-Item Env:\VITE_ANDROID_BUILD
    Pop-Location
}

# Vérifier que le dossier dist existe
if (-not (Test-Path $WebDistDir)) {
    Write-Host "ERROR: Web dist directory not found: $WebDistDir" -ForegroundColor Red
    Write-Host "TIP: Run with -Build flag to build the UI first" -ForegroundColor Yellow
    exit 1
}

# Créer le dossier assets/web s'il n'existe pas
if (-not (Test-Path $AndroidAssetsDir)) {
    New-Item -ItemType Directory -Path $AndroidAssetsDir -Force | Out-Null
}

# Nettoyer l'ancien contenu
Write-Host "Cleaning old assets..." -ForegroundColor Yellow
if (Test-Path $AndroidAssetsDir) {
    Remove-Item "$AndroidAssetsDir\*" -Recurse -Force
}

# Copier les fichiers
Write-Host "Copying files..." -ForegroundColor Yellow
Copy-Item -Path "$WebDistDir\*" -Destination $AndroidAssetsDir -Recurse -Force

Write-Host "UI assets copied successfully!" -ForegroundColor Green
Write-Host "   Source: $WebDistDir" -ForegroundColor Gray
Write-Host ('   Destination: ' + $AndroidAssetsDir) -ForegroundColor Gray
