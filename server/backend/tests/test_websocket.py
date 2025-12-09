"""WebSocket integration tests using the real FastAPI app.

These tests perform a handshake to obtain a token, then exercise auth,
ping/pong, invalid JSON handling, and a basic action ack. No mocks are used;
modules are reloaded per test to pick up the temp config.
"""

from __future__ import annotations

import os
import sys
from typing import Tuple

import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect


# Modules that depend on runtime settings; remove them to force a clean reload.
MODULES_TO_RESET = [
    "app.config",
    "app.utils.token_manager",
    "app.routes.tokens",
    "app.routes.profiles",
    "app.routes.health",
    "app.routes.discovery",
    "app.routes.plugins",
    "app.websocket",
    "app.main",
]


def _fresh_client(tmp_path) -> Tuple[TestClient, str]:
    os.environ["DECK_DECK_DATA_DIR"] = str(tmp_path)
    os.environ["DECK_DECK_TOKEN"] = "test-deck-token"
    os.environ["DECK_HANDSHAKE_SECRET"] = "test-handshake"

    for name in MODULES_TO_RESET:
        sys.modules.pop(name, None)

    from app.config import reset_settings_cache  # type: ignore

    reset_settings_cache()

    import app.main as main  # type: ignore
    import app.routes.tokens as tokens  # type: ignore

    return TestClient(main.app), tokens.settings.deck_token


@pytest.fixture()
def client(tmp_path):
    return _fresh_client(tmp_path)


def test_websocket_rejects_invalid_token(client):
    test_client, _ = client

    with pytest.raises(WebSocketDisconnect) as exc:
        with test_client.websocket_connect(
            "/ws", headers={"Authorization": "Bearer wrong-token"}
        ):
            pass

    assert exc.value.code == 4001


def test_websocket_ping_and_action_ack(client):
    test_client, token = client

    with test_client.websocket_connect(
        "/ws", headers={"Authorization": f"Bearer {token}"}
    ) as websocket:
        websocket.send_text("ping")
        assert websocket.receive_text() == "pong"

        websocket.send_json(
            {
                "action": "processes",
                "payload": {"limit": 1},
                "messageId": "msg-1",
            }
        )
        response = websocket.receive_json()
        assert response["type"] == "ack"
        assert response["status"] in ("ok", "success")
        assert response["messageId"] == "msg-1"


def test_websocket_invalid_json_returns_error(client):
    test_client, token = client

    with test_client.websocket_connect(
        "/ws", headers={"Authorization": f"Bearer {token}"}
    ) as ws:
        ws.send_text("not-json")
        message = ws.receive_json()
        assert message["type"] == "error"
        assert message["error"] == "invalid_json"
