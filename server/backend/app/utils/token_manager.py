from __future__ import annotations

import secrets
import time
from dataclasses import dataclass
from typing import Dict, Optional


@dataclass
class TokenData:
    token: str
    client_id: Optional[str]
    metadata: Dict
    issued_at: float
    expires_at: float


class TokenManager:
    def __init__(self, ttl_seconds: int = 24 * 3600, default_token: Optional[str] = None):
        self.ttl_seconds = ttl_seconds
        self.default_token = default_token
        self._tokens: Dict[str, TokenData] = {}
        if default_token:
            self._tokens[default_token] = TokenData(
                token=default_token,
                client_id=None,
                metadata={"default": True},
                issued_at=time.time(),
                expires_at=float("inf"),
            )

    def issue_token(self, client_id: Optional[str], metadata: Optional[Dict] = None) -> TokenData:
        token = secrets.token_urlsafe(32)
        now = time.time()
        data = TokenData(
            token=token,
            client_id=client_id,
            metadata=metadata or {},
            issued_at=now,
            expires_at=now + self.ttl_seconds,
        )
        self._tokens[token] = data
        return data

    def revoke_token(self, token: str) -> bool:
        if token == self.default_token:
            return False
        return self._tokens.pop(token, None) is not None

    def is_valid(self, token: Optional[str]) -> bool:
        if not token:
            return False
        if token == self.default_token:
            return True
        data = self._tokens.get(token)
        if not data:
            return False
        if data.expires_at < time.time():
            self._tokens.pop(token, None)
            return False
        return True

    def get_info(self, token: str) -> Optional[TokenData]:
        data = self._tokens.get(token)
        if not data:
            return None
        if data.expires_at < time.time():
            self._tokens.pop(token, None)
            return None
        return data

    def rotate_token(self, old_token: str, client_id: Optional[str], metadata: Optional[Dict] = None) -> TokenData:
        if not self.is_valid(old_token):
            raise ValueError("invalid token")
        self.revoke_token(old_token)
        return self.issue_token(client_id, metadata)

    def cleanup(self) -> None:
        now = time.time()
        expired = [t for t, d in self._tokens.items() if d.expires_at < now and t != self.default_token]
        for t in expired:
            self._tokens.pop(t, None)

    def stats(self) -> Dict:
        now = time.time()
        active = [t for t, d in self._tokens.items() if d.expires_at >= now]
        return {
            "active": len(active),
            "default": bool(self.default_token),
        }


# Singleton helper to share the manager across modules
_singleton: Optional[TokenManager] = None


def get_token_manager(default_token: Optional[str] = None, ttl_seconds: int = 24 * 3600) -> TokenManager:
    global _singleton
    if _singleton is None:
        _singleton = TokenManager(ttl_seconds=ttl_seconds, default_token=default_token)
    return _singleton
