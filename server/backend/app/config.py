from __future__ import annotations

import secrets
from pathlib import Path
from typing import Optional

from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Runtime configuration for the Control Deck backend."""

    host: str = "0.0.0.0"
    port: int = 4455
    obs_ws_url: str = "ws://localhost:4455"
    obs_ws_password: str = ""
    obs_request_timeout: float = 10.0
    deck_token: str = Field(default_factory=lambda: secrets.token_hex(32))
    handshake_secret: Optional[str] = None
    deck_data_dir: Path = Field(
        default_factory=lambda: Path(__file__).resolve().parents[2]
    )
    log_level: str = "info"
    tls_key_path: Optional[Path] = None
    tls_cert_path: Optional[Path] = None

    class Config:
        env_prefix = "DECK_"
        case_sensitive = False

    @property
    def config_dir(self) -> Path:
        return self.deck_data_dir / "config"

    @property
    def profiles_dir(self) -> Path:
        return self.deck_data_dir / "profiles"

    @property
    def logs_dir(self) -> Path:
        return self.deck_data_dir / "logs"

    @property
    def plugins_dir(self) -> Path:
        return self.deck_data_dir / "plugins"

    @property
    def static_dir(self) -> Path:
        # Static files are produced by `server/frontend` into ../static
        return Path(__file__).resolve().parents[2] / "static"

    def ensure_runtime_dirs(self) -> None:
        for path in [
            self.config_dir,
            self.profiles_dir,
            self.logs_dir,
            self.plugins_dir,
        ]:
            path.mkdir(parents=True, exist_ok=True)

    @property
    def effective_handshake_secret(self) -> str:
        return self.handshake_secret or self.deck_token


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    settings = Settings()
    settings.ensure_runtime_dirs()
    return settings


def reset_settings_cache() -> None:
    """Clear cached settings (useful for tests)."""
    get_settings.cache_clear()
