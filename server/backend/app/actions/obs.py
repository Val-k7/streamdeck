from __future__ import annotations

import base64
from typing import Any, Dict

import httpx
from loguru import logger

from app.config import get_settings

settings = get_settings()


class OBSClient:
    """Lightweight RPC client against OBS WebSocket HTTP endpoint."""

    def __init__(self, url: str, password: str = "", timeout: float = 10.0):
        self.base_url = self._normalize_url(url)
        self.password = password or ""
        self.timeout = timeout

    @staticmethod
    def _normalize_url(url: str) -> str:
        base = url or "ws://localhost:4455"
        return base.replace("ws://", "http://").replace("wss://", "https://")

    def _headers(self) -> Dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self.password:
            token = base64.b64encode(f":{self.password}".encode()).decode()
            headers["Authorization"] = f"Basic {token}"
        return headers

    def call(self, request_type: str, request_data: Dict[str, Any] | None = None) -> Dict[str, Any]:
        payload = {"requestType": request_type, "requestData": request_data or {}}

        try:
            response = httpx.post(
                f"{self.base_url}/api",
                json=payload,
                headers=self._headers(),
                timeout=self.timeout,
            )
            response.raise_for_status()
        except httpx.TimeoutException:
            raise RuntimeError("OBS WebSocket connection timeout. Is OBS Studio running?") from None
        except httpx.HTTPStatusError as exc:
            status = exc.response.status_code
            reason = exc.response.reason_phrase
            if status == 404:
                raise RuntimeError(
                    "OBS WebSocket server not found. Is OBS Studio running with WebSocket enabled?"
                ) from None
            if status == 401:
                raise RuntimeError("OBS WebSocket authentication failed. Check your password.") from None
            if status == 0 or status >= 500:
                raise RuntimeError(f"OBS WebSocket server error: {reason}. Is OBS Studio running?") from None
            raise RuntimeError(f"OBS request failed: {reason}") from None
        except httpx.RequestError as exc:
            message = str(exc)
            lowered = message.lower()
            if "timeout" in lowered or "aborted" in lowered:
                raise RuntimeError("OBS WebSocket connection timeout. Is OBS Studio running?") from None
            raise RuntimeError(
                "Cannot connect to OBS WebSocket server. Is OBS Studio running with WebSocket enabled on port 4455?"
            ) from None

        result = response.json()
        status_info = result.get("requestStatus", {})
        code = status_info.get("code")
        if code != 100:
            comment = status_info.get("comment") or "Unknown error"
            if code == 404:
                raise RuntimeError(f"OBS resource not found: {comment}") from None
            if code == 400:
                raise RuntimeError(f"OBS invalid request: {comment}") from None
            raise RuntimeError(f"OBS request error (code {code}): {comment}") from None

        return result.get("responseData", {})


def _require(payload: Dict[str, Any], *keys: str) -> None:
    missing = [key for key in keys if payload.get(key) is None]
    if missing:
        raise RuntimeError(f"Missing parameter(s) for OBS action: {', '.join(missing)}")


class OBSSourceManager:
    def __init__(self, obs_client: OBSClient):
        self.obs_client = obs_client

    def _get_source(self, scene_name: str, source_name: str) -> Dict[str, Any]:
        sources = self.list_sources(scene_name)
        for item in sources.get("sceneItems", []):
            if item.get("sourceName") == source_name:
                return item
        raise RuntimeError(f"Source not found: {source_name}")

    def list_sources(self, scene_name: str) -> Dict[str, Any]:
        _require({"sceneName": scene_name}, "sceneName")
        return self.obs_client.call("GetSceneItemList", {"sceneName": scene_name})

    def set_source_visibility(self, scene_name: str, source_name: str, visible: bool) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemEnabled",
            {"sceneName": scene_name, "sceneItemId": source["sceneItemId"], "sceneItemEnabled": bool(visible)},
        )

    def set_source_locked(self, scene_name: str, source_name: str, locked: bool) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemLocked",
            {"sceneName": scene_name, "sceneItemId": source["sceneItemId"], "sceneItemLocked": bool(locked)},
        )

    def set_source_index(self, scene_name: str, source_name: str, new_index: int) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemIndex",
            {"sceneName": scene_name, "sceneItemId": source["sceneItemId"], "sceneItemIndex": int(new_index)},
        )

    def set_source_position(self, scene_name: str, source_name: str, x: float, y: float) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemTransform",
            {
                "sceneName": scene_name,
                "sceneItemId": source["sceneItemId"],
                "sceneItemTransform": {"positionX": float(x), "positionY": float(y)},
            },
        )

    def set_source_size(self, scene_name: str, source_name: str, width: float, height: float) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemTransform",
            {
                "sceneName": scene_name,
                "sceneItemId": source["sceneItemId"],
                "sceneItemTransform": {"width": float(width), "height": float(height)},
            },
        )

    def set_source_rotation(self, scene_name: str, source_name: str, rotation: float) -> Dict[str, Any]:
        source = self._get_source(scene_name, source_name)
        return self.obs_client.call(
            "SetSceneItemTransform",
            {
                "sceneName": scene_name,
                "sceneItemId": source["sceneItemId"],
                "sceneItemTransform": {"rotation": float(rotation)},
            },
        )


