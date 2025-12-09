from __future__ import annotations

import logging
import os
from typing import Optional

from zeroconf import IPVersion, ServiceInfo, Zeroconf

from ..config import Settings, get_settings

logger = logging.getLogger(__name__)


class DiscoveryService:
    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.zeroconf: Optional[Zeroconf] = None
        self.info: Optional[ServiceInfo] = None

    def start(self) -> None:
        if os.environ.get("DECK_DISABLE_DISCOVERY", "0") in {"1", "true", "True"}:
            logger.info("Discovery disabled via DECK_DISABLE_DISCOVERY")
            return
        if self.zeroconf:
            return
        name = f"{self.settings.host}:{self.settings.port}"
        desc = {
            "serverId": "python-backend",
            "protocol": "wss" if (self.settings.tls_key_path and self.settings.tls_cert_path) else "ws",
        }
        service_type = "_controldeck._tcp.local."
        self.info = ServiceInfo(
            type_=service_type,
            name=f"ControlDeck.{service_type}",
            port=self.settings.port,
            weight=0,
            priority=0,
            properties=desc,
            addresses=None,
        )
        self.zeroconf = Zeroconf(ip_version=IPVersion.All)
        try:
            self.zeroconf.register_service(self.info)
        except Exception as exc:  # pragma: no cover - defensive
            logger.warning("Failed to start discovery", exc_info=exc)
            self.stop()

    def stop(self) -> None:
        if self.zeroconf and self.info:
            try:
                self.zeroconf.unregister_service(self.info)
            except Exception:
                pass
        if self.zeroconf:
            try:
                self.zeroconf.close()
            except Exception:
                pass
        self.zeroconf = None
        self.info = None
