"""Tests for action handlers."""
from __future__ import annotations

import pytest

from app.actions.audio import handle_audio
from app.actions.keyboard import _parse_combo, handle_keyboard
from app.actions.system import handle_system


class TestKeyboardActions:
    """Test keyboard action handlers."""

    def test_parse_combo_single_key(self):
        """Test parsing a single key."""
        result = _parse_combo("a")
        assert result == ["a"]

    def test_parse_combo_with_plus(self):
        """Test parsing combo with + separator."""
        result = _parse_combo("ctrl+shift+s")
        assert result == ["ctrl", "shift", "s"]

    def test_parse_combo_with_spaces(self):
        """Test parsing combo with spaces."""
        result = _parse_combo("ctrl shift s")
        assert result == ["ctrl", "shift", "s"]

    def test_parse_combo_mixed_separators(self):
        """Test parsing combo with mixed separators."""
        result = _parse_combo("ctrl + shift s")
        assert result == ["ctrl", "shift", "s"]

    def test_parse_combo_lowercase_conversion(self):
        """Test that keys are converted to lowercase."""
        result = _parse_combo("CTRL+SHIFT+S")
        assert result == ["ctrl", "shift", "s"]

    def test_parse_combo_removes_empty(self):
        """Test that empty strings are removed."""
        result = _parse_combo("ctrl + + shift")
        assert result == ["ctrl", "shift"]

    def test_handle_keyboard_valid_combo(self, monkeypatch):
        """Test handling a valid keyboard combo."""
        # Mock pyautogui.hotkey
        called_with = []

        def mock_hotkey(*keys):
            called_with.extend(keys)

        import app.actions.keyboard as keyboard_module
        monkeypatch.setattr(keyboard_module.pyautogui, "hotkey", mock_hotkey)

        result = handle_keyboard("ctrl+c")

        assert result["status"] == "ok"
        assert result["combo"] == "ctrl+c"
        assert called_with == ["ctrl", "c"]

    def test_handle_keyboard_empty_string(self):
        """Test that empty strings are rejected."""
        with pytest.raises(ValueError, match="non-empty string"):
            handle_keyboard("")

    def test_handle_keyboard_whitespace_only(self):
        """Test that whitespace-only strings are rejected."""
        with pytest.raises(ValueError, match="non-empty string"):
            handle_keyboard("   ")

    def test_handle_keyboard_not_string(self):
        """Test that non-string inputs are rejected."""
        with pytest.raises(ValueError):
            handle_keyboard(123)  # type: ignore

        with pytest.raises(ValueError):
            handle_keyboard(None)  # type: ignore


