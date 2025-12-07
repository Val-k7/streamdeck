param(
  [string]$ProjectDir = (Resolve-Path "$PSScriptRoot/.."),
  [string]$ServiceName = "AndroidControlDeck"
)

function Ensure-Node {
  $node = Get-Command node -ErrorAction SilentlyContinue
  if ($node) {
    $version = (& node -v) -replace "v([0-9]+).*", '$1'
    if ([int]$version -lt 18) { Write-Host "Node.js 18+ requis, version détectée $version" }
    else { return }
  }

  if (Get-Command winget -ErrorAction SilentlyContinue) {
    winget install -e --id OpenJS.NodeJS.LTS -h
  } elseif (Get-Command choco -ErrorAction SilentlyContinue) {
    choco install -y nodejs-lts
  } else {
    Write-Error "Installez Node.js depuis https://nodejs.org puis relancez le script."
    exit 1
  }
}

Ensure-Node
Set-Location $ProjectDir
if (Test-Path "package-lock.json") {
  npm ci --omit=dev
} else {
  npm install --omit=dev
}

Write-Host "Dépendances installées. Exécutez 'npm run setup' pour initialiser la configuration." -ForegroundColor Green

$nodePath = (Get-Command node).Source
$scriptPath = Join-Path $ProjectDir "index.js"
$binPath = '"' + $nodePath + '" "' + $scriptPath + '"'

$existing = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($existing) { Stop-Service $ServiceName -ErrorAction SilentlyContinue; sc.exe delete $ServiceName | Out-Null }

sc.exe create $ServiceName binPath= $binPath start= auto DisplayName= "Android Control Deck"
sc.exe start $ServiceName
Write-Host "Service Windows créé et démarré ($ServiceName)." -ForegroundColor Green
