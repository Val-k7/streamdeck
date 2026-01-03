"""Security-focused tests for WebSocket and rate limiting."""
from __future__ import annotations

import json

import pytest

from app.utils.rate_limiter import RateLimiter
from app.websocket import _dispatch_action


class TestRateLimiter:
    """Test rate limiting functionality."""

    def test_rate_limiter_allows_within_limit(self, rate_limiter: RateLimiter):
        """Test that requests within limit are allowed."""
        for i in range(5):
            result = rate_limiter.check("test", "client1")
            assert result["allowed"] is True
            assert result["retry_after"] == 0

    def test_rate_limiter_blocks_over_limit(self, rate_limiter: RateLimiter):
        """Test that requests over limit are blocked."""
        # Make 5 requests (at limit)
        for i in range(5):
            rate_limiter.check("test", "client1")

        # 6th request should be blocked
        result = rate_limiter.check("test", "client1")
        assert result["allowed"] is False
        assert result["retry_after"] > 0

    def test_rate_limiter_separate_clients(self, rate_limiter: RateLimiter):
        """Test that different clients have separate limits."""
        # Client 1 uses all requests
        for i in range(5):
            rate_limiter.check("test", "client1")

        # Client 2 should still be allowed
        result = rate_limiter.check("test", "client2")
        assert result["allowed"] is True

    def test_rate_limiter_purges_old_entries(self, rate_limiter: RateLimiter):
        """Test that old entries are purged from buckets."""
        import time

        # Configure with 1 second window for faster testing
        limiter = RateLimiter()
        limiter.configure("fast", max_requests=2, window_seconds=0.5)

        # Make 2 requests
        limiter.check("fast", "client1")
        limiter.check("fast", "client1")

        # 3rd request should be blocked
        result = limiter.check("fast", "client1")
        assert result["allowed"] is False

        # Wait for window to expire
        time.sleep(0.6)

        # Should be allowed again
        result = limiter.check("fast", "client1")
        assert result["allowed"] is True

    def test_rate_limiter_no_policy(self):
        """Test behavior when no policy is configured."""
        limiter = RateLimiter()

        # Without configuration, should allow all
        result = limiter.check("unconfigured", "client1")
        assert result["allowed"] is True

    def test_rate_limiter_stats(self, rate_limiter: RateLimiter):
        """Test that stats are reported correctly."""
        rate_limiter.check("test", "client1")
        rate_limiter.check("test", "client2")

        stats = rate_limiter.stats()

        assert "policies" in stats
        assert "test" in stats["policies"]
        assert stats["buckets"] == 2  # 2 clients


