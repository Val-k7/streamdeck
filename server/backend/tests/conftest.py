"""Pytest configuration and shared fixtures for Control Deck tests."""
from __future__ import annotations

import tempfile
from pathlib import Path
from typing import Generator

import pytest
from fastapi.testclient import TestClient

from app.config import Settings, reset_settings_cache
from app.main import app
from app.utils.rate_limiter import RateLimiter
from app.utils.token_manager import TokenManager


@pytest.fixture
def temp_data_dir() -> Generator[Path, None, None]:
    """Create a temporary data directory for tests."""
    with tempfile.TemporaryDirectory() as tmpdir:
        yield Path(tmpdir)


@pytest.fixture
def test_settings(temp_data_dir: Path) -> Settings:
    """Create test settings with temporary directories."""
    reset_settings_cache()
    settings = Settings(
        deck_data_dir=temp_data_dir,
        deck_token="test-token-12345",
        handshake_secret="test-handshake-secret",
        allowed_origins="http://localhost:3000,http://localhost:4455",
        max_message_size=102400,
        rate_limit_requests=100,
        rate_limit_window=60,
        log_level="debug",
    )
    settings.ensure_runtime_dirs()
    return settings


@pytest.fixture
def client(test_settings: Settings) -> TestClient:
    """Create a FastAPI test client."""
    return TestClient(app)


@pytest.fixture
def rate_limiter() -> RateLimiter:
    """Create a fresh rate limiter instance."""
    limiter = RateLimiter()
    limiter.configure("test", max_requests=5, window_seconds=10)
    return limiter


@pytest.fixture
def token_manager() -> TokenManager:
    """Create a token manager for tests."""
    return TokenManager(default_token="test-token-12345")


@pytest.fixture
def sample_profile() -> dict:
    """Sample profile data for testing."""
    return {
        "id": "test-profile",
        "name": "Test Profile",
        "rows": 4,
        "cols": 4,
        "version": 1,
        "controls": [
            {
                "id": "ctrl-1",
                "type": "button",
                "row": 0,
                "col": 0,
                "label": "Test Button",
                "colorHex": "#FF0000",
                "action": {
                    "type": "keyboard",
                    "payload": "ctrl+c",
                },
            }
        ],
    }


@pytest.fixture
def sample_script(temp_data_dir: Path) -> Path:
    """Create a sample script file for testing."""
    scripts_dir = temp_data_dir / "scripts"
    scripts_dir.mkdir(parents=True, exist_ok=True)

    script_path = scripts_dir / "test_script.sh"
    script_path.write_text("#!/bin/bash\necho 'Hello from test script'")
    script_path.chmod(0o755)

    return script_path


@pytest.fixture
def mock_websocket_client():
    """Mock WebSocket client for testing."""
    class MockWebSocket:
        def __init__(self):
            self.messages_sent = []
            self.messages_received = []
            self.closed = False
            self.close_code = None

            # Mock client attribute
            class MockClient:
                host = "127.0.0.1"
            self.client = MockClient()

            # Mock headers
            self.headers = {}
            self.query_params = {}

        async def accept(self):
            pass

        async def send_text(self, message: str):
            self.messages_sent.append(message)

        async def send_json(self, data: dict):
            self.messages_sent.append(data)

        async def receive_text(self) -> str:
            if self.messages_received:
                return self.messages_received.pop(0)
            raise Exception("No messages to receive")

        async def close(self, code: int = 1000):
            self.closed = True
            self.close_code = code

    return MockWebSocket()
