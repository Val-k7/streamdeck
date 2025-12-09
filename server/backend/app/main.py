from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from .config import get_settings
from .routes import discovery, health, plugins, profiles, tokens
from .utils.logger import setup_logger
from .websocket import websocket_router

settings = get_settings()
setup_logger(settings)
app = FastAPI(title="Control Deck", version="0.0.1")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"]
    ,
    allow_headers=["*"],
)

# Routers REST
app.include_router(health.router, prefix="/health", tags=["health"])
app.include_router(discovery.router, prefix="/discovery", tags=["discovery"])
app.include_router(tokens.router, prefix="/tokens", tags=["tokens"])
app.include_router(profiles.router, prefix="/profiles", tags=["profiles"])
app.include_router(plugins.router, prefix="/plugins", tags=["plugins"])

# WebSocket
app.include_router(websocket_router, tags=["websocket"])

# Servir les fichiers statiques buildés par le frontend (si présents)
static_dir: Path = settings.static_dir
if static_dir.exists():
    app.mount(
        "/",
        StaticFiles(directory=static_dir, html=True),
        name="static",
    )
