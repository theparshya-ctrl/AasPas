from enum import StrEnum


class ErrorCode(StrEnum):
    VALIDATION_ERROR = "VALIDATION_ERROR"
    NOT_FOUND = "NOT_FOUND"
    UNAUTHORIZED = "UNAUTHORIZED"
    FORBIDDEN = "FORBIDDEN"
    CONFLICT = "CONFLICT"
    RATE_LIMITED = "RATE_LIMITED"
    INTERNAL_ERROR = "INTERNAL_ERROR"
    SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"


class AppError(Exception):
    """Base application exception with HTTP mapping."""

    def __init__(
        self,
        message: str,
        *,
        code: ErrorCode = ErrorCode.INTERNAL_ERROR,
        status_code: int = 500,
        details: dict | None = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.code = code
        self.status_code = status_code
        self.details = details or {}


class NotFoundError(AppError):
    def __init__(self, message: str = "Resource not found", details: dict | None = None) -> None:
        super().__init__(
            message,
            code=ErrorCode.NOT_FOUND,
            status_code=404,
            details=details,
        )


class UnauthorizedError(AppError):
    def __init__(self, message: str = "Authentication required") -> None:
        super().__init__(message, code=ErrorCode.UNAUTHORIZED, status_code=401)


class ForbiddenError(AppError):
    def __init__(self, message: str = "Permission denied") -> None:
        super().__init__(message, code=ErrorCode.FORBIDDEN, status_code=403)


class ConflictError(AppError):
    def __init__(self, message: str = "Resource conflict", details: dict | None = None) -> None:
        super().__init__(message, code=ErrorCode.CONFLICT, status_code=409, details=details)


class ValidationAppError(AppError):
    def __init__(self, message: str, details: dict | None = None) -> None:
        super().__init__(
            message,
            code=ErrorCode.VALIDATION_ERROR,
            status_code=422,
            details=details,
        )
