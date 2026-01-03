"""WebSocket compression middleware for Control Deck."""
from __future__ import annotations

import gzip
import zlib
from typing import Any, Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware


class CompressionMiddleware(BaseHTTPMiddleware):
    """Middleware to compress HTTP responses."""

    def __init__(self, app, minimum_size: int = 1024):
        super().__init__(app)
        self.minimum_size = minimum_size

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        response = await call_next(request)

        # Check if compression is supported
        accept_encoding = request.headers.get("Accept-Encoding", "")

        # Skip compression for small responses
        content_length = response.headers.get("Content-Length")
        if content_length and int(content_length) < self.minimum_size:
            return response

        # Skip if already compressed
        if response.headers.get("Content-Encoding"):
            return response

        # Get response body
        body = b""
        async for chunk in response.body_iterator:
            body += chunk

        # Skip if body is too small
        if len(body) < self.minimum_size:
            return Response(
                content=body,
                status_code=response.status_code,
                headers=dict(response.headers),
                media_type=response.media_type,
            )

        # Compress with gzip if supported
        if "gzip" in accept_encoding:
            compressed_body = gzip.compress(body, compresslevel=6)
            headers = dict(response.headers)
            headers["Content-Encoding"] = "gzip"
            headers["Content-Length"] = str(len(compressed_body))

            return Response(
                content=compressed_body,
                status_code=response.status_code,
                headers=headers,
                media_type=response.media_type,
            )

        # Compress with deflate if supported
        elif "deflate" in accept_encoding:
            compressed_body = zlib.compress(body)
            headers = dict(response.headers)
            headers["Content-Encoding"] = "deflate"
            headers["Content-Length"] = str(len(compressed_body))

            return Response(
                content=compressed_body,
                status_code=response.status_code,
                headers=headers,
                media_type=response.media_type,
            )

        # No compression
        return Response(
            content=body,
            status_code=response.status_code,
            headers=dict(response.headers),
            media_type=response.media_type,
        )


def enable_websocket_compression() -> dict[str, Any]:
    """Configuration for WebSocket permessage-deflate extension.

    Returns:
        Dictionary with WebSocket compression settings
    """
    return {
        "compression": "deflate",
        "compress_settings": {
            "level": 6,  # Compression level (1-9, 6 is default)
            "memLevel": 8,  # Memory level (1-9)
        },
    }