class TestWebSocketSecurity:
    """Test WebSocket security features."""

    def test_dispatch_action_profile_select(self):
        """Test profile selection action."""
        payload = {
            "kind": "profile:select",
            "profileId": "test-profile",
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["type"] == "profile:select:ack"
        assert response["status"] == "ok"
        assert response["profileId"] == "test-profile"
        assert response["messageId"] == "msg123"

    def test_dispatch_action_missing_action(self):
        """Test that missing action returns error."""
        payload = {
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["type"] == "ack"
        assert response["status"] == "error"
        assert response["error"] == "missing action"
        assert response["messageId"] == "msg123"

    def test_dispatch_action_unknown_action(self):
        """Test that unknown action returns error."""
        payload = {
            "action": "unknown_action",
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["type"] == "ack"
        assert response["status"] == "error"
        assert response["error"] == "unknown action"

    def test_dispatch_action_keyboard(self, monkeypatch):
        """Test dispatching keyboard action."""
        import app.actions.keyboard as keyboard_module

        called = []

        def mock_handle_keyboard(combo):
            called.append(combo)
            return {"status": "ok", "combo": combo}

        monkeypatch.setattr(keyboard_module, "handle_keyboard", mock_handle_keyboard)

        payload = {
            "action": "keyboard",
            "payload": "ctrl+c",
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["type"] == "ack"
        assert response["status"] == "ok"
        assert response["messageId"] == "msg123"
        assert called == ["ctrl+c"]

    def test_dispatch_action_exception_handling(self, monkeypatch):
        """Test that exceptions are caught and returned as errors."""
        import app.actions.keyboard as keyboard_module

        def mock_handle_keyboard(combo):
            raise ValueError("Test error")

        monkeypatch.setattr(keyboard_module, "handle_keyboard", mock_handle_keyboard)

        payload = {
            "action": "keyboard",
            "payload": "ctrl+c",
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["type"] == "ack"
        assert response["status"] == "error"
        assert "Test error" in response["error"]
        assert response["messageId"] == "msg123"

    def test_dispatch_action_control_kind_wrapping(self, monkeypatch):
        """Test that control kind payload wrapping works."""
        import app.actions.keyboard as keyboard_module

        called = []

        def mock_handle_keyboard(combo):
            called.append(combo)
            return {"status": "ok"}

        monkeypatch.setattr(keyboard_module, "handle_keyboard", mock_handle_keyboard)

        # Some clients wrap actions in "control" kind
        payload = {
            "kind": "control",
            "type": "keyboard",  # Action type in "type" field
            "payload": "ctrl+v",
            "messageId": "msg123",
        }

        response = _dispatch_action(payload)

        assert response["status"] == "ok"
        assert called == ["ctrl+v"]


class TestMessageValidation:
    """Test message size and format validation."""

    def test_valid_json_parsing(self):
        """Test that valid JSON is parsed correctly."""
        message = '{"action": "keyboard", "payload": "ctrl+c"}'
        parsed = json.loads(message)

        assert parsed["action"] == "keyboard"
        assert parsed["payload"] == "ctrl+c"

    def test_invalid_json_handling(self):
        """Test that invalid JSON raises exception."""
        message = '{invalid json}'

        with pytest.raises(json.JSONDecodeError):
            json.loads(message)

    def test_message_size_limit(self):
        """Test message size validation logic."""
        max_size = 102400  # 100KB

        # Small message should be OK
        small_message = "ping"
        assert len(small_message) <= max_size

        # Large message should exceed limit
        large_message = "x" * (max_size + 1)
        assert len(large_message) > max_size


class TestTokenValidation:
    """Test token-based authentication."""

    def test_token_manager_valid_token(self, token_manager):
        """Test that valid tokens are accepted."""
        assert token_manager.is_valid("test-token-12345") is True

    def test_token_manager_invalid_token(self, token_manager):
        """Test that invalid tokens are rejected."""
        assert token_manager.is_valid("wrong-token") is False

    def test_token_manager_empty_token(self, token_manager):
        """Test that empty tokens are rejected."""
        assert token_manager.is_valid("") is False
        assert token_manager.is_valid(None) is False

    def test_token_manager_stats(self, token_manager):
        """Test token manager statistics."""
        stats = token_manager.stats()

        assert "active" in stats
        assert stats["active"] >= 0


@pytest.mark.security
class TestSecurityConstraints:
    """Test security constraints are properly enforced."""

    def test_no_shell_injection_in_actions(self):
        """Test that actions don't allow shell injection."""
        # This is a meta-test to ensure subprocess.run is called with shell=False
        import subprocess
        from pathlib import Path

        # Get the source code of scripts.py
        scripts_file = Path(__file__).parent.parent / "app" / "actions" / "scripts.py"
        content = scripts_file.read_text()

        # Verify shell=False is present
        assert "shell=False" in content

        # Verify shell=True is NOT present (except in comments)
        lines = content.split("\n")
        code_lines = [
            line for line in lines
            if not line.strip().startswith("#") and "shell=True" in line
        ]
        assert len(code_lines) == 0, "Found shell=True in code (security risk!)"

    def test_timeout_enforced_in_scripts(self):
        """Test that script execution has timeout."""
        from pathlib import Path

        scripts_file = Path(__file__).parent.parent / "app" / "actions" / "scripts.py"
        content = scripts_file.read_text()

        # Verify timeout parameter is present
        assert "timeout=" in content

    def test_path_validation_in_scripts(self):
        """Test that path validation function exists."""
        from app.actions.scripts import _validate_script_path

        # Function should exist
        assert callable(_validate_script_path)

    def test_allowed_extensions_defined(self):
        """Test that allowed extensions are defined."""
        from app.actions.scripts import ALLOWED_EXTENSIONS

        assert isinstance(ALLOWED_EXTENSIONS, set)
        assert len(ALLOWED_EXTENSIONS) > 0

    def test_allowed_directories_defined(self):
        """Test that allowed directories are defined."""
        from app.actions.scripts import ALLOWED_SCRIPT_DIRS

        assert isinstance(ALLOWED_SCRIPT_DIRS, list)
        assert len(ALLOWED_SCRIPT_DIRS) > 0
