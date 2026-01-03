"""Tests for ProfileManager utility."""
from __future__ import annotations

import json
from pathlib import Path

import pytest

from app.config import Settings
from app.utils.profile_manager import Control, Profile, ProfileManager


class TestProfileManager:
    """Test suite for ProfileManager."""

    def test_init(self, test_settings: Settings):
        """Test ProfileManager initialization."""
        manager = ProfileManager(test_settings)
        assert manager.settings == test_settings
        assert test_settings.profiles_dir.exists()

    def test_aliases_backward_compatibility(self, test_settings: Settings):
        """Test that aliases are defined for backward compatibility."""
        manager = ProfileManager(test_settings)
        assert "audio" in manager.aliases
        assert "streaming" in manager.aliases
        assert "macros" in manager.aliases

    def test_list_profiles_empty(self, test_settings: Settings):
        """Test listing profiles when none exist."""
        manager = ProfileManager(test_settings)
        profiles = manager.list_profiles()
        assert profiles == []

    def test_list_profiles_with_data(self, test_settings: Settings, sample_profile: dict):
        """Test listing profiles with existing data."""
        manager = ProfileManager(test_settings)

        # Create a profile file
        profile_file = test_settings.profiles_dir / "test-profile.json"
        profile_file.write_text(json.dumps(sample_profile), encoding="utf-8")

        profiles = manager.list_profiles()
        assert len(profiles) == 1
        assert profiles[0]["id"] == "test-profile"
        assert profiles[0]["name"] == "Test Profile"

    def test_list_profiles_ignores_invalid_json(self, test_settings: Settings):
        """Test that invalid JSON files are skipped."""
        manager = ProfileManager(test_settings)

        # Create invalid JSON file
        invalid_file = test_settings.profiles_dir / "invalid.json"
        invalid_file.write_text("not valid json{", encoding="utf-8")

        profiles = manager.list_profiles()
        assert profiles == []

    def test_list_profiles_with_aliases(self, test_settings: Settings):
        """Test that aliases are injected when target profiles exist."""
        manager = ProfileManager(test_settings)

        # Create profile with aliased ID
        profile_data = {
            "id": "profile_default_mixer",
            "name": "Audio Mixer",
            "rows": 4,
            "cols": 4,
            "controls": [],
        }
        profile_file = test_settings.profiles_dir / "profile_default_mixer.json"
        profile_file.write_text(json.dumps(profile_data), encoding="utf-8")

        profiles = manager.list_profiles()

        # Should have both original ID and alias
        ids = [p["id"] for p in profiles]
        assert "profile_default_mixer" in ids
        assert "audio" in ids

    def test_get_profile_exists(self, test_settings: Settings, sample_profile: dict):
        """Test getting an existing profile."""
        manager = ProfileManager(test_settings)

        profile_file = test_settings.profiles_dir / "test-profile.json"
        profile_file.write_text(json.dumps(sample_profile), encoding="utf-8")

        profile = manager.get_profile("test-profile")
        assert profile is not None
        assert profile["id"] == "test-profile"
        assert profile["name"] == "Test Profile"

    def test_get_profile_not_exists(self, test_settings: Settings):
        """Test getting a non-existent profile."""
        manager = ProfileManager(test_settings)
        profile = manager.get_profile("non-existent")
        assert profile is None

    def test_get_profile_via_alias(self, test_settings: Settings):
        """Test getting a profile via its alias."""
        manager = ProfileManager(test_settings)

        # Create aliased profile
        profile_data = {
            "id": "profile_default_mixer",
            "name": "Audio Mixer",
            "rows": 4,
            "cols": 4,
            "controls": [],
        }
        profile_file = test_settings.profiles_dir / "profile_default_mixer.json"
        profile_file.write_text(json.dumps(profile_data), encoding="utf-8")

        # Get via alias
        profile = manager.get_profile("audio")
        assert profile is not None
        assert profile["name"] == "Audio Mixer"

    def test_save_profile_success(self, test_settings: Settings, sample_profile: dict):
        """Test saving a profile successfully."""
        manager = ProfileManager(test_settings)

        result = manager.save_profile("test-profile", sample_profile)

        assert isinstance(result, Profile)
        assert result.id == "test-profile"
        assert result.name == "Test Profile"

        # Verify file was created
        profile_file = test_settings.profiles_dir / "test-profile.json"
        assert profile_file.exists()

        # Verify content
        saved_data = json.loads(profile_file.read_text(encoding="utf-8"))
        assert saved_data["id"] == "test-profile"

    def test_save_profile_id_mismatch(self, test_settings: Settings, sample_profile: dict):
        """Test that saving fails when profile ID doesn't match."""
        manager = ProfileManager(test_settings)

        with pytest.raises(ValueError, match="id mismatch"):
            manager.save_profile("wrong-id", sample_profile)

    def test_save_profile_creates_valid_json(self, test_settings: Settings, sample_profile: dict):
        """Test that saved profile is valid JSON with correct formatting."""
        manager = ProfileManager(test_settings)
        manager.save_profile("test-profile", sample_profile)

        profile_file = test_settings.profiles_dir / "test-profile.json"
        content = profile_file.read_text(encoding="utf-8")

        # Should be valid JSON
        parsed = json.loads(content)
        assert parsed["id"] == "test-profile"

        # Should be indented (2 spaces)
        assert "\n  " in content

    def test_delete_profile_exists(self, test_settings: Settings, sample_profile: dict):
        """Test deleting an existing profile."""
        manager = ProfileManager(test_settings)

        # Create profile first
        profile_file = test_settings.profiles_dir / "test-profile.json"
        profile_file.write_text(json.dumps(sample_profile), encoding="utf-8")

        result = manager.delete_profile("test-profile")
        assert result is True
        assert not profile_file.exists()

    def test_delete_profile_not_exists(self, test_settings: Settings):
        """Test deleting a non-existent profile."""
        manager = ProfileManager(test_settings)
        result = manager.delete_profile("non-existent")
        assert result is False

    def test_delete_profile_handles_errors(self, test_settings: Settings, monkeypatch):
        """Test that delete handles exceptions gracefully."""
        manager = ProfileManager(test_settings)

        # Create profile
        profile_file = test_settings.profiles_dir / "test-profile.json"
        profile_file.write_text("{}", encoding="utf-8")

        # Mock unlink to raise exception
        def mock_unlink():
            raise PermissionError("Cannot delete")

        monkeypatch.setattr(profile_file, "unlink", mock_unlink)

        result = manager.delete_profile("test-profile")
        assert result is False


class TestControlModel:
    """Test the Control Pydantic model."""

    def test_control_creation(self):
        """Test creating a Control instance."""
        control = Control(
            id="test-1",
            type="button",
            row=0,
            col=0,
            label="Test",
            colorHex="#FF0000",
            action={"type": "keyboard", "payload": "ctrl+c"},
        )

        assert control.id == "test-1"
        assert control.type == "button"
        assert control.label == "Test"

    def test_control_optional_fields(self):
        """Test that optional fields work correctly."""
        control = Control(id="test-1", type="button", row=0, col=0)

        assert control.label is None
        assert control.colorHex is None
        assert control.action is None


class TestProfileModel:
    """Test the Profile Pydantic model."""

    def test_profile_creation(self, sample_profile: dict):
        """Test creating a Profile instance."""
        profile = Profile(**sample_profile)

        assert profile.id == "test-profile"
        assert profile.name == "Test Profile"
        assert profile.rows == 4
        assert profile.cols == 4
        assert profile.version == 1
        assert len(profile.controls) == 1

    def test_profile_defaults(self):
        """Test Profile default values."""
        profile = Profile(id="test", name="Test", rows=4, cols=4)

        assert profile.version == 1
        assert profile.controls == []
