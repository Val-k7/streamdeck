from __future__ import annotations

try:
    import pyperclip
except ImportError:  # pyperclip not yet in deps
    pyperclip = None


def copy_text(text: str) -> dict:
    if not pyperclip:
        return {"status": "not_implemented", "reason": "pyperclip missing"}
    pyperclip.copy(text)
    return {"status": "ok"}


def paste_text() -> dict:
    if not pyperclip:
        return {"status": "not_implemented", "reason": "pyperclip missing"}
    return {"status": "ok", "text": pyperclip.paste()}
