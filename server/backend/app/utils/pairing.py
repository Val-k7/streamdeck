from __future__ import annotations

import secrets
import time
from dataclasses import dataclass
from typing import Dict, Optional

import qrcode


def _now() -> float:
    return time.time()


@dataclass
class PairingCode:
    code: str
    server_id: str
    expires_at: float
    fingerprint: Optional[str] = None


class PairingManager:
    def __init__(self, ttl_seconds: int = 300):
        self.ttl = ttl_seconds
        self._codes: Dict[str, PairingCode] = {}
        self._paired: Dict[str, str] = {}

    def generate(self, server_id: str) -> PairingCode:
        code = secrets.token_hex(4)
        entry = PairingCode(code=code, server_id=server_id, expires_at=_now() + self.ttl)
        self._codes[code] = entry
        return entry

    def validate(self, code: str, server_id: str) -> bool:
        entry = self._codes.get(code)
        if not entry:
            return False
        if entry.expires_at < _now():
            self._codes.pop(code, None)
            return False
        return entry.server_id == server_id

    def finalize(self, code: str, server_id: str, fingerprint: Optional[str]) -> bool:
        if not self.validate(code, server_id):
            return False
        self._paired[server_id] = fingerprint or ""
        self._codes.pop(code, None)
        return True

    def paired_servers(self) -> Dict[str, str]:
        return dict(self._paired)

    def qr_png_bytes(self, data: str) -> bytes:
        img = qrcode.make(data)
        buf = bytearray()
        img.save(buf, format="PNG")  # type: ignore[arg-type]
        return bytes(buf)
