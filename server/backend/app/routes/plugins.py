from fastapi import APIRouter, HTTPException

from ..plugins.manager import PluginManager

router = APIRouter()
manager = PluginManager()
manager.load_all()


@router.get("/")
async def list_plugins():
    return {"plugins": manager.list_plugins()}


@router.post("/{name}/enable")
async def enable_plugin(name: str):
    ok = manager.enable(name)
    if not ok:
        raise HTTPException(status_code=404, detail="plugin not found")
    return {"status": "enabled", "name": name}


@router.post("/{name}/disable")
async def disable_plugin(name: str):
    ok = manager.disable(name)
    if not ok:
        raise HTTPException(status_code=404, detail="plugin not found")
    return {"status": "disabled", "name": name}


@router.get("/{name}/config")
async def get_plugin_config(name: str):
    return {"status": "not_implemented", "name": name}


@router.post("/{name}/config")
async def update_plugin_config(name: str, payload: dict):
    return {"status": "updated", "name": name, "config": payload}
