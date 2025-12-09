from __future__ import annotations

from typing import List

import pyautogui


def _parse_combo(combo: str) -> List[str]:
    parts = [p.strip() for p in combo.replace("+", " ").split()]  # split on + or spaces
    return [p.lower() for p in parts if p]


def handle_keyboard(combo: str) -> dict:
    if not isinstance(combo, str) or not combo.strip():
        raise ValueError("Keyboard payload must be a non-empty string like 'ctrl+shift+s'")
    keys = _parse_combo(combo)
    pyautogui.hotkey(*keys)
    return {"status": "ok", "combo": combo}
