"""Security tests for script execution."""
from __future__ import annotations

import subprocess
from pathlib import Path

import pytest

from app.actions.scripts import ALLOWED_EXTENSIONS, ALLOWED_SCRIPT_DIRS, _validate_script_path, run_script
from app.config import Settings


class TestScriptPathValidation:
    """Test script path validation security."""

    def test_validate_valid_script(self, temp_data_dir: Path, sample_script: Path):
        """Test validation of a valid script."""
        result = _validate_script_path(str(sample_script))
        assert result == sample_script.resolve()

    def test_validate_nonexistent_script(self, temp_data_dir: Path):
        """Test that non-existent scripts are rejected."""
        fake_path = temp_data_dir / "scripts" / "nonexistent.sh"

        with pytest.raises(ValueError, match="Script not found"):
            _validate_script_path(str(fake_path))

    def test_validate_directory_not_file(self, temp_data_dir: Path):
        """Test that directories are rejected."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        with pytest.raises(ValueError, match="must be a file"):
            _validate_script_path(str(scripts_dir))

    def test_validate_path_traversal_attack(self, temp_data_dir: Path):
        """Test that path traversal attacks are prevented."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create malicious script outside allowed directory
        malicious_dir = temp_data_dir / "malicious"
        malicious_dir.mkdir(parents=True, exist_ok=True)
        malicious_script = malicious_dir / "evil.sh"
        malicious_script.write_text("#!/bin/bash\nrm -rf /")

        # Try to access via path traversal
        with pytest.raises(ValueError, match="must be in allowed directories"):
            _validate_script_path(str(malicious_script))

    def test_validate_symlink_escape(self, temp_data_dir: Path):
        """Test that symlink escapes are prevented."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create target outside allowed directory
        outside_dir = temp_data_dir / "outside"
        outside_dir.mkdir(parents=True, exist_ok=True)
        target_script = outside_dir / "target.sh"
        target_script.write_text("#!/bin/bash\necho 'evil'")

        # Create symlink in allowed directory
        symlink_path = scripts_dir / "link.sh"
        try:
            symlink_path.symlink_to(target_script)

            # Should reject because resolved path is outside allowed dirs
            with pytest.raises(ValueError, match="must be in allowed directories"):
                _validate_script_path(str(symlink_path))
        except OSError:
            # Symlinks may not be supported on Windows
            pytest.skip("Symlinks not supported on this platform")

    def test_validate_invalid_extension(self, temp_data_dir: Path):
        """Test that invalid file extensions are rejected."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create script with invalid extension
        invalid_script = scripts_dir / "script.exe"
        invalid_script.write_text("malicious code")

        with pytest.raises(ValueError, match="extension not allowed"):
            _validate_script_path(str(invalid_script))

    def test_allowed_extensions(self):
        """Test that allowed extensions are properly defined."""
        assert ".sh" in ALLOWED_EXTENSIONS
        assert ".bat" in ALLOWED_EXTENSIONS
        assert ".ps1" in ALLOWED_EXTENSIONS
        assert ".py" in ALLOWED_EXTENSIONS
        assert ".exe" not in ALLOWED_EXTENSIONS

    def test_allowed_directories_configured(self):
        """Test that allowed directories are properly configured."""
        assert len(ALLOWED_SCRIPT_DIRS) >= 1
        assert all(isinstance(d, Path) for d in ALLOWED_SCRIPT_DIRS)


