from __future__ import annotations

import time
from typing import Dict, Tuple


class RateLimiter:
    def __init__(self):
        self.policies: Dict[str, Tuple[int, float]] = {}
        self.buckets: Dict[str, list[float]] = {}

    def configure(self, scope: str, max_requests: int, window_seconds: float) -> None:
        self.policies[scope] = (max_requests, window_seconds)

    def check(self, scope: str, key: str):
        max_requests, window = self.policies.get(scope, (0, 0))
        if max_requests == 0:
            return {"allowed": True, "retry_after": 0}

        bucket_key = f"{scope}:{key}"
        now = time.time()
        window_start = now - window
        bucket = self.buckets.setdefault(bucket_key, [])
        # purge old entries
        bucket[:] = [ts for ts in bucket if ts >= window_start]
        if len(bucket) >= max_requests:
            retry_after = bucket[0] + window - now
            return {"allowed": False, "retry_after": max(retry_after, 0)}
        bucket.append(now)
        return {"allowed": True, "retry_after": 0}

    def stats(self) -> Dict:
        return {"policies": self.policies, "buckets": len(self.buckets)}
