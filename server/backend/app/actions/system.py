from __future__ import annotations

import os
import subprocess
from typing import Any, Dict


def handle_system(action: str, payload: Dict[str, Any] | None = None) -> dict:
    payload = payload or {}
    action_upper = (action or payload.get("action") or "").lower()

    if os.name == "nt":
        if action_upper == "lock":
            subprocess.run(["rundll32.exe", "user32.dll,LockWorkStation"], check=False)
            return {"status": "ok"}
        if action_upper == "shutdown":
            subprocess.run(["shutdown", "/s", "/t", "0"], check=False)
            return {"status": "ok"}
        if action_upper == "restart":
            subprocess.run(["shutdown", "/r", "/t", "0"], check=False)
            return {"status": "ok"}

    return {"status": "not_implemented", "action": action_upper}
