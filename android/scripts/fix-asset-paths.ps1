# Script pour corriger les chemins d'assets dans le HTML pour Android WebView
param(
    [string]$HtmlPath = "android\app\src\main\assets\web\index.html"
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
$fullPath = Join-Path $rootDir $HtmlPath

if (-not (Test-Path $fullPath)) {
    Write-Host "HTML file not found: $fullPath" -ForegroundColor Red
    exit 1
}

Write-Host "Fixing asset paths in HTML..." -ForegroundColor Yellow

$content = Get-Content $fullPath -Raw

# Remplacer les chemins absolus par des chemins relatifs
$content = $content -replace 'src="/assets/', 'src="./assets/'
$content = $content -replace 'href="/assets/', 'href="./assets/'
$content = $content -replace 'src="/', 'src="./'
$content = $content -replace 'href="/', 'href="./'

Set-Content -Path $fullPath -Value $content -NoNewline

Write-Host "Asset paths fixed!" -ForegroundColor Green

