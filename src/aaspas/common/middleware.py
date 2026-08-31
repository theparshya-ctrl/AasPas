import time
from collections import defaultdict, deque
from collections.abc import Callable

import structlog
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

from aaspas.common.exceptions import AppError, ErrorCode
from aaspas.config import get_settings

logger = structlog.get_logger(__name__)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        start = time.perf_counter()
        response = await call_next(request)
        duration_ms = round((time.perf_counter() - start) * 1000, 2)
        logger.info(
            "http_request",
            method=request.method,
            path=request.url.path,
            status=response.status_code,
            duration_ms=duration_ms,
        )
        return response


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Simple in-memory rate limiter suitable for single-instance deployments."""

    def __init__(self, app: Callable, requests_per_minute: int) -> None:
        super().__init__(app)
        self.requests_per_minute = requests_per_minute
        self._hits: dict[str, deque[float]] = defaultdict(deque)

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        if request.url.path in {"/health", "/docs", "/openapi.json", "/redoc"}:
            return await call_next(request)

        client = request.client.host if request.client else "unknown"
        now = time.time()
        window_start = now - 60
        hits = self._hits[client]
        while hits and hits[0] < window_start:
            hits.popleft()

        if len(hits) >= self.requests_per_minute:
            raise AppError(
                "Rate limit exceeded",
                code=ErrorCode.RATE_LIMITED,
                status_code=429,
            )

        hits.append(now)
        return await call_next(request)


def register_middleware(app: Callable) -> None:
    settings = get_settings()
    app.add_middleware(RateLimitMiddleware, requests_per_minute=settings.rate_limit_per_minute)
    app.add_middleware(RequestLoggingMiddleware)
