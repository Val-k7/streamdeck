#!/usr/bin/env bash
set -euo pipefail

PORT=${PORT:-4455}
HOST=${HOST:-0.0.0.0}
DATA_DIR=${DECK_DECK_DATA_DIR:-$(dirname "$0")/../data}
HANDSHAKE_SECRET=${DECK_HANDSHAKE_SECRET:-}
DECK_TOKEN=${DECK_DECK_TOKEN:-}

export DECK_DECK_DATA_DIR="$DATA_DIR"
export DECK_DISABLE_DISCOVERY="${DECK_DISABLE_DISCOVERY:-0}"
if [[ -n "$HANDSHAKE_SECRET" ]]; then
  export DECK_HANDSHAKE_SECRET="$HANDSHAKE_SECRET"
fi
if [[ -n "$DECK_TOKEN" ]]; then
  export DECK_DECK_TOKEN="$DECK_TOKEN"
fi

cd "$(dirname "$0")/.."
echo "Starting Control Deck backend on ${HOST}:${PORT} (data dir: ${DATA_DIR})..."
python -m uvicorn app.main:app --host "$HOST" --port "$PORT"
