from __future__ import annotations

import json
from typing import Any, Dict, Set

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from . import actions
from .config import get_settings
from .utils.token_manager import get_token_manager

router = APIRouter()
settings = get_settings()
token_manager = get_token_manager(default_token=settings.deck_token)
connections: Set[WebSocket] = set()


@router.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    token = ws.headers.get("authorization")
    token = token[7:] if token and token.lower().startswith("bearer ") else token

    if not token:
        token = ws.query_params.get("token")

    if token_manager.stats()["active"] > 0 and not token_manager.is_valid(token):
        await ws.close(code=4001)
        return

    await ws.accept()
    connections.add(ws)
    try:
        while True:
            message = await ws.receive_text()

            if message == "ping":
                await ws.send_text("pong")
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


def _dispatch_action(payload: Dict[str, Any]) -> Dict[str, Any]:
    action = payload.get("action")
    data = payload.get("payload")
    message_id = payload.get("messageId")

    if not action:
        return {"type": "ack", "status": "error", "error": "missing action", "messageId": message_id}

    try:
        if action == "keyboard":
            result = actions.handle_keyboard(data)
        elif action == "audio":
            result = actions.handle_audio(data.get("action") if isinstance(data, dict) else "", data if isinstance(data, dict) else {})
        elif action == "obs":
            result = actions.handle_obs(data.get("action") if isinstance(data, dict) else "", data if isinstance(data, dict) else {})
        elif action == "scripts":
            result = actions.run_script(data)
        elif action == "system":
            result = actions.handle_system(data.get("action") if isinstance(data, dict) else "", data if isinstance(data, dict) else {})
        elif action == "clipboard:copy":
            result = actions.copy_text(data)
        elif action == "clipboard:paste":
            result = actions.paste_text()
        elif action == "screenshot":
            result = actions.take_screenshot()
        elif action == "processes":
            result = actions.list_processes()
        else:
            return {"type": "ack", "status": "error", "error": "unknown action", "messageId": message_id}

        return {"type": "ack", "status": result.get("status", "ok"), "messageId": message_id, **result}
    except Exception as exc:  # noqa: BLE001
        return {"type": "ack", "status": "error", "error": str(exc), "messageId": message_id}
