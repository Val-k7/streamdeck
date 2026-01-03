"""Custom exceptions for Control Deck backend."""
from __future__ import annotations


class ControlDeckException(Exception):
    """Base exception for all Control Deck errors."""

    def __init__(self, message: str, code: str = "UNKNOWN_ERROR"):
        self.message = message
        self.code = code
        super().__init__(message)


class ValidationError(ControlDeckException):
    """Raised when input validation fails."""

    def __init__(self, message: str):
        super().__init__(message, code="VALIDATION_ERROR")


class AuthenticationError(ControlDeckException):
    """Raised when authentication fails."""

    def __init__(self, message: str):
        super().__init__(message, code="AUTHENTICATION_ERROR")


class RateLimitError(ControlDeckException):
    """Raised when rate limit is exceeded."""

    def __init__(self, message: str, retry_after: float = 0):
        super().__init__(message, code="RATE_LIMIT_EXCEEDED")
        self.retry_after = retry_after


class ActionNotFoundError(ControlDeckException):
    """Raised when requested action doesn't exist."""

    def __init__(self, action: str):
        super().__init__(f"Action not found: {action}", code="ACTION_NOT_FOUND")
        self.action = action


class ProfileNotFoundError(ControlDeckException):
    """Raised when requested profile doesn't exist."""

    def __init__(self, profile_id: str):
        super().__init__(f"Profile not found: {profile_id}", code="PROFILE_NOT_FOUND")
        self.profile_id = profile_id


class ScriptExecutionError(ControlDeckException):
    """Raised when script execution fails."""

    def __init__(self, message: str, exit_code: int | None = None):
        super().__init__(message, code="SCRIPT_EXECUTION_ERROR")
        self.exit_code = exit_code


class ScriptValidationError(ValidationError):
    """Raised when script path validation fails."""

    def __init__(self, message: str):
        super().__init__(message)
        self.code = "SCRIPT_VALIDATION_ERROR"


class MessageTooLargeError(ValidationError):
    """Raised when message exceeds size limit."""

    def __init__(self, size: int, limit: int):
        super().__init__(f"Message too large: {size} bytes (limit: {limit})")
        self.size = size
        self.limit = limit
        self.code = "MESSAGE_TOO_LARGE"
