# Script PowerShell pour incrémenter la version Android

$SCRIPT_DIR = Split-Path $PSScriptRoot -Parent
$BUILD_GRADLE = Join-Path $SCRIPT_DIR "app\build.gradle.kts"
$VERSION_FILE = Join-Path (Split-Path $SCRIPT_DIR -Parent) "VERSION"

# Lire la version actuelle
$content = Get-Content $BUILD_GRADLE -Raw
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $CURRENT_VERSION_CODE = [int]$matches[1]
} else {
    Write-Host "❌ Impossible de trouver versionCode" -ForegroundColor Red
    exit 1
}

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $CURRENT_VERSION = $matches[1]
} else {
    Write-Host "❌ Impossible de trouver versionName" -ForegroundColor Red
    exit 1
}

Write-Host "Version actuelle: $CURRENT_VERSION (code: $CURRENT_VERSION_CODE)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Type de mise à jour:"
Write-Host "1. Patch (1.0.0 -> 1.0.1)"
Write-Host "2. Minor (1.0.0 -> 1.1.0)"
Write-Host "3. Major (1.0.0 -> 2.0.0)"
Write-Host "4. Version personnalisée"
$choice = Read-Host "Choix [1-4]"

$VERSION_PARTS = $CURRENT_VERSION -split '\.'

switch ($choice) {
    "1" {
        # Patch
        $MAJOR = [int]$VERSION_PARTS[0]
        $MINOR = [int]$VERSION_PARTS[1]
        $PATCH = [int]$VERSION_PARTS[2]
        $NEW_VERSION = "$MAJOR.$MINOR.$($PATCH + 1)"
        $NEW_VERSION_CODE = $CURRENT_VERSION_CODE + 1
    }
    "2" {
        # Minor
        $MAJOR = [int]$VERSION_PARTS[0]
        $MINOR = [int]$VERSION_PARTS[1]
        $NEW_VERSION = "$MAJOR.$($MINOR + 1).0"
        $NEW_VERSION_CODE = $CURRENT_VERSION_CODE + 10
    }
    "3" {
        # Major
        $MAJOR = [int]$VERSION_PARTS[0]
        $NEW_VERSION = "$($MAJOR + 1).0.0"
        $NEW_VERSION_CODE = $CURRENT_VERSION_CODE + 100
    }
    "4" {
        $NEW_VERSION = Read-Host "Nouvelle version (ex: 1.2.3)"
        $NEW_VERSION_CODE = Read-Host "Nouveau version code (ex: 123)"
        $NEW_VERSION_CODE = [int]$NEW_VERSION_CODE
    }
    default {
        Write-Host "❌ Choix invalide" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Nouvelle version: $NEW_VERSION (code: $NEW_VERSION_CODE)" -ForegroundColor Cyan
$confirm = Read-Host "Confirmer? (y/N)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "Annulé." -ForegroundColor Yellow
    exit 0
}

# Mettre à jour build.gradle.kts
$newContent = $content -replace 'versionCode = \d+', "versionCode = $NEW_VERSION_CODE"
$newContent = $newContent -replace 'versionName = "[^"]+"', "versionName = `"$NEW_VERSION`""
Set-Content -Path $BUILD_GRADLE -Value $newContent -NoNewline

# Mettre à jour VERSION si le fichier existe
if (Test-Path $VERSION_FILE) {
    Set-Content -Path $VERSION_FILE -Value $NEW_VERSION
}

Write-Host "✅ Version mise à jour: $NEW_VERSION (code: $NEW_VERSION_CODE)" -ForegroundColor Green


