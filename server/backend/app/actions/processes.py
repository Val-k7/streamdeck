from __future__ import annotations

import psutil


def list_processes(limit: int = 50) -> dict:
    procs = []
    for p in psutil.process_iter(attrs=["pid", "name"]):
        procs.append({"pid": p.info["pid"], "name": p.info.get("name")})
        if len(procs) >= limit:
            break
    return {"status": "ok", "processes": procs}