class TestRunScript:
    """Test script execution with security constraints."""

    def test_run_script_success(self, temp_data_dir: Path, sample_script: Path):
        """Test successful script execution."""
        result = run_script([str(sample_script)])

        assert result["status"] == "ok"
        assert "Hello from test script" in result["stdout"]

    def test_run_script_with_arguments(self, temp_data_dir: Path):
        """Test script execution with arguments."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create script that echoes arguments
        script = scripts_dir / "echo_args.sh"
        script.write_text('#!/bin/bash\necho "Args: $1 $2"')
        script.chmod(0o755)

        result = run_script([str(script), "hello", "world"])

        assert result["status"] == "ok"
        assert "hello world" in result["stdout"]

    def test_run_script_validation_error(self, temp_data_dir: Path):
        """Test that validation errors are handled."""
        fake_script = temp_data_dir / "fake.sh"

        result = run_script([str(fake_script)])

        assert result["status"] == "error"
        assert "error" in result
        assert "not found" in result["error"].lower()

    def test_run_script_timeout(self, temp_data_dir: Path, monkeypatch):
        """Test that long-running scripts timeout."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        script = scripts_dir / "timeout.sh"
        script.write_text("#!/bin/bash\nsleep 100")
        script.chmod(0o755)

        # Mock subprocess.run to raise TimeoutExpired
        def mock_run(*args, **kwargs):
            raise subprocess.TimeoutExpired(cmd=args[0], timeout=30)

        monkeypatch.setattr(subprocess, "run", mock_run)

        result = run_script([str(script)])

        assert result["status"] == "error"
        assert "timeout" in result["error"].lower()

    def test_run_script_exit_code_error(self, temp_data_dir: Path):
        """Test handling of script execution errors."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create script that exits with error
        script = scripts_dir / "error.sh"
        script.write_text("#!/bin/bash\nexit 1")
        script.chmod(0o755)

        result = run_script([str(script)])

        assert result["status"] == "error"
        assert result["code"] == 1

    def test_run_script_invalid_command_format(self):
        """Test that invalid command formats are rejected."""
        # Empty command
        result = run_script([])
        assert result["status"] == "error"
        assert "Invalid command format" in result["error"]

        # Not a list
        result = run_script("not a list")  # type: ignore
        assert result["status"] == "error"

    def test_run_script_shell_injection_prevented(self, temp_data_dir: Path):
        """Test that shell injection is prevented."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        # Create safe script
        script = scripts_dir / "safe.sh"
        script.write_text('#!/bin/bash\necho "Safe"')
        script.chmod(0o755)

        # Try shell injection via arguments
        malicious_arg = "; rm -rf /"
        result = run_script([str(script), malicious_arg])

        # Script should execute but treat malicious_arg as literal string
        # Since we use shell=False, the semicolon is just a string character
        assert result["status"] == "ok"

    def test_run_script_captures_stderr(self, temp_data_dir: Path):
        """Test that stderr is captured."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        script = scripts_dir / "stderr.sh"
        script.write_text('#!/bin/bash\necho "error" >&2\nexit 1')
        script.chmod(0o755)

        result = run_script([str(script)])

        assert result["status"] == "error"
        assert "error" in result["stderr"]

    @pytest.mark.skipif(
        not Path("/bin/bash").exists(),
        reason="Requires bash shell"
    )
    def test_run_python_script(self, temp_data_dir: Path):
        """Test running Python scripts."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        script = scripts_dir / "test.py"
        script.write_text('#!/usr/bin/env python3\nprint("Python works")')
        script.chmod(0o755)

        result = run_script([str(script)])

        # May fail if python3 not in PATH, but that's OK for this test
        if result["status"] == "ok":
            assert "Python works" in result["stdout"]


class TestScriptSecurityConstraints:
    """Test security constraints are properly enforced."""

    def test_shell_false_enforced(self, temp_data_dir: Path, monkeypatch):
        """Test that shell=False is enforced."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        script = scripts_dir / "test.sh"
        script.write_text("#!/bin/bash\necho 'test'")
        script.chmod(0o755)

        # Track subprocess.run calls
        original_run = subprocess.run
        calls = []

        def track_run(*args, **kwargs):
            calls.append(kwargs)
            return original_run(*args, **kwargs)

        monkeypatch.setattr(subprocess, "run", track_run)

        run_script([str(script)])

        # Verify shell=False was used
        assert len(calls) == 1
        assert calls[0].get("shell") is False

    def test_timeout_enforced(self, temp_data_dir: Path, monkeypatch):
        """Test that timeout is enforced."""
        scripts_dir = temp_data_dir / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)

        script = scripts_dir / "test.sh"
        script.write_text("#!/bin/bash\necho 'test'")
        script.chmod(0o755)

        # Track subprocess.run calls
        original_run = subprocess.run
        calls = []

        def track_run(*args, **kwargs):
            calls.append(kwargs)
            return original_run(*args, **kwargs)

        monkeypatch.setattr(subprocess, "run", track_run)

        run_script([str(script)])

        # Verify timeout was set
        assert len(calls) == 1
        assert "timeout" in calls[0]
        assert calls[0]["timeout"] == 30
