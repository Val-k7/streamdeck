# Script PowerShell pour générer un keystore pour le signing Android

$KEYSTORE_DIR = Join-Path (Split-Path $PSScriptRoot -Parent) "keystore"
$KEYSTORE_FILE = Join-Path $KEYSTORE_DIR "control-deck-release.jks"
$KEY_ALIAS = "control-deck-key"

Write-Host "🔐 Génération du keystore Android" -ForegroundColor Cyan
Write-Host ""

# Créer le répertoire keystore s'il n'existe pas
if (-not (Test-Path $KEYSTORE_DIR)) {
    New-Item -ItemType Directory -Path $KEYSTORE_DIR -Force | Out-Null
}

# Vérifier si le keystore existe déjà
if (Test-Path $KEYSTORE_FILE) {
    Write-Host "⚠️  Le keystore existe déjà: $KEYSTORE_FILE" -ForegroundColor Yellow
    $replace = Read-Host "Voulez-vous le remplacer? (y/N)"
    if ($replace -ne "y" -and $replace -ne "Y") {
        Write-Host "Annulé." -ForegroundColor Yellow
        exit 0
    }
    Remove-Item $KEYSTORE_FILE -Force
}

# Demander les informations
Write-Host "Entrez les informations pour le keystore:"
$storePassword = Read-Host "Mot de passe du keystore (min 6 caractères)" -AsSecureString
$storePasswordConfirm = Read-Host "Confirmez le mot de passe du keystore" -AsSecureString

$storePasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePassword)
)
$storePasswordConfirmPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePasswordConfirm)
)

if ($storePasswordPlain -ne $storePasswordConfirmPlain) {
    Write-Host "❌ Les mots de passe ne correspondent pas" -ForegroundColor Red
    exit 1
}

if ($storePasswordPlain.Length -lt 6) {
    Write-Host "❌ Le mot de passe doit contenir au moins 6 caractères" -ForegroundColor Red
    exit 1
}

$keyPassword = Read-Host "Mot de passe de la clé (peut être identique au keystore)" -AsSecureString
$keyPasswordConfirm = Read-Host "Confirmez le mot de passe de la clé" -AsSecureString

$keyPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword)
)
$keyPasswordConfirmPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPasswordConfirm)
)

if ($keyPasswordPlain -ne $keyPasswordConfirmPlain) {
    Write-Host "❌ Les mots de passe ne correspondent pas" -ForegroundColor Red
    exit 1
}

$CN = Read-Host "Nom complet (CN)"
$OU = Read-Host "Organisation (O)"
$OU_UNIT = Read-Host "Unité organisationnelle (OU)"
$L = Read-Host "Ville (L)"
$ST = Read-Host "État/Province (ST)"
$C = Read-Host "Code pays (C) [FR]"
if ([string]::IsNullOrWhiteSpace($C)) {
    $C = "FR"
}

# Générer le keystore
Write-Host ""
Write-Host "Génération du keystore..." -ForegroundColor Cyan

$dname = "CN=$CN, OU=$OU_UNIT, O=$OU, L=$L, ST=$ST, C=$C"

& keytool -genkeypair `
    -v `
    -keystore $KEYSTORE_FILE `
    -alias $KEY_ALIAS `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $storePasswordPlain `
    -keypass $keyPasswordPlain `
    -dname $dname

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erreur lors de la génération du keystore" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ Keystore généré avec succès: $KEYSTORE_FILE" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Créez le fichier keystore.properties avec:" -ForegroundColor Cyan
Write-Host "   storeFile=$KEYSTORE_FILE"
Write-Host "   storePassword=$storePasswordPlain"
Write-Host "   keyAlias=$KEY_ALIAS"
Write-Host "   keyPassword=$keyPasswordPlain"
Write-Host ""
Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
Write-Host "   - Gardez le keystore et les mots de passe en sécurité"
Write-Host "   - Ne commitez JAMAIS keystore.properties dans Git"
Write-Host "   - Ajoutez keystore/ et keystore.properties à .gitignore"


