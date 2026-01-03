"""Application-wide constants for Control Deck backend."""
from __future__ import annotations

# WebSocket Configuration
WEBSOCKET_MESSAGE_TIMEOUT_MS = 5000  # 5 seconds
WEBSOCKET_HEARTBEAT_INTERVAL_MS = 15000  # 15 seconds
WEBSOCKET_MAX_RECONNECT_ATTEMPTS = 6
WEBSOCKET_RECONNECT_BASE_DELAY_MS = 1000  # Exponential backoff base

# WebSocket Close Codes
WS_CLOSE_UNAUTHORIZED = 4001
WS_CLOSE_MESSAGE_TOO_BIG = 1009
WS_CLOSE_RATE_LIMITED = 4029

# Security Constraints
DEFAULT_MESSAGE_SIZE_LIMIT = 102400  # 100KB
DEFAULT_RATE_LIMIT_REQUESTS = 100
DEFAULT_RATE_LIMIT_WINDOW_SECONDS = 60

# Script Execution
SCRIPT_EXECUTION_TIMEOUT_SECONDS = 30

# Token Management
DEFAULT_TOKEN_EXPIRY_HOURS = 24

# Profile Management
PROFILE_VERSION = 1

# HTTP Response Codes (for reference)
HTTP_OK = 200
HTTP_BAD_REQUEST = 400
HTTP_UNAUTHORIZED = 401
HTTP_NOT_FOUND = 404
HTTP_TOO_MANY_REQUESTS = 429
HTTP_INTERNAL_ERROR = 500

# Action Types (for validation)
ACTION_TYPES = {
    "keyboard",
    "audio",
    "obs",
    "scripts",
    "system",
    "clipboard:copy",
    "clipboard:paste",
    "screenshot",
    "processes",
}

# Audio Action Types
AUDIO_SET_VOLUME = "SET_VOLUME"
AUDIO_SET_DEVICE_VOLUME = "SET_DEVICE_VOLUME"
AUDIO_SET_APPLICATION_VOLUME = "SET_APPLICATION_VOLUME"
AUDIO_MUTE = "MUTE"
AUDIO_UNMUTE = "UNMUTE"

# System Action Types
SYSTEM_LOCK = "lock"
SYSTEM_SHUTDOWN = "shutdown"
SYSTEM_RESTART = "restart"

# Message Types
MESSAGE_TYPE_ACK = "ack"
MESSAGE_TYPE_ERROR = "error"
MESSAGE_TYPE_PROFILE_SELECT = "profile:select"
MESSAGE_TYPE_PROFILE_SELECT_ACK = "profile:select:ack"
MESSAGE_TYPE_CONTROL_STATE = "control:state"

# Status Values
STATUS_OK = "ok"
STATUS_ERROR = "error"
STATUS_NOT_IMPLEMENTED = "not_implemented"
