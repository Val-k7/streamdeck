from __future__ import annotations

import json
from typing import Any, Dict, Set

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from . import actions
from .config import get_settings
from .constants import (
    MESSAGE_TYPE_ACK,
    MESSAGE_TYPE_PROFILE_SELECT_ACK,
    STATUS_ERROR,
    STATUS_OK,
    WS_CLOSE_MESSAGE_TOO_BIG,
    WS_CLOSE_UNAUTHORIZED,
)
from .utils.logger import get_logger
from .utils.rate_limiter import RateLimiter
from .utils.token_manager import get_token_manager

router = APIRouter()
settings = get_settings()
token_manager = get_token_manager(default_token=settings.deck_token)
connections: Set[WebSocket] = set()
rate_limiter = RateLimiter()
rate_limiter.configure("websocket", settings.rate_limit_requests, settings.rate_limit_window)
logger = get_logger(__name__)


@router.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    token = ws.headers.get("authorization")
    token = token[7:] if token and token.lower().startswith("bearer ") else token

    if not token:
        token = ws.query_params.get("token")

    if token_manager.stats()["active"] > 0 and not token_manager.is_valid(token):
        await ws.close(code=WS_CLOSE_UNAUTHORIZED)
        return

    await ws.accept()
    connections.add(ws)

    # Get client identifier for rate limiting
    client_id = ws.headers.get("x-client-id") or (ws.client.host if ws.client else "unknown")

    try:
        while True:
            message = await ws.receive_text()

            # Validate message size
            if len(message) > settings.max_message_size:
                logger.warning(f"Message too large from {client_id}: {len(message)} bytes")
                await ws.close(code=WS_CLOSE_MESSAGE_TOO_BIG)
                return

            if message == "ping":
                await ws.send_text("pong")
                continue

            # Rate limiting check
            rate_check = rate_limiter.check("websocket", client_id)
            if not rate_check["allowed"]:
                logger.warning(f"Rate limit exceeded for {client_id}")
                await ws.send_json({
                    "type": "error",
                    "error": "rate_limit_exceeded",
                    "retry_after": rate_check["retry_after"]
                })
                continue

            try:
                payload = json.loads(message)
            except json.JSONDecodeError:
                await ws.send_json({"type": "error", "error": "invalid_json"})
                continue

            response = _dispatch_action(payload)

            # Broadcast support: if payload.broadcast is True, send to others
            if isinstance(payload, dict) and payload.get("broadcast") is True:
                for client in list(connections):
                    if client is ws:
                        continue
                    try:
                        await client.send_json(response)
                    except Exception:
                        connections.discard(client)
            await ws.send_json(response)
    except WebSocketDisconnect:
        connections.discard(ws)
        return


# Alias for inclusion in main app
websocket_router = router


# Action handler mapping for cleaner dispatch
ACTION_HANDLERS = {
    "keyboard": lambda data: actions.handle_keyboard(data),
    "audio": lambda data: actions.handle_audio(
        data.get("action") if isinstance(data, dict) else "",
        data if isinstance(data, dict) else {},
    ),
    "obs": lambda data: actions.handle_obs(
        data.get("action") if isinstance(data, dict) else "",
        data if isinstance(data, dict) else {},
    ),
    "scripts": lambda data: actions.run_script(data),
    "system": lambda data: actions.handle_system(
        data.get("action") if isinstance(data, dict) else "",
        data if isinstance(data, dict) else {},
    ),
    "clipboard:copy": lambda data: actions.copy_text(data),
    "clipboard:paste": lambda data: actions.paste_text(),
    "screenshot": lambda data: actions.take_screenshot(),
    "processes": lambda data: actions.list_processes(),
}


def _dispatch_action(payload: Dict[str, Any]) -> Dict[str, Any]:
    """Dispatch incoming action to appropriate handler.

    Args:
        payload: WebSocket message payload

    Returns:
        Response dictionary with type, status, and result data
    """
    kind = payload.get("kind")
    action = payload.get("action")
    data = payload.get("payload")
    message_id = payload.get("messageId")

    # Handle profile selection
    if kind == "profile:select":
        return {
            "type": MESSAGE_TYPE_PROFILE_SELECT_ACK,
            "status": STATUS_OK,
            "profileId": payload.get("profileId"),
            "messageId": message_id,
        }

    # Handle control kind wrapper
    if kind == "control":
        action = action or payload.get("type")

    # Validate action exists
    if not action:
        return {
            "type": MESSAGE_TYPE_ACK,
            "status": STATUS_ERROR,
            "error": "missing action",
            "messageId": message_id,
        }

    # Dispatch to handler
    try:
        handler = ACTION_HANDLERS.get(action)
        if not handler:
            return {
                "type": MESSAGE_TYPE_ACK,
                "status": STATUS_ERROR,
                "error": "unknown action",
                "messageId": message_id,
            }

        result = handler(data)
        return {
            "type": MESSAGE_TYPE_ACK,
            "status": result.get("status", STATUS_OK),
            "messageId": message_id,
            **result,
        }
    except Exception as exc:  # noqa: BLE001
        logger.exception(f"Error dispatching action {action}")
        return {
            "type": MESSAGE_TYPE_ACK,
            "status": STATUS_ERROR,
            "error": str(exc),
            "messageId": message_id,
        }
