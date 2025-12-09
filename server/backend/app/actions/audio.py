from __future__ import annotations

import os
from typing import Any, Dict

try:
    from ctypes import POINTER, cast  # type: ignore
    from comtypes import CLSCTX_ALL  # type: ignore
    from pycaw.pycaw import AudioUtilities, IAudioEndpointVolume  # type: ignore

    _PYCAW_AVAILABLE = True
except Exception:  # pragma: no cover - import guard
    _PYCAW_AVAILABLE = False


def _get_volume_interface():
    devices = AudioUtilities.GetSpeakers()
    interface = devices.Activate(IAudioEndpointVolume._iid_, CLSCTX_ALL, None)
    return cast(IAudioEndpointVolume, interface)


def _set_master_volume(percent: float) -> None:
    percent = max(0.0, min(100.0, percent))
    volume = _get_volume_interface()
    volume.SetMasterVolumeLevelScalar(percent / 100.0, None)


def _toggle_mute(mute: bool) -> None:
    volume = _get_volume_interface()
    volume.SetMute(bool(mute), None)


def handle_audio(action: str, payload: Dict[str, Any] | None = None) -> dict:
    if os.name != "nt" or not _PYCAW_AVAILABLE:
        return {"status": "not_implemented", "reason": "pycaw/windows required"}

    payload = payload or {}
    action_upper = (action or payload.get("action") or "").upper()

    if action_upper in {"SET_VOLUME", "SET_DEVICE_VOLUME", "SET_APPLICATION_VOLUME"}:
        vol = payload.get("volume")
        if vol is None:
            return {"status": "error", "error": "volume required"}
        _set_master_volume(float(vol))
        return {"status": "ok", "volume": float(vol)}

    if action_upper in {"MUTE", "UNMUTE"}:
        _toggle_mute(action_upper == "MUTE")
        return {"status": "ok", "muted": action_upper == "MUTE"}

    return {"status": "not_implemented", "action": action_upper}
