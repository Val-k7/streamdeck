"""API smoke tests for the Python backend.

These tests cover health endpoints, token lifecycle, and profile CRUD using
the real FastAPI app with a temporary data directory. No mocks are used; we
recreate the app for each test by clearing cached modules so settings pick up
the per-test temp directory.
"""

from __future__ import annotations

import os
import sys
from typing import Tuple

import pytest
from fastapi.testclient import TestClient


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


def _fresh_client(tmp_path) -> Tuple[TestClient, object]:
    os.environ["DECK_DECK_DATA_DIR"] = str(tmp_path)
    os.environ["DECK_DECK_TOKEN"] = "test-deck-token"

    for name in MODULES_TO_RESET:
        sys.modules.pop(name, None)

    from app.config import reset_settings_cache  # type: ignore

    reset_settings_cache()

    import app.main as main  # type: ignore
    import app.routes.tokens as tokens  # type: ignore

    return TestClient(main.app), tokens


@pytest.fixture()
def client(tmp_path):
    return _fresh_client(tmp_path)


def test_health_endpoints(client):
    test_client, _ = client

    r = test_client.get("/health/")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}

    diag = test_client.get("/health/diagnostics")
    assert diag.status_code == 200
    body = diag.json()
    assert "tokens" in body and "rateLimiter" in body and "cache" in body

    perf = test_client.get("/health/performance")
    assert perf.status_code == 200
    assert perf.json()["uptimeSeconds"] >= 0


def test_token_handshake_rotate_and_revoke(client):
    test_client, tokens_module = client
    secret = tokens_module.settings.effective_handshake_secret

    handshake = test_client.post(
        "/tokens/handshake", params={"secret": secret, "clientId": "tester"}
    )
    assert handshake.status_code == 200
    token = handshake.json()["token"]

    info = test_client.get(
        "/tokens/info", headers={"Authorization": f"Bearer {token}"}
    )
    assert info.status_code == 200
    assert info.json()["token"] == token

    rotated = test_client.post(
        "/tokens/rotate",
        headers={"Authorization": f"Bearer {token}"},
        params={"clientId": "tester-rotated"},
    )
    assert rotated.status_code == 200
    new_token = rotated.json()["token"]
    assert new_token != token

    revoke = test_client.post("/tokens/revoke", params={"token": new_token})
    assert revoke.status_code == 200

    # Old token is no longer valid after rotation
    old_info = test_client.get(
        "/tokens/info", headers={"Authorization": f"Bearer {token}"}
    )
    assert old_info.status_code in (401, 404)


def test_profile_crud(client):
    test_client, _ = client

    payload = {
        "id": "test-profile",
        "name": "Test Profile",
        "rows": 1,
        "cols": 1,
        "controls": [],
    }

    save = test_client.post("/profiles/test-profile", json=payload)
    assert save.status_code == 200
    body = save.json()
    assert body["status"] == "saved"
    assert body["profileId"] == "test-profile"

    fetched = test_client.get("/profiles/test-profile")
    assert fetched.status_code == 200
    assert fetched.json()["name"] == "Test Profile"

    listed = test_client.get("/profiles/")
    assert listed.status_code == 200
    ids = {item["id"] for item in listed.json().get("profiles", [])}
    assert "test-profile" in ids
