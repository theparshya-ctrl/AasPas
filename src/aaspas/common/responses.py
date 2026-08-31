from datetime import UTC, datetime
from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ErrorDetail(BaseModel):
    code: str
    message: str
    details: dict[str, Any] = Field(default_factory=dict)


class ApiResponse(BaseModel, Generic[T]):
    """Standard API envelope for all successful responses."""

    success: bool = True
    data: T | None = None
    meta: dict[str, Any] = Field(default_factory=dict)
    timestamp: datetime = Field(default_factory=lambda: datetime.now(UTC))


class ErrorResponse(BaseModel):
    """Standard API envelope for errors."""

    success: bool = False
    error: ErrorDetail
    timestamp: datetime = Field(default_factory=lambda: datetime.now(UTC))


class PaginatedMeta(BaseModel):
    page: int
    page_size: int
    total: int
    total_pages: int


class PaginatedResponse(BaseModel, Generic[T]):
    success: bool = True
    data: list[T]
    meta: PaginatedMeta
    timestamp: datetime = Field(default_factory=lambda: datetime.now(UTC))
