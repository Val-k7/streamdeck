from __future__ import annotations

import os
from typing import Any, Dict

from app.actions.obs import handle_obs
from app.config import get_settings
from .base import BasePlugin


class OBSPlugin(BasePlugin):
    name = "obs"

    def __init__(self) -> None:
        self.settings = get_settings()

    def load(self) -> None:
        # Expose OBS settings via env for compatibility with existing tooling
        os.environ.setdefault("OBS_WS_URL", self.settings.obs_ws_url)
        os.environ.setdefault("OBS_WS_PASSWORD", self.settings.obs_ws_password)

    def unload(self) -> None:
        # Nothing to clean up yet
        return None

    def execute(self, action: str, payload: Dict[str, Any] | None = None) -> Any:
        return handle_obs(action, payload or {})
