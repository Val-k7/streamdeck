from fastapi import APIRouter, HTTPException, Request

from ..config import get_settings
from ..utils.discovery import DiscoveryService
from ..utils.pairing import PairingManager

router = APIRouter()
settings = get_settings()
discovery_service = DiscoveryService(settings)
pairing_manager = PairingManager()
discovery_service.start()


@router.get("/")
async def discovery(request: Request):
    client_host = request.client.host if request.client else "unknown"
    protocol = "wss" if settings.tls_key_path and settings.tls_cert_path else "ws"
    host = settings.host if settings.host != "0.0.0.0" else request.headers.get("host", "localhost")
    return {
        "serverId": "python-backend",
        "serverName": "ControlDeck",
        "port": settings.port,
        "protocol": protocol,
        "host": host,
        "capabilities": {
            "tls": bool(settings.tls_key_path and settings.tls_cert_path),
            "websocket": True,
            "profiles": True,
            "plugins": True,
        },
        "client": client_host,
    }


@router.post("/pairing/request")
async def pairing_request():
    code = pairing_manager.generate(server_id="python-backend")
    return {"code": code.code, "expiresAt": code.expires_at, "serverId": code.server_id}


@router.post("/pairing/confirm")
async def pairing_confirm(code: str | None = None, serverId: str | None = None, fingerprint: str | None = None):
    if not code or not serverId:
        raise HTTPException(status_code=400, detail="code and serverId required")
    ok = pairing_manager.finalize(code, serverId, fingerprint)
    if not ok:
        raise HTTPException(status_code=401, detail="invalid or expired code")
    return {"status": "paired", "serverId": serverId}


@router.get("/pairing/servers")
async def pairing_servers():
    return {"servers": pairing_manager.paired_servers()}
