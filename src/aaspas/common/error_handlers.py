import json
from typing import Any

import structlog
from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from aaspas.common.exceptions import AppError, ErrorCode
from aaspas.common.responses import ErrorDetail, ErrorResponse

logger = structlog.get_logger(__name__)


def _serialize_validation_errors(errors: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return json.loads(json.dumps(errors, default=str))


async def app_error_handler(_request: Request, exc: AppError) -> JSONResponse:
    body = ErrorResponse(
        error=ErrorDetail(code=exc.code.value, message=exc.message, details=exc.details)
    )
    return JSONResponse(status_code=exc.status_code, content=body.model_dump(mode="json"))


async def validation_error_handler(
    _request: Request, exc: RequestValidationError
) -> JSONResponse:
    body = ErrorResponse(
        error=ErrorDetail(
            code=ErrorCode.VALIDATION_ERROR.value,
            message="Request validation failed",
            details={"errors": _serialize_validation_errors(exc.errors())},
        )
    )
    return JSONResponse(status_code=422, content=body.model_dump(mode="json"))


async def http_exception_handler(_request: Request, exc: StarletteHTTPException) -> JSONResponse:
    code = ErrorCode.NOT_FOUND if exc.status_code == 404 else ErrorCode.INTERNAL_ERROR
    body = ErrorResponse(
        error=ErrorDetail(
            code=code.value,
            message=str(exc.detail),
            details={},
        )
    )
    return JSONResponse(status_code=exc.status_code, content=body.model_dump(mode="json"))


async def unhandled_exception_handler(_request: Request, exc: Exception) -> JSONResponse:
    logger.exception("unhandled_exception", error=str(exc))
    body = ErrorResponse(
        error=ErrorDetail(
            code=ErrorCode.INTERNAL_ERROR.value,
            message="An unexpected error occurred",
        )
    )
    return JSONResponse(status_code=500, content=body.model_dump(mode="json"))


def register_exception_handlers(app: Any) -> None:
    app.add_exception_handler(AppError, app_error_handler)
    app.add_exception_handler(RequestValidationError, validation_error_handler)
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(Exception, unhandled_exception_handler)
