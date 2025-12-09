from fastapi import APIRouter, Header, HTTPException

from ..config import get_settings
from ..utils.token_manager import get_token_manager

router = APIRouter()
settings = get_settings()
token_manager = get_token_manager(default_token=settings.deck_token)


@router.post("/handshake")
async def handshake(secret: str | None = None, clientId: str | None = None):
    expected = settings.effective_handshake_secret
    if not secret:
        raise HTTPException(status_code=400, detail="secret required")
    if secret != expected:
        raise HTTPException(status_code=401, detail="invalid secret")
    issued = token_manager.issue_token(clientId, {})
    return {
        "token": issued.token,
        "expiresAt": issued.expires_at,
        "clientId": clientId,
    }


@router.post("/handshake/revoke")
async def revoke(token: str | None = None):
    if not token:
        raise HTTPException(status_code=400, detail="token required")
    ok = token_manager.revoke_token(token)
    if not ok:
        raise HTTPException(status_code=404, detail="token not found or default")
    return {"status": "revoked"}


@router.get("/info")
async def token_info(authorization: str | None = Header(default=None)):
    token = _extract_token(authorization)
    if not token:
        raise HTTPException(status_code=401, detail="token required")
    info = token_manager.get_info(token)
    if not info:
        raise HTTPException(status_code=404, detail="token not found or expired")
    return {
        "token": info.token,
        "clientId": info.client_id,
        "metadata": info.metadata,
        "issuedAt": info.issued_at,
        "expiresAt": info.expires_at,
    }


@router.post("/rotate")
async def rotate(authorization: str | None = Header(default=None), clientId: str | None = None):
    token = _extract_token(authorization)
    if not token:
        raise HTTPException(status_code=401, detail="token required")
    try:
        new_token = token_manager.rotate_token(token, clientId, {})
    except ValueError:
        raise HTTPException(status_code=400, detail="invalid token")
    return {
        "token": new_token.token,
        "expiresAt": new_token.expires_at,
        "clientId": clientId,
    }


@router.post("/revoke")
async def revoke_any(token: str | None = None, authorization: str | None = Header(default=None)):
    effective = token or _extract_token(authorization)
    if not effective:
        raise HTTPException(status_code=400, detail="token required")
    ok = token_manager.revoke_token(effective)
    if not ok:
        raise HTTPException(status_code=404, detail="token not found or default")
    return {"status": "revoked"}


def _extract_token(header: str | None) -> str | None:
    if not header:
        return None
    if header.lower().startswith("bearer "):
        return header[7:]
    return header
