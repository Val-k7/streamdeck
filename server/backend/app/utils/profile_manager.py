from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Optional

from pydantic import BaseModel, Field

from ..config import Settings, get_settings


class Control(BaseModel):
    id: str
    type: str
    row: int
    col: int
    label: Optional[str] = None
    colorHex: Optional[str] = None
    action: Optional[Dict] = None


class Profile(BaseModel):
    id: str
    name: str
    rows: int
    cols: int
    version: int = 1
    controls: List[Control] = Field(default_factory=list)


class ProfileManager:
    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.settings.profiles_dir.mkdir(parents=True, exist_ok=True)

    def list_profiles(self) -> List[Dict]:
        profiles = []
        for file in self.settings.profiles_dir.glob("*.json"):
            try:
                data = json.loads(file.read_text(encoding="utf-8"))
                profiles.append({"id": data.get("id", file.stem), "name": data.get("name", file.stem)})
            except Exception:
                continue
        return profiles

    def get_profile(self, profile_id: str) -> Optional[Dict]:
        file = self.settings.profiles_dir / f"{profile_id}.json"
        if not file.exists():
            return None
        return json.loads(file.read_text(encoding="utf-8"))

    def save_profile(self, profile_id: str, payload: Dict) -> Profile:
        profile = Profile(**payload)
        if profile.id != profile_id:
            raise ValueError("id mismatch")
        file = self.settings.profiles_dir / f"{profile_id}.json"
        file.write_text(profile.model_dump_json(indent=2), encoding="utf-8")
        return profile

    def delete_profile(self, profile_id: str) -> bool:
        file = self.settings.profiles_dir / f"{profile_id}.json"
        if not file.exists():
            return False
        try:
            file.unlink()
        except Exception:
            return False
        return True
