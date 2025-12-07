#!/usr/bin/env pwsh
# Script de migration des données depuis android/server vers server/ (si nécessaire)

param(
    [string]$SourceDir = "../android/server",
    [string]$DestDir = "..",
    [switch]$DryRun = $false
)

Write-Host "🔄 Migration des données du serveur" -ForegroundColor Cyan
Write-Host "Source: $SourceDir" -ForegroundColor Gray
Write-Host "Destination: $DestDir" -ForegroundColor Gray
Write-Host ""

if ($DryRun) {
    Write-Host "⚠️  Mode DRY-RUN - Aucune modification ne sera effectuée" -ForegroundColor Yellow
    Write-Host ""
}

# Vérifier que le répertoire source existe
if (-not (Test-Path $SourceDir)) {
    Write-Host "❌ Répertoire source introuvable: $SourceDir" -ForegroundColor Red
    exit 1
}

# Répertoires et fichiers à migrer
$itemsToMigrate = @(
    @{ Source = "config"; Dest = "config"; Type = "Directory" },
    @{ Source = "profiles"; Dest = "profiles"; Type = "Directory" },
    @{ Source = "logs"; Dest = "logs"; Type = "Directory" },
    @{ Source = "plugins"; Dest = "plugins"; Type = "Directory" }
)

$migratedCount = 0
$skippedCount = 0
$errorCount = 0

foreach ($item in $itemsToMigrate) {
    $sourcePath = Join-Path $SourceDir $item.Source
    $destPath = Join-Path $DestDir $item.Dest

    if (-not (Test-Path $sourcePath)) {
        Write-Host "⏭️  Ignoré (n'existe pas): $($item.Source)" -ForegroundColor Gray
        $skippedCount++
        continue
    }

    if ($item.Type -eq "Directory") {
        if (Test-Path $destPath) {
            Write-Host "⚠️  Déjà existant: $($item.Dest)" -ForegroundColor Yellow
            $skippedCount++
            continue
        }

        Write-Host "📁 Migration: $($item.Source) -> $($item.Dest)" -ForegroundColor Green

        if (-not $DryRun) {
            try {
                Copy-Item -Path $sourcePath -Destination $destPath -Recurse -Force
                Write-Host "   ✓ Migré avec succès" -ForegroundColor Green
                $migratedCount++
            } catch {
                Write-Host "   ❌ Erreur: $_" -ForegroundColor Red
                $errorCount++
            }
        } else {
            $migratedCount++
        }
    }
}

Write-Host ""
Write-Host "📊 Résumé de la migration:" -ForegroundColor Cyan
Write-Host "   ✓ Migrés: $migratedCount" -ForegroundColor Green
Write-Host "   ⏭️  Ignorés: $skippedCount" -ForegroundColor Yellow
Write-Host "   ❌ Erreurs: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Gray" })

if ($DryRun) {
    Write-Host ""
    Write-Host "💡 Pour effectuer la migration, exécutez sans -DryRun" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "✅ Migration terminée!" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  Note: Les données dans $SourceDir sont toujours présentes." -ForegroundColor Yellow
    Write-Host "   Vous pouvez les supprimer manuellement après vérification." -ForegroundColor Yellow
}

