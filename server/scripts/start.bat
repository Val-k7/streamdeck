@echo off
REM Script de démarrage pour le serveur Control Deck (Windows)

echo 🚀 Starting Control Deck Server...

REM Vérifier Node.js
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Node.js is not installed
    exit /b 1
)

for /f "tokens=*" %%i in ('node -v') do set NODE_VERSION=%%i
echo ✓ Node.js version: %NODE_VERSION%

REM Vérifier les dépendances
if not exist "node_modules" (
    echo ⚠ Installing dependencies...
    call npm install
)

REM Créer les répertoires nécessaires
if not exist "logs" mkdir logs
if not exist "config" mkdir config
if not exist "profiles" mkdir profiles
if not exist "plugins" mkdir plugins

REM Vérifier la configuration
if not exist "config\server.config.json" (
    if exist "config\server.config.sample.json" (
        echo ⚠ Creating config from sample...
        copy "config\server.config.sample.json" "config\server.config.json"
    ) else (
        echo ⚠ No config file found. Using defaults.
    )
)

REM Variables d'environnement
if "%NODE_ENV%"=="" set NODE_ENV=development
if "%PORT%"=="" set PORT=4455
if "%LOG_LEVEL%"=="" set LOG_LEVEL=info

echo ✓ Environment: %NODE_ENV%
echo ✓ Port: %PORT%
echo ✓ Log Level: %LOG_LEVEL%

REM Démarrer le serveur
echo.
echo 🎯 Starting server...
echo.

if "%NODE_ENV%"=="production" (
    node index.js
) else (
    REM En développement, utiliser nodemon si disponible
    where nodemon >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        nodemon index.js
    ) else (
        node index.js
    )
)





