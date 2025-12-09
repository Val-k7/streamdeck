Param(
    [string]$Port = "4455",
    [string]$Host = "0.0.0.0",
    [string]$DataDir,
    [string]$HandshakeSecret,
    [string]$DeckToken
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Split-Path -Parent $scriptDir

if (-not $DataDir) { $DataDir = Join-Path $backendRoot "data" }

$env:DECK_DECK_DATA_DIR = $DataDir
if ($HandshakeSecret) { $env:DECK_HANDSHAKE_SECRET = $HandshakeSecret }
if ($DeckToken) { $env:DECK_DECK_TOKEN = $DeckToken }
$env:DECK_DISABLE_DISCOVERY = $env:DECK_DISABLE_DISCOVERY -as [string] -or "0"

Push-Location $backendRoot
try {
    Write-Host "Starting Control Deck backend on $Host:$Port (data dir: $DataDir)..."
    & "python" -m uvicorn app.main:app --host $Host --port $Port
}
finally {
    Pop-Location
}
