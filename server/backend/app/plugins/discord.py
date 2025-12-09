from __future__ import annotations

import os
import subprocess
from typing import Any, Dict

from .base import BasePlugin


class DiscordPlugin(BasePlugin):
    name = "discord"

    def load(self) -> None:  # pragma: no cover - side-effect free
        return None

    def unload(self) -> None:  # pragma: no cover - side-effect free
        return None

    def execute(self, action: str, payload: Dict[str, Any] | None = None) -> Any:
        payload = payload or {}
        action_lower = (action or "").lower()

        if action_lower == "mute":
            return self._toggle_shortcut("^+m")
        if action_lower == "deafen":
            return self._toggle_shortcut("^+d")

        return {"status": "not_implemented", "action": action_lower}

    def _toggle_shortcut(self, shortcut: str) -> Dict[str, Any]:
        if os.name != "nt":
            return {"status": "not_implemented", "reason": "windows_only"}

        script = f"Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.SendKeys]::SendWait('{shortcut}')"
        subprocess.run(["powershell", "-Command", script], check=False)
        return {"status": "ok", "shortcut": shortcut}
