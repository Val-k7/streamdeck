# Script PowerShell pour construire l'APK/AAB de release

$SCRIPT_DIR = Split-Path $PSScriptRoot -Parent
Set-Location $SCRIPT_DIR

Write-Host "🔨 Build de release Android" -ForegroundColor Cyan
Write-Host ""

# Vérifier que le keystore est configuré
if (-not (Test-Path "keystore.properties")) {
    Write-Host "⚠️  keystore.properties non trouvé" -ForegroundColor Yellow
    Write-Host "   Créez-le en copiant keystore.properties.example"
    Write-Host "   Ou exécutez scripts/generate-keystore.ps1"
    exit 1
}

# Nettoyer les builds précédents
Write-Host "🧹 Nettoyage..." -ForegroundColor Cyan
& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors du nettoyage" -ForegroundColor Red
    exit 1
}

# Construire l'APK de release
Write-Host ""
Write-Host "📦 Construction de l'APK de release..." -ForegroundColor Cyan
& .\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la construction de l'APK" -ForegroundColor Red
    exit 1
}

# Construire l'AAB (pour Google Play)
Write-Host ""
Write-Host "📦 Construction de l'AAB (Android App Bundle)..." -ForegroundColor Cyan
& .\gradlew.bat bundleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la construction de l'AAB" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ Build terminé!" -ForegroundColor Green
Write-Host ""
Write-Host "APK: app\build\outputs\apk\release\app-release.apk"
Write-Host "AAB: app\build\outputs\bundle\release\app-release.aab"


