from __future__ import annotations

import os
import subprocess
from pathlib import Path
from typing import Sequence

from ..config import get_settings

settings = get_settings()

# Whitelist of allowed script directories (configurable via env)
ALLOWED_SCRIPT_DIRS = [
    settings.deck_data_dir / "scripts",
    Path.home() / ".config" / "control-deck" / "scripts",
]

# Whitelist of allowed script extensions
ALLOWED_EXTENSIONS = {".sh", ".bat", ".ps1", ".py"}


def _validate_script_path(script_path: str) -> Path:
    """Validate that the script path is safe and allowed.

    Raises:
        ValueError: If the script path is invalid or not allowed
    """
    try:
        resolved = Path(script_path).resolve()
    except (ValueError, OSError) as exc:
        raise ValueError(f"Invalid script path: {script_path}") from exc

    # Check if file exists
    if not resolved.exists():
        raise ValueError(f"Script not found: {script_path}")

    # Check if it's a file (not directory)
    if not resolved.is_file():
        raise ValueError(f"Script path must be a file: {script_path}")

    # Check if within allowed directories
    is_allowed = any(
        resolved.is_relative_to(allowed_dir)
        for allowed_dir in ALLOWED_SCRIPT_DIRS
        if allowed_dir.exists()
    )
    if not is_allowed:
        raise ValueError(
            f"Script must be in allowed directories: {[str(d) for d in ALLOWED_SCRIPT_DIRS]}"
        )

    # Check file extension
    if resolved.suffix.lower() not in ALLOWED_EXTENSIONS:
        raise ValueError(
            f"Script extension not allowed. Allowed: {ALLOWED_EXTENSIONS}"
        )

    return resolved


def run_script(cmd: Sequence[str]) -> dict:
    """Run a script with security validation.

    Args:
        cmd: Command as a sequence [script_path, arg1, arg2, ...]

    Returns:
        Dict with status, stdout, stderr
    """
    if not isinstance(cmd, (list, tuple)) or len(cmd) == 0:
        return {"status": "error", "error": "Invalid command format"}

    try:
        # Validate script path (first element)
        script_path = _validate_script_path(cmd[0])

        # Rebuild command with validated path
        validated_cmd = [str(script_path)] + list(cmd[1:])

        # Run with shell=False for security
        completed = subprocess.run(
            validated_cmd,
            check=True,
            capture_output=True,
            text=True,
            timeout=30,  # 30 second timeout
            shell=False,  # Never use shell=True
        )
        return {"status": "ok", "stdout": completed.stdout, "stderr": completed.stderr}

    except ValueError as exc:
        # Validation error
        return {"status": "error", "error": str(exc)}
    except subprocess.TimeoutExpired:
        return {"status": "error", "error": "Script execution timeout"}
    except subprocess.CalledProcessError as exc:
        return {"status": "error", "code": exc.returncode, "stdout": exc.stdout, "stderr": exc.stderr}
    except Exception as exc:
        return {"status": "error", "error": f"Unexpected error: {str(exc)}"}
