from __future__ import annotations

import tempfile
from pathlib import Path

from PIL import ImageGrab


def take_screenshot() -> dict:
    img = ImageGrab.grab()
    tmp = Path(tempfile.gettempdir()) / f"screenshot_{img.width}x{img.height}.png"
    img.save(tmp)
    return {"status": "ok", "path": str(tmp)}