class OBSFilterManager:
    def __init__(self, obs_client: OBSClient):
        self.obs_client = obs_client

    def list_filters(self, source_name: str, source_type: str = "OBS_SOURCE_TYPE_INPUT") -> Dict[str, Any]:
        _require({"sourceName": source_name}, "sourceName")
        return self.obs_client.call(
            "GetSourceFilterList",
            {"sourceName": source_name, "sourceType": source_type},
        )

    def add_filter(self, source_name: str, filter_name: str, filter_type: str, filter_settings: Dict[str, Any] | None = None) -> Dict[str, Any]:
        _require(
            {"sourceName": source_name, "filterName": filter_name, "filterType": filter_type},
            "sourceName",
            "filterName",
            "filterType",
        )
        return self.obs_client.call(
            "CreateSourceFilter",
            {
                "sourceName": source_name,
                "filterName": filter_name,
                "filterType": filter_type,
                "filterSettings": filter_settings or {},
            },
        )

    def remove_filter(self, source_name: str, filter_name: str) -> Dict[str, Any]:
        _require({"sourceName": source_name, "filterName": filter_name}, "sourceName", "filterName")
        return self.obs_client.call(
            "RemoveSourceFilter",
            {"sourceName": source_name, "filterName": filter_name},
        )

    def get_filter_settings(self, source_name: str, filter_name: str) -> Dict[str, Any]:
        _require({"sourceName": source_name, "filterName": filter_name}, "sourceName", "filterName")
        return self.obs_client.call("GetSourceFilter", {"sourceName": source_name, "filterName": filter_name})

    def set_filter_settings(self, source_name: str, filter_name: str, filter_settings: Dict[str, Any]) -> Dict[str, Any]:
        _require({"sourceName": source_name, "filterName": filter_name}, "sourceName", "filterName")
        return self.obs_client.call(
            "SetSourceFilterSettings",
            {"sourceName": source_name, "filterName": filter_name, "filterSettings": filter_settings or {}},
        )

    def set_filter_enabled(self, source_name: str, filter_name: str, enabled: bool) -> Dict[str, Any]:
        _require({"sourceName": source_name, "filterName": filter_name}, "sourceName", "filterName")
        return self.obs_client.call(
            "SetSourceFilterEnabled",
            {"sourceName": source_name, "filterName": filter_name, "filterEnabled": bool(enabled)},
        )


class OBSTransitionManager:
    def __init__(self, obs_client: OBSClient):
        self.obs_client = obs_client

    def list_transitions(self) -> Dict[str, Any]:
        return self.obs_client.call("GetTransitionList")

    def get_current_transition(self) -> Dict[str, Any]:
        return self.obs_client.call("GetCurrentSceneTransition")

    def set_transition(self, transition_name: str, transition_duration: int | None = None) -> Dict[str, Any]:
        _require({"transitionName": transition_name}, "transitionName")
        params: Dict[str, Any] = {"transitionName": transition_name}
        if transition_duration is not None:
            params["transitionDuration"] = int(transition_duration)
        return self.obs_client.call("SetCurrentSceneTransition", params)

    def trigger_transition(self, transition_name: str | None = None, transition_duration: int | None = None) -> Dict[str, Any]:
        if transition_name:
            self.set_transition(transition_name, transition_duration)
        return self.obs_client.call("TriggerStudioModeTransition")


