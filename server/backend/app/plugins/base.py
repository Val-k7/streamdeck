from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict


class BasePlugin(ABC):
    name: str = "base"

    @abstractmethod
    def load(self) -> None:
        ...

    @abstractmethod
    def unload(self) -> None:
        ...

    @abstractmethod
    def execute(self, action: str, payload: Dict[str, Any] | None = None) -> Any:
        ...
