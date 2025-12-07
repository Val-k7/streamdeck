# Script PowerShell d'audit de sécurité des dépendances Android

Write-Host "🔒 Audit de sécurité Android Control Deck" -ForegroundColor Cyan
Write-Host ""

$SCRIPT_DIR = Split-Path $PSScriptRoot -Parent
Set-Location $SCRIPT_DIR

# Vérifier les dépendances
Write-Host "📦 Vérification des dépendances..." -ForegroundColor Cyan
& .\gradlew.bat dependencies --configuration releaseRuntimeClasspath | Select-String -Pattern "(\+\-\-|FAILED)" -ErrorAction SilentlyContinue

# Vérifier ProGuard
Write-Host ""
Write-Host "🛡️  Vérification ProGuard..." -ForegroundColor Cyan
if (Test-Path "app\proguard-rules.pro") {
    Write-Host "✅ Fichier proguard-rules.pro trouvé" -ForegroundColor Green
    $rules = Get-Content "app\proguard-rules.pro" -Raw
    if ($rules -match "keep") {
        Write-Host "✅ Règles ProGuard présentes" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Aucune règle 'keep' trouvée dans ProGuard" -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠️  Fichier proguard-rules.pro non trouvé" -ForegroundColor Yellow
}

# Vérifier les permissions
Write-Host ""
Write-Host "🔐 Vérification des permissions AndroidManifest..." -ForegroundColor Cyan
$manifest = Get-Content "app\src\main\AndroidManifest.xml" -Raw
if ($manifest -match "android.permission.INTERNET") {
    Write-Host "✅ Permission INTERNET trouvée" -ForegroundColor Green
} else {
    Write-Host "❌ Permission INTERNET manquante" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Audit terminé" -ForegroundColor Green


