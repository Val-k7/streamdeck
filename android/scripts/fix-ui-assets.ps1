# Script pour corriger les chemins des assets dans index.html après le build
# Convertir les chemins absolus /assets/* en chemins relatifs ./assets/*
# Vérifie aussi que les fichiers CSS/icônes sont présents et correctement intégrés

param(
    [string]$AssetPath = "$PSScriptRoot\..\app\src\main\assets\web"
)

$ErrorActionPreference = "Stop"

Write-Host "Fixing UI asset paths for Android WebView..." -ForegroundColor Cyan
Write-Host "Asset path: $AssetPath" -ForegroundColor Gray

# Vérifier que le dossier existe
if (-not (Test-Path $AssetPath)) {
    Write-Host "ERROR: Asset path not found: $AssetPath" -ForegroundColor Red
    exit 1
}

$indexHtmlPath = Join-Path $AssetPath "index.html"

if (-not (Test-Path $indexHtmlPath)) {
    Write-Host "ERROR: index.html not found at: $indexHtmlPath" -ForegroundColor Red
    exit 1
}

# Vérifier les fichiers CSS présents
Write-Host "Checking for CSS files..." -ForegroundColor Yellow
$cssFiles = Get-ChildItem -Path $AssetPath -Filter "*.css" -Recurse | ForEach-Object { $_.Name }
if ($cssFiles) {
    Write-Host "  ✓ Found CSS files: $($cssFiles -join ', ')" -ForegroundColor Green
} else {
    Write-Host "  ✗ WARNING: No CSS files found!" -ForegroundColor Red
}

# Vérifier les assets/icons présents
Write-Host "Checking for assets subdirectory..." -ForegroundColor Yellow
$assetsDir = Join-Path $AssetPath "assets"
if (Test-Path $assetsDir) {
    $assetCount = @(Get-ChildItem -Path $assetsDir -Recurse).Count
    Write-Host "  ✓ Assets directory found with $assetCount files" -ForegroundColor Green

    # Compter les types de fichiers
    $svgCount = @(Get-ChildItem -Path $assetsDir -Filter "*.svg" -Recurse).Count
    $jsCount = @(Get-ChildItem -Path $assetsDir -Filter "*.js" -Recurse).Count
    $imageCount = @(Get-ChildItem -Path $assetsDir -Filter "*.{png,jpg,jpeg,webp}" -Recurse).Count

    Write-Host "    - SVG files: $svgCount" -ForegroundColor Gray
    Write-Host "    - JavaScript files: $jsCount" -ForegroundColor Gray
    Write-Host "    - Image files: $imageCount" -ForegroundColor Gray
} else {
    Write-Host "  ✗ WARNING: Assets subdirectory not found!" -ForegroundColor Red
}

# Lire le contenu du fichier
$content = Get-Content $indexHtmlPath -Raw

# Vérifier si les chemins sont déjà relatifs ou absolus
Write-Host "Checking path format in index.html..." -ForegroundColor Yellow
$hasAbsolutePaths = $content -match 'href="/assets/' -or $content -match 'src="/assets/'
$hasRelativePaths = $content -match 'href="\./assets/' -or $content -match 'src="\./assets/'

# Compter les références
$absoluteCount = [regex]::Matches($content, '(href|src)="/assets/').Count
$relativeCount = [regex]::Matches($content, '(href|src)="\./assets/').Count

Write-Host "  - Absolute paths found: $absoluteCount" -ForegroundColor Gray
Write-Host "  - Relative paths found: $relativeCount" -ForegroundColor Gray

if ($hasAbsolutePaths -and -not $hasRelativePaths) {
    Write-Host "✓ Asset paths are absolute (correct for WebViewAssetLoader)!" -ForegroundColor Green
} elseif ($hasRelativePaths) {
    Write-Host "Converting relative paths to absolute paths..." -ForegroundColor Yellow
    # Remplacer les chemins relatifs par des chemins absolus
    $content = $content -replace '(href|src)="\./assets/', '$1="/assets/'

    # Écrire le fichier modifié
    Set-Content -Path $indexHtmlPath -Value $content -NoNewline
    Write-Host "✓ Asset paths fixed successfully!" -ForegroundColor Green
} else {
    Write-Host "Could not determine asset path format. Manual review may be needed." -ForegroundColor Yellow
}

# Vérifier aussi les chemins des polices dans les fichiers CSS
Write-Host "Checking CSS files for font references..." -ForegroundColor Yellow
$cssFilesFullPath = Get-ChildItem -Path $AssetPath -Filter "*.css" -Recurse
if ($cssFilesFullPath) {
    foreach ($cssFile in $cssFilesFullPath) {
        $cssContent = Get-Content $cssFile.FullName -Raw
        # Vérifier s'il y a des URLs de polices absolues
        if ($cssContent -match 'url\s*\(\s*["\x27]?/') {
            Write-Host "  WARNING: Found absolute font paths in $($cssFile.Name)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "  No CSS files to check" -ForegroundColor Gray
}

Write-Host ""
Write-Host "UI asset path fixing complete!" -ForegroundColor Green

