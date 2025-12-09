from __future__ import annotations

from pathlib import Path
from typing import Optional

from loguru import logger

from ..config import Settings, get_settings


def setup_logger(settings: Optional[Settings] = None) -> None:
    """Configure Loguru sinks once for the backend.

    - Console sink at configured level
    - Rotating file sink in logs_dir
    """

    settings = settings or get_settings()
    log_dir: Path = settings.logs_dir
    log_dir.mkdir(parents=True, exist_ok=True)

    logger.remove()
    logger.add(
        sink=lambda msg: print(msg, end=""),
        level=settings.log_level.upper(),
        colorize=False,
        enqueue=True,
        backtrace=False,
        diagnose=False,
    )

    log_file = log_dir / "control-deck.log"
    logger.add(
        log_file,
        level=settings.log_level.upper(),
        rotation="20 MB",
        retention="14 days",
        enqueue=True,
        backtrace=False,
        diagnose=False,
        format="{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | {message}",
    )