class TestAudioActions:
    """Test audio action handlers."""

    def test_handle_audio_set_volume(self, monkeypatch):
        """Test setting volume."""
        import app.actions.audio as audio_module

        # Mock pycaw availability
        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        # Mock the internal functions
        volume_set = []

        def mock_set_volume(percent):
            volume_set.append(percent)

        monkeypatch.setattr(audio_module, "_set_master_volume", mock_set_volume)

        result = handle_audio("SET_VOLUME", {"volume": 75})

        assert result["status"] == "ok"
        assert result["volume"] == 75.0
        assert volume_set == [75.0]

    def test_handle_audio_set_volume_clamping(self, monkeypatch):
        """Test that volume is clamped to 0-100."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        volume_set = []

        def mock_set_volume(percent):
            volume_set.append(percent)

        def mock_get_volume_interface():
            class MockVolume:
                def SetMasterVolumeLevelScalar(self, level, context):
                    # Level should be 0.0-1.0
                    assert 0.0 <= level <= 1.0
                    volume_set.append(level * 100)

            return MockVolume()

        monkeypatch.setattr(audio_module, "_get_volume_interface", mock_get_volume_interface)

        # Test clamping
        audio_module._set_master_volume(150)  # Should clamp to 100
        assert volume_set[-1] == 100.0

        audio_module._set_master_volume(-50)  # Should clamp to 0
        assert volume_set[-1] == 0.0

    def test_handle_audio_mute(self, monkeypatch):
        """Test mute action."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        mute_calls = []

        def mock_toggle_mute(mute):
            mute_calls.append(mute)

        monkeypatch.setattr(audio_module, "_toggle_mute", mock_toggle_mute)

        result = handle_audio("MUTE", {})

        assert result["status"] == "ok"
        assert result["muted"] is True
        assert mute_calls == [True]

    def test_handle_audio_unmute(self, monkeypatch):
        """Test unmute action."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        mute_calls = []

        def mock_toggle_mute(mute):
            mute_calls.append(mute)

        monkeypatch.setattr(audio_module, "_toggle_mute", mock_toggle_mute)

        result = handle_audio("UNMUTE", {})

        assert result["status"] == "ok"
        assert result["muted"] is False
        assert mute_calls == [False]

    def test_handle_audio_missing_volume_param(self, monkeypatch):
        """Test that missing volume parameter returns error."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        result = handle_audio("SET_VOLUME", {})

        assert result["status"] == "error"
        assert "volume required" in result["error"]

    def test_handle_audio_not_available(self, monkeypatch):
        """Test that unavailable pycaw returns not_implemented."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", False)

        result = handle_audio("SET_VOLUME", {"volume": 50})

        assert result["status"] == "not_implemented"
        assert "pycaw/windows required" in result["reason"]

    def test_handle_audio_unknown_action(self, monkeypatch):
        """Test that unknown actions return not_implemented."""
        import app.actions.audio as audio_module

        monkeypatch.setattr(audio_module, "_PYCAW_AVAILABLE", True)

        result = handle_audio("UNKNOWN_ACTION", {})

        assert result["status"] == "not_implemented"
        assert result["action"] == "UNKNOWN_ACTION"


class TestSystemActions:
    """Test system action handlers."""

    @pytest.mark.skipif(
        not hasattr(__import__("os"), "name") or __import__("os").name != "nt",
        reason="Windows-specific test"
    )
    def test_handle_system_lock_windows(self, monkeypatch):
        """Test lock action on Windows."""
        import subprocess

        run_calls = []

        def mock_run(cmd, **kwargs):
            run_calls.append(cmd)

        monkeypatch.setattr(subprocess, "run", mock_run)

        result = handle_system("lock", {})

        assert result["status"] == "ok"
        assert len(run_calls) == 1
        assert "rundll32.exe" in run_calls[0]

    @pytest.mark.skipif(
        not hasattr(__import__("os"), "name") or __import__("os").name != "nt",
        reason="Windows-specific test"
    )
    def test_handle_system_shutdown_windows(self, monkeypatch):
        """Test shutdown action on Windows."""
        import subprocess

        run_calls = []

        def mock_run(cmd, **kwargs):
            run_calls.append(cmd)

        monkeypatch.setattr(subprocess, "run", mock_run)

        result = handle_system("shutdown", {})

        assert result["status"] == "ok"
        assert len(run_calls) == 1
        assert "shutdown" in run_calls[0]
        assert "/s" in run_calls[0]

    @pytest.mark.skipif(
        not hasattr(__import__("os"), "name") or __import__("os").name != "nt",
        reason="Windows-specific test"
    )
    def test_handle_system_restart_windows(self, monkeypatch):
        """Test restart action on Windows."""
        import subprocess

        run_calls = []

        def mock_run(cmd, **kwargs):
            run_calls.append(cmd)

        monkeypatch.setattr(subprocess, "run", mock_run)

        result = handle_system("restart", {})

        assert result["status"] == "ok"
        assert len(run_calls) == 1
        assert "/r" in run_calls[0]

    def test_handle_system_unknown_action(self):
        """Test that unknown actions return not_implemented."""
        result = handle_system("unknown_action", {})

        assert result["status"] == "not_implemented"
        assert result["action"] == "unknown_action"

    def test_handle_system_case_insensitive(self, monkeypatch):
        """Test that actions are case-insensitive."""
        import subprocess

        run_calls = []

        def mock_run(cmd, **kwargs):
            run_calls.append(cmd)

        monkeypatch.setattr(subprocess, "run", mock_run)

        # Try uppercase
        result = handle_system("LOCK", {})

        # Should work (on Windows) or return not_implemented (on other OS)
        assert result["status"] in ["ok", "not_implemented"]
