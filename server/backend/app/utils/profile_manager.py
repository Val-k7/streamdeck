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
        # Simple alias mapping to keep backward compatibility with legacy profile IDs
        self.aliases = {
            "audio": "profile_default_mixer",
            "streaming": "profile_default_streaming",
            "macros": "profile_default_user",
        }

    def list_profiles(self) -> List[Dict]:
        profiles = []
        for file in self.settings.profiles_dir.glob("*.json"):
            try:
                data = json.loads(file.read_text(encoding="utf-8"))
                profiles.append({"id": data.get("id", file.stem), "name": data.get("name", file.stem)})
            except Exception:
                continue

        # Inject legacy aliases when target files exist (so UI tabs match expected labels)
        for alias, target in self.aliases.items():
            target_file = self.settings.profiles_dir / f"{target}.json"
            if not target_file.exists():
                continue
            if any(p["id"] == alias for p in profiles):
                continue
            try:
                data = json.loads(target_file.read_text(encoding="utf-8"))
                profiles.append({"id": alias, "name": data.get("name", alias)})
            except Exception:
                continue
        return profiles

    def get_profile(self, profile_id: str) -> Optional[Dict]:
        file = self.settings.profiles_dir / f"{profile_id}.json"
        if not file.exists():
            alias = self.aliases.get(profile_id)
            if alias:
                aliased_file = self.settings.profiles_dir / f"{alias}.json"
                if aliased_file.exists():
                    return json.loads(aliased_file.read_text(encoding="utf-8"))
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
