from __future__ import annotations

import importlib
import sys
from pathlib import Path
from typing import Any, Dict, Optional

from .base import BasePlugin


class PluginManager:
    """Simple plugin loader for built-in Python plugins.

    - Autoloads modules from app.plugins.* (excluding base/manager)
    - enable/disable toggles in-memory instances
    - list_plugins returns both loaded status and availability
    """

    def __init__(self, plugins_dir: Optional[Path] = None):
        self.plugins_dir = plugins_dir or Path(__file__).parent
        self._available_modules = self._discover_modules()
        self._plugins: Dict[str, BasePlugin] = {}

    def _discover_modules(self) -> Dict[str, str]:
        modules: Dict[str, str] = {}
        if self.plugins_dir.exists():
            sys.path.append(str(self.plugins_dir))
            for module_path in self.plugins_dir.glob("*.py"):
                if module_path.name in {"__init__.py", "base.py", "manager.py"}:
                    continue
                modules[module_path.stem] = f"app.plugins.{module_path.stem}"
        return modules

    def load_all(self) -> None:
        for name in list(self._available_modules.keys()):
            self.enable(name)

    def list_plugins(self) -> Dict[str, Dict[str, Any]]:
        all_names = set(self._available_modules.keys()) | set(self._plugins.keys())
        return {name: {"loaded": name in self._plugins} for name in sorted(all_names)}

    def enable(self, name: str) -> bool:
        if name in self._plugins:
            return True

        module_path = self._available_modules.get(name)
        if not module_path:
            return False

        try:
            module = importlib.import_module(module_path)
            plugin_cls = next(
                (obj for obj in module.__dict__.values() if isinstance(obj, type) and issubclass(obj, BasePlugin) and obj is not BasePlugin),
                None,
            )
            if not plugin_cls:
                return False

            plugin: BasePlugin = plugin_cls()
            plugin.load()
            self._plugins[plugin.name] = plugin
            return True
        except Exception:
            return False

    def disable(self, name: str) -> bool:
        plugin = self._plugins.pop(name, None)
        if plugin:
            try:
                plugin.unload()
            except Exception:
                pass
            return True
        return False

    def execute(self, name: str, action: str, payload: Dict[str, Any] | None = None) -> Any:
        plugin = self._plugins.get(name)
        if not plugin:
            raise ValueError("plugin not found")
        return plugin.execute(action, payload or {})
