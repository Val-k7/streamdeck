from __future__ import annotations

import subprocess
from typing import Sequence


def run_script(cmd: Sequence[str]) -> dict:
    try:
        completed = subprocess.run(cmd, check=True, capture_output=True, text=True)
        return {"status": "ok", "stdout": completed.stdout, "stderr": completed.stderr}
    except subprocess.CalledProcessError as exc:
        return {"status": "error", "code": exc.returncode, "stdout": exc.stdout, "stderr": exc.stderr}
