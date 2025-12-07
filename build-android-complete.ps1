#!/usr/bin/env pwsh
# Script pour builder complètement l'app pour Android (Web + APK)
# Usage: .\build-android-complete.ps1

param(
    [switch]$Install = $false  # Si -Install, installe aussi l'APK sur l'appareil
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Write-Host "🚀 Build Complet Android StreamDeck" -ForegroundColor Cyan
Write-Host "Root: $rootDir" -ForegroundColor Gray
Write-Host ""

# ========== STEP 1: BUILD WEB ==========
Write-Host "📦 STEP 1: Building Web UI..." -ForegroundColor Yellow
$webDir = Join-Path $rootDir "web"
Push-Location $webDir

Write-Host "  Setting VITE_ANDROID_BUILD=true" -ForegroundColor Gray
$env:VITE_ANDROID_BUILD = "true"

Write-Host "  Running: npm run build" -ForegroundColor Gray
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Web build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "  ✓ Web build successful" -ForegroundColor Green

Pop-Location
Write-Host ""

# ========== STEP 2: COPY ASSETS ==========
Write-Host "📁 STEP 2: Copying assets to Android..." -ForegroundColor Yellow
$scriptDir = Join-Path $rootDir "android/scripts"
Push-Location $scriptDir

Write-Host "  Running: ./copy-ui-assets.ps1" -ForegroundColor Gray
./copy-ui-assets.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Asset copy failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "  ✓ Assets copied successfully" -ForegroundColor Green

Pop-Location
Write-Host ""

# ========== STEP 3: ANDROID BUILD ==========
Write-Host "🔨 STEP 3: Building Android APK..." -ForegroundColor Yellow
$androidDir = Join-Path $rootDir "android"
Push-Location $androidDir

Write-Host "  Running: ./gradlew clean" -ForegroundColor Gray
./gradlew clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Gradle clean failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}

Write-Host "  Running: ./gradlew assembleDebug" -ForegroundColor Gray
./gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Gradle build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "  ✓ APK built successfully" -ForegroundColor Green

Pop-Location
Write-Host ""

# ========== STEP 4: OPTIONAL INSTALL ==========
if ($Install) {
    Write-Host "📲 STEP 4: Installing APK to device..." -ForegroundColor Yellow
    Push-Location $androidDir

    Write-Host "  Running: ./gradlew installDebug" -ForegroundColor Gray
    ./gradlew installDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ❌ Install failed!" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "  ✓ APK installed successfully" -ForegroundColor Green

    Pop-Location
    Write-Host ""
}

# ========== SUCCESS ==========
Write-Host "✅ Build Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "APK Location: $androidDir\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
Write-Host ""
if (-not $Install) {
    Write-Host "To install on device, run:" -ForegroundColor Gray
    Write-Host "  .\build-android-complete.ps1 -Install" -ForegroundColor Gray
}
Write-Host ""
Write-Host "Next: Open Android Studio and run the app via USB!" -ForegroundColor Cyan