class OBSStudioModeManager:
    def __init__(self, obs_client: OBSClient):
        self.obs_client = obs_client

    def set_studio_mode_enabled(self, enabled: bool) -> Dict[str, Any]:
        return self.obs_client.call("SetStudioModeEnabled", {"studioModeEnabled": bool(enabled)})

    def get_studio_mode_enabled(self) -> Dict[str, Any]:
        return self.obs_client.call("GetStudioModeEnabled")

    def get_preview_scene(self) -> Dict[str, Any]:
        return self.obs_client.call("GetPreviewScene")

    def set_preview_scene(self, scene_name: str) -> Dict[str, Any]:
        _require({"sceneName": scene_name}, "sceneName")
        return self.obs_client.call("SetCurrentPreviewScene", {"sceneName": scene_name})

    def get_program_scene(self) -> Dict[str, Any]:
        return self.obs_client.call("GetCurrentProgramScene")

    def trigger_transition(self, transition_name: str | None = None, transition_duration: int | None = None) -> Dict[str, Any]:
        transition_manager = OBSTransitionManager(self.obs_client)
        if transition_name:
            transition_manager.set_transition(transition_name, transition_duration)
        return self.obs_client.call("TriggerStudioModeTransition")


class OBSAdvancedManager:
    def __init__(self, obs_client: OBSClient):
        self.obs_client = obs_client
        self.sources = OBSSourceManager(obs_client)
        self.filters = OBSFilterManager(obs_client)
        self.transitions = OBSTransitionManager(obs_client)
        self.studio_mode = OBSStudioModeManager(obs_client)

    def handle_advanced_action(self, action: str, payload: Dict[str, Any]) -> Any:
        payload = payload or {}

        if action == "list_sources":
            _require(payload, "sceneName")
            return self.sources.list_sources(payload["sceneName"])
        if action == "get_source_info":
            _require(payload, "sceneName", "sourceName")
            return self.sources._get_source(payload["sceneName"], payload["sourceName"])
        if action == "set_source_visibility":
            _require(payload, "sceneName", "sourceName", "visible")
            return self.sources.set_source_visibility(payload["sceneName"], payload["sourceName"], payload["visible"])
        if action == "set_source_locked":
            _require(payload, "sceneName", "sourceName", "locked")
            return self.sources.set_source_locked(payload["sceneName"], payload["sourceName"], payload["locked"])
        if action == "set_source_index":
            _require(payload, "sceneName", "sourceName", "newIndex")
            return self.sources.set_source_index(payload["sceneName"], payload["sourceName"], payload["newIndex"])
        if action == "set_source_position":
            _require(payload, "sceneName", "sourceName", "x", "y")
            return self.sources.set_source_position(payload["sceneName"], payload["sourceName"], payload["x"], payload["y"])
        if action == "set_source_size":
            _require(payload, "sceneName", "sourceName", "width", "height")
            return self.sources.set_source_size(payload["sceneName"], payload["sourceName"], payload["width"], payload["height"])
        if action == "set_source_rotation":
            _require(payload, "sceneName", "sourceName", "rotation")
            return self.sources.set_source_rotation(payload["sceneName"], payload["sourceName"], payload["rotation"])

        if action == "list_filters":
            _require(payload, "sourceName")
            return self.filters.list_filters(payload["sourceName"], payload.get("sourceType", "OBS_SOURCE_TYPE_INPUT"))
        if action == "add_filter":
            _require(payload, "sourceName", "filterName", "filterType")
            return self.filters.add_filter(
                payload["sourceName"], payload["filterName"], payload["filterType"], payload.get("filterSettings", {}),
            )
        if action == "remove_filter":
            _require(payload, "sourceName", "filterName")
            return self.filters.remove_filter(payload["sourceName"], payload["filterName"])
        if action == "get_filter_settings":
            _require(payload, "sourceName", "filterName")
            return self.filters.get_filter_settings(payload["sourceName"], payload["filterName"])
        if action == "set_filter_settings":
            _require(payload, "sourceName", "filterName", "filterSettings")
            return self.filters.set_filter_settings(payload["sourceName"], payload["filterName"], payload.get("filterSettings", {}))
        if action == "set_filter_enabled":
            _require(payload, "sourceName", "filterName", "enabled")
            return self.filters.set_filter_enabled(payload["sourceName"], payload["filterName"], payload["enabled"])

        if action == "list_transitions":
            return self.transitions.list_transitions()
        if action == "get_current_transition":
            return self.transitions.get_current_transition()
        if action == "set_transition":
            _require(payload, "transitionName")
            return self.transitions.set_transition(payload["transitionName"], payload.get("transitionDuration"))
        if action == "trigger_transition":
            return self.transitions.trigger_transition(payload.get("transitionName"), payload.get("transitionDuration"))

        if action == "set_studio_mode":
            _require(payload, "enabled")
            return self.studio_mode.set_studio_mode_enabled(payload["enabled"])
        if action == "get_studio_mode":
            return self.studio_mode.get_studio_mode_enabled()
        if action == "get_preview_scene":
            return self.studio_mode.get_preview_scene()
        if action == "set_preview_scene":
            _require(payload, "sceneName")
            return self.studio_mode.set_preview_scene(payload["sceneName"])
        if action == "get_program_scene":
            return self.studio_mode.get_program_scene()
        if action == "trigger_studio_transition":
            return self.studio_mode.trigger_transition(payload.get("transitionName"), payload.get("transitionDuration"))

        raise RuntimeError(f"Unknown OBS action: {action}")


