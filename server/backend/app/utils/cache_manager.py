from __future__ import annotations

import time
from collections import OrderedDict
from typing import Any, Optional


class CacheEntry:
    __slots__ = ("value", "expires_at")

    def __init__(self, value: Any, ttl_ms: int):
        self.value = value
        self.expires_at = time.time() + ttl_ms / 1000.0 if ttl_ms > 0 else float("inf")

    def is_expired(self) -> bool:
        return time.time() > self.expires_at


class CacheManager:
    def __init__(self, max_entries: int = 128):
        self.max_entries = max_entries
        self._store: OrderedDict[str, CacheEntry] = OrderedDict()

    def get(self, key: str) -> Optional[Any]:
        entry = self._store.get(key)
        if not entry:
            return None
        if entry.is_expired():
            self._store.pop(key, None)
            return None
        # move to end for LRU
        self._store.move_to_end(key)
        return entry.value

    def set(self, key: str, value: Any, ttl_ms: int = 0) -> None:
        if key in self._store:
            self._store.move_to_end(key)
        self._store[key] = CacheEntry(value, ttl_ms)
        if len(self._store) > self.max_entries:
            self._store.popitem(last=False)

    def delete(self, key: str) -> None:
        self._store.pop(key, None)

    def stats(self) -> dict:
        return {"size": len(self._store), "capacity": self.max_entries}
