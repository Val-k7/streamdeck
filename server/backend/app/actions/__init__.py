"""Action handlers (keyboard, audio, obs, scripts, system, clipboard, screenshot, processes)."""

from .audio import handle_audio
from .clipboard import copy_text, paste_text
from .keyboard import handle_keyboard
from .obs import handle_obs
from .processes import list_processes
from .screenshot import take_screenshot
from .scripts import run_script
from .system import handle_system

__all__ = [
    "handle_audio",
    "copy_text",
    "paste_text",
    "handle_keyboard",
    "handle_obs",
    "list_processes",
    "take_screenshot",
    "run_script",
    "handle_system",
]
