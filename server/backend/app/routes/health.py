from fastapi import APIRouter

from ..utils.cache_manager import CacheManager
from ..utils.rate_limiter import RateLimiter
from ..utils.token_manager import get_token_manager
import time

router = APIRouter()
cache = CacheManager()
rate_limiter = RateLimiter()
token_manager = get_token_manager()
started_at = time.time()


@router.get("/")
async def healthcheck():
    return {"status": "ok"}


@router.get("/diagnostics")
async def diagnostics():
    return {
        "tokens": token_manager.stats(),
        "rateLimiter": rate_limiter.stats(),
        "cache": cache.stats(),
    }


@router.get("/performance")
async def performance():
    return {"uptimeSeconds": round(time.time() - started_at, 2)}


@router.get("/errors")
async def errors():
    return {"errors": []}
