#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE_NAME="android-control-deck"
NODE_MINIMUM_MAJOR=18

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

install_node_linux() {
  if command_exists apt-get; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
  elif command_exists dnf; then
    sudo dnf install -y nodejs
  elif command_exists yum; then
    sudo yum install -y nodejs
  else
    echo "Impossible d'installer Node.js automatiquement : gestionnaire de paquets inconnu" >&2
    exit 1
  fi
}

install_node_macos() {
  if command_exists brew; then
    brew install node@20
    brew link --overwrite --force node@20
  else
    echo "Homebrew est requis pour installer Node.js automatiquement." >&2
    echo "Installez-le via https://brew.sh puis relancez le script." >&2
    exit 1
  fi
}

ensure_node() {
  if command_exists node; then
    local major
    major=$(node -v | sed 's/v\([0-9]*\).*/\1/')
    if [ "$major" -lt "$NODE_MINIMUM_MAJOR" ]; then
      echo "Node.js $NODE_MINIMUM_MAJOR ou supérieur est requis. Version détectée : $major"
      case "$(uname -s)" in
        Linux) install_node_linux ;;
        Darwin) install_node_macos ;;
        *) echo "Système non pris en charge pour l'installation automatique" ; exit 1 ;;
      esac
    fi
  else
    echo "Node.js non détecté, installation..."
    case "$(uname -s)" in
      Linux) install_node_linux ;;
      Darwin) install_node_macos ;;
      *) echo "Système non pris en charge pour l'installation automatique" ; exit 1 ;;
    esac
  fi
}

create_systemd_service() {
  local node_bin
  node_bin=$(command -v node)
  sudo tee /etc/systemd/system/${SERVICE_NAME}.service >/dev/null <<SERVICE
[Unit]
Description=Android Control Deck Server
After=network-online.target

[Service]
Type=simple
WorkingDirectory=${PROJECT_DIR}
ExecStart=${node_bin} ${PROJECT_DIR}/index.js
Restart=on-failure
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
SERVICE

  sudo systemctl daemon-reload
  sudo systemctl enable ${SERVICE_NAME}
  sudo systemctl restart ${SERVICE_NAME}
  echo "Service systemd créé et démarré (${SERVICE_NAME})."
}

create_launchd_service() {
  local node_bin plist_path
  node_bin=$(command -v node)
  plist_path="${HOME}/Library/LaunchAgents/com.android-control-deck.plist"
  cat >"${plist_path}" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
  <dict>
    <key>Label</key>
    <string>com.android-control-deck</string>
    <key>ProgramArguments</key>
    <array>
      <string>${node_bin}</string>
      <string>${PROJECT_DIR}/index.js</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${PROJECT_DIR}</string>
    <key>EnvironmentVariables</key>
    <dict>
      <key>NODE_ENV</key>
      <string>production</string>
    </dict>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
  </dict>
</plist>
PLIST
  launchctl unload "${plist_path}" 2>/dev/null || true
  launchctl load "${plist_path}"
  echo "Service launchd installé (com.android-control-deck)."
}

case "$(uname -s)" in
  Linux|Darwin) ;;
  *) echo "Utilisez scripts/install.ps1 pour Windows."; exit 1 ;;
esac

ensure_node
cd "${PROJECT_DIR}"
if [ -f package-lock.json ]; then
  npm ci --omit=dev
else
  npm install --omit=dev
fi

echo "Dépendances installées. Vous pouvez exécuter 'npm run setup' pour configurer le port et le token."

case "$(uname -s)" in
  Linux) create_systemd_service ;;
  Darwin) create_launchd_service ;;
  *) echo "Création de service non prise en charge" ;;
esac
