# Script PowerShell d'audit de sécurité pour le serveur

Write-Host "🔒 Audit de sécurité Control Deck Server" -ForegroundColor Cyan
Write-Host ""

# Vérifier les dépendances
Write-Host "📦 Audit des dépendances npm..." -ForegroundColor Cyan
$auditResult = npm audit --audit-level=moderate 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Des vulnérabilités ont été trouvées" -ForegroundColor Yellow
    Write-Host "   Exécutez 'npm audit fix' pour les corriger automatiquement"
} else {
    Write-Host "✅ Aucune vulnérabilité trouvée" -ForegroundColor Green
}

# Vérifier les tokens par défaut
Write-Host ""
Write-Host "🔑 Vérification des tokens..." -ForegroundColor Cyan
$changeMeTokens = Select-String -Path "server\config\*.json" -Pattern "change-me" -Exclude "*.sample.*","*.example.*" -ErrorAction SilentlyContinue
if ($changeMeTokens) {
    Write-Host "❌ Des tokens 'change-me' ont été trouvés dans la configuration" -ForegroundColor Red
    Write-Host "   Remplacez-les par des tokens sécurisés"
    exit 1
} else {
    Write-Host "✅ Aucun token 'change-me' trouvé" -ForegroundColor Green
}

# Vérifier les secrets hardcodés
Write-Host ""
Write-Host "🔍 Recherche de secrets potentiellement exposés..." -ForegroundColor Cyan
$secrets = Select-String -Path "server\*.js","server\actions\*.js","server\utils\*.js" -Pattern "password.*=.*['\"].*[^=]" -Exclude "*test*","*sample*","*example*" -ErrorAction SilentlyContinue
if ($secrets) {
    Write-Host "⚠️  Des mots de passe potentiels ont été trouvés" -ForegroundColor Yellow
    Write-Host "   Vérifiez qu'ils ne sont pas hardcodés"
} else {
    Write-Host "✅ Aucun secret hardcodé suspect trouvé" -ForegroundColor Green
}

# Vérifier les console.log en production
Write-Host ""
Write-Host "📝 Vérification des logs..." -ForegroundColor Cyan
$consoleLogs = Select-String -Path "server\index.js","server\actions\*.js","server\utils\*.js" -Pattern "console\.(log|warn|error)" -Exclude "*test*","*node_modules*" -ErrorAction SilentlyContinue
if ($consoleLogs) {
    $count = ($consoleLogs | Measure-Object).Count
    Write-Host "⚠️  $count occurrences de console.* trouvées" -ForegroundColor Yellow
    Write-Host "   Remplacez-les par logger en production"
} else {
    Write-Host "✅ Aucun console.* trouvé dans les fichiers critiques" -ForegroundColor Green
}

# Vérifier la configuration TLS
Write-Host ""
Write-Host "🔐 Vérification TLS..." -ForegroundColor Cyan
if (-not $env:TLS_KEY_PATH -and -not $env:TLS_CERT_PATH) {
    Write-Host "⚠️  TLS non configuré" -ForegroundColor Yellow
    Write-Host "   Configurez TLS_KEY_PATH et TLS_CERT_PATH pour la production"
} else {
    Write-Host "✅ TLS configuré" -ForegroundColor Green
}

Write-Host ""
Write-Host "✅ Audit terminé" -ForegroundColor Green


