from __future__ import annotations

import os
from ctypes import POINTER, Structure, byref, c_int, windll  # type: ignore
from typing import Any, Dict

from .base import BasePlugin


class _KeyBdInput(Structure):
    _fields_ = [
        ("wVk", c_int),
        ("wScan", c_int),
        ("dwFlags", c_int),
        ("time", c_int),
        ("dwExtraInfo", POINTER(c_int)),
    ]


class SpotifyPlugin(BasePlugin):
    name = "spotify"

    def load(self) -> None:  # pragma: no cover - no side effects
        return None

    def unload(self) -> None:  # pragma: no cover - no side effects
        return None

    def execute(self, action: str, payload: Dict[str, Any] | None = None) -> Any:
        action_lower = (action or "").lower()

        if action_lower == "play_pause":
            return self._send_media_key(0xB3)
        if action_lower == "next":
            return self._send_media_key(0xB0)
        if action_lower == "previous":
            return self._send_media_key(0xB1)

        return {"status": "not_implemented", "action": action_lower}

    def _send_media_key(self, vk_code: int) -> Dict[str, Any]:
        if os.name != "nt":
            return {"status": "not_implemented", "reason": "windows_only"}

        # Use keybd_event via ctypes for media key simulation
        try:
            windll.user32.keybd_event(vk_code, 0, 0, 0)
            windll.user32.keybd_event(vk_code, 0, 0x0002, 0)
            return {"status": "ok", "vk": vk_code}
        except Exception as exc:  # noqa: BLE001
            return {"status": "error", "error": str(exc)}
