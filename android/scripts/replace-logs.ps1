# Script pour remplacer tous les Log.* par AppLogger.* dans les fichiers Android

$files = Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | Where-Object {
    $content = Get-Content $_.FullName -Raw
    $content -match 'Log\.(d|e|w|i|v)'
}

Write-Host "Fichiers à traiter: $($files.Count)" -ForegroundColor Cyan

foreach ($file in $files) {
    Write-Host "Traitement: $($file.Name)" -ForegroundColor Yellow

    $content = Get-Content $file.FullName -Raw

    # Vérifier si AppLogger est déjà importé
    $hasAppLoggerImport = $content -match 'import com\.androidcontroldeck\.logging\.AppLogger'
    $hasLogImport = $content -match 'import android\.util\.Log'

    # Remplacer les imports
    if ($hasLogImport -and -not $hasAppLoggerImport) {
        $content = $content -replace 'import android\.util\.Log', "import com.androidcontroldeck.logging.AppLogger`nimport android.util.Log"
    } elseif (-not $hasAppLoggerImport) {
        # Ajouter l'import après le package
        $content = $content -replace '(package [^\n]+\n)', "`$1import com.androidcontroldeck.logging.AppLogger`n"
    }

    # Remplacer Log.d par AppLogger.d
    $content = $content -replace 'Log\.d\(', 'AppLogger.d('

    # Remplacer Log.e par AppLogger.e
    $content = $content -replace 'Log\.e\(', 'AppLogger.e('

    # Remplacer Log.w par AppLogger.w
    $content = $content -replace 'Log\.w\(', 'AppLogger.w('

    # Remplacer Log.i par AppLogger.i
    $content = $content -replace 'Log\.i\(', 'AppLogger.i('

    # Remplacer Log.v par AppLogger.v
    $content = $content -replace 'Log\.v\(', 'AppLogger.v('

    # Remplacer android.util.Log par AppLogger
    $content = $content -replace 'android\.util\.Log\.(d|e|w|i|v)\(', 'AppLogger.$1('

    Set-Content -Path $file.FullName -Value $content -NoNewline

    Write-Host "  ✓ Terminé" -ForegroundColor Green
}

Write-Host "`nRemplacement termine!" -ForegroundColor Green

