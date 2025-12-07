# Script PowerShell de vérification finale pour la production

Write-Host "🔍 Vérification Production Ready - Control Deck" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$ERRORS = 0
$WARNINGS = 0

function Check {
    param([string]$Message)
    if ($LASTEXITCODE -eq 0 -or $?) {
        Write-Host "✅ $Message" -ForegroundColor Green
    } else {
        Write-Host "❌ $Message" -ForegroundColor Red
        $script:ERRORS++
    }
}

function Warn {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
    $script:WARNINGS++
}

# Vérifier la structure des tests
Write-Host "📋 Vérification des tests..." -ForegroundColor Cyan
if (Test-Path "android\app\src\test") {
    $testFiles = Get-ChildItem -Path "android\app\src\test" -Recurse -Filter "*Test.kt" -ErrorAction SilentlyContinue
    if ($testFiles) {
        Check "Tests Android trouvés ($($testFiles.Count) fichiers)"
    } else {
        Warn "Aucun test Android trouvé"
    }
} else {
    Warn "Dossier de tests Android manquant"
}

if (Test-Path "server\__tests__") {
    $serverTests = Get-ChildItem -Path "server\__tests__" -Recurse -Filter "*.test.js" -ErrorAction SilentlyContinue
    if ($serverTests) {
        Check "Tests serveur trouvés ($($serverTests.Count) fichiers)"
    } else {
        Warn "Aucun test serveur trouvé"
    }
} else {
    Warn "Dossier de tests serveur manquant"
}

$webTests = Get-ChildItem -Path "web\src" -Recurse -Include "*.test.ts*" -ErrorAction SilentlyContinue
if ($webTests) {
    Check "Tests web trouvés ($($webTests.Count) fichiers)"
} else {
    Warn "Aucun test web trouvé"
}

# Vérifier la sécurité
Write-Host ""
Write-Host "🔒 Vérification de la sécurité..." -ForegroundColor Cyan

$consoleLogs = Select-String -Path "server\index.js","server\actions\*.js" -Pattern "console\.(log|warn|error)" -Exclude "*test*","*node_modules*" -ErrorAction SilentlyContinue
if ($consoleLogs) {
    Warn "console.* trouvé dans les fichiers serveur critiques"
} else {
    Check "Aucun console.* dans les fichiers serveur critiques"
}

$changeMeTokens = Select-String -Path "server\config\*.json" -Pattern "change-me" -Exclude "*sample*","*example*" -ErrorAction SilentlyContinue
if ($changeMeTokens) {
    Warn "Token 'change-me' trouvé dans la configuration"
} else {
    Check "Aucun token 'change-me' dans la configuration"
}

if (Test-Path "android\app\src\release\res\xml\network_security_config.xml") {
    Check "Configuration réseau release Android trouvée"
} else {
    Warn "Configuration réseau release Android manquante"
}

# Vérifier la documentation
Write-Host ""
Write-Host "📚 Vérification de la documentation..." -ForegroundColor Cyan

$docs = @(
    "GUIDE_INSTALLATION_PRODUCTION.md",
    "GUIDE_DEPLOIEMENT.md",
    "README_TESTING.md",
    "README_ENV.md",
    "OPTIMIZATIONS.md",
    "GUIDE_NETTOYAGE.md"
)

foreach ($doc in $docs) {
    if (Test-Path $doc -ErrorAction SilentlyContinue) {
        Check "Documentation $doc trouvée"
    } elseif (Test-Path "server\$doc" -ErrorAction SilentlyContinue) {
        Check "Documentation $doc trouvée (serveur)"
    } else {
        Warn "Documentation $doc manquante"
    }
}

# Vérifier les scripts
Write-Host ""
Write-Host "🛠️  Vérification des scripts..." -ForegroundColor Cyan

$scripts = @(
    "android\scripts\generate-keystore.sh",
    "android\scripts\bump-version.sh",
    "android\scripts\build-release.sh",
    "server\scripts\audit-security.sh"
)

foreach ($script in $scripts) {
    if (Test-Path $script) {
        Check "Script $script trouvé"
    } else {
        # Vérifier la version PowerShell
        $psScript = $script -replace '\.sh$', '.ps1'
        if (Test-Path $psScript) {
            Check "Script $psScript trouvé (PowerShell)"
        } else {
            Warn "Script $script manquant"
        }
    }
}

# Résumé
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "📊 Résumé" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "✅ Vérifications réussies" -ForegroundColor Green
Write-Host "⚠️  Avertissements: $WARNINGS" -ForegroundColor Yellow
if ($ERRORS -gt 0) {
    Write-Host "❌ Erreurs: $ERRORS" -ForegroundColor Red
    exit 1
} else {
    Write-Host "✅ Aucune erreur critique" -ForegroundColor Green
    exit 0
}