_client = OBSClient(settings.obs_ws_url, settings.obs_ws_password, settings.obs_request_timeout)
_advanced_manager = OBSAdvancedManager(_client)


def _parse_action_and_payload(action: str, payload: Dict[str, Any] | str | None):
    if isinstance(payload, str):
        return payload, {}
    payload = payload or {}
    parsed_action = action or payload.get("action") or payload.get("type") or ""
    params = payload.get("params") or payload.get("payload") or payload
    return parsed_action, params if isinstance(params, dict) else {}


def handle_obs(action: str, payload: Dict[str, Any] | None = None) -> dict:
    parsed_action, params = _parse_action_and_payload(action, payload)
    if not parsed_action:
        return {"status": "error", "error": "action required"}

    action_upper = parsed_action.upper()

    try:
        if action_upper in {"STARTSTREAMING", "START_STREAMING"}:
            _client.call("StartStream")
            return {"status": "ok", "action": "start_streaming"}

        if action_upper in {"STOPSTREAMING", "STOP_STREAMING"}:
            _client.call("StopStream")
            return {"status": "ok", "action": "stop_streaming"}

        if action_upper == "START_RECORDING":
            _client.call("StartRecord")
            return {"status": "ok", "action": "start_recording"}

        if action_upper == "STOP_RECORDING":
            _client.call("StopRecord")
            return {"status": "ok", "action": "stop_recording"}

        if action_upper == "TOGGLE_STREAMING":
            stream_status = _client.call("GetStreamStatus")
            if stream_status.get("outputActive"):
                _client.call("StopStream")
                return {"status": "ok", "action": "stop_streaming"}
            _client.call("StartStream")
            return {"status": "ok", "action": "start_streaming"}

        if action_upper == "TOGGLE_RECORDING":
            record_status = _client.call("GetRecordStatus")
            if record_status.get("outputActive"):
                _client.call("StopRecord")
                return {"status": "ok", "action": "stop_recording"}
            _client.call("StartRecord")
            return {"status": "ok", "action": "start_recording"}

        if action_upper in {"SET_SCENE", "CHANGE_SCENE"}:
            scene_name = params.get("sceneName")
            _require({"sceneName": scene_name}, "sceneName")
            _client.call("SetCurrentProgramScene", {"sceneName": scene_name})
            return {"status": "ok", "sceneName": scene_name}

        if action_upper in {"SET_VOLUME", "OBS_VOLUME"}:
            input_name = params.get("sourceName") or params.get("inputName")
            volume_db = params.get("volumeDb")
            if volume_db is None:
                volume_db = params.get("volume")
            _require({"inputName": input_name, "volumeDb": volume_db}, "inputName", "volumeDb")
            _client.call("SetInputVolume", {"inputName": input_name, "inputVolumeDb": float(volume_db)})
            return {"status": "ok", "inputName": input_name, "volumeDb": float(volume_db)}

        if action_upper == "MUTE":
            input_name = params.get("sourceName") or params.get("inputName")
            _require({"inputName": input_name}, "inputName")
            _client.call("ToggleInputMute", {"inputName": input_name})
            return {"status": "ok", "muted": True, "inputName": input_name}

        if action_upper == "UNMUTE":
            input_name = params.get("sourceName") or params.get("inputName")
            _require({"inputName": input_name}, "inputName")
            _client.call("SetInputMute", {"inputName": input_name, "inputMuted": False})
            return {"status": "ok", "muted": False, "inputName": input_name}

        # Advanced actions fallback (lowercase)
        result = _advanced_manager.handle_advanced_action(parsed_action.lower(), params)
        if result is None:
            return {"status": "ok", "action": parsed_action.lower()}
        return {"status": "ok", "action": parsed_action.lower(), "result": result}
    except Exception as exc:  # noqa: BLE001
        logger.error("OBS action failed", action=parsed_action, error=str(exc))
        return {"status": "error", "error": str(exc), "action": parsed_action}
