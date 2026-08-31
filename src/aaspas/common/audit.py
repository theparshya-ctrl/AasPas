import uuid
from datetime import UTC, datetime
from enum import StrEnum
from typing import Any

from sqlalchemy import DateTime, String, Text
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.types import JSON

from aaspas.database import Base


class AuditAction(StrEnum):
    CREATE = "create"
    UPDATE = "update"
    DELETE = "delete"
    LOGIN = "login"
    LOGOUT = "logout"
    ACCESS = "access"
    ADMIN = "admin"


class AuditLog(Base):
    """Immutable audit trail for important actions across all modules."""

    __tablename__ = "audit_logs"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    actor_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True, index=True)
    actor_role: Mapped[str | None] = mapped_column(String(50), nullable=True)
    module: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    action: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    resource_type: Mapped[str] = mapped_column(String(100), nullable=False)
    resource_id: Mapped[str | None] = mapped_column(String(100), nullable=True, index=True)
    metadata_json: Mapped[dict[str, Any] | None] = mapped_column(
        "metadata", JSON().with_variant(JSONB, "postgresql"), nullable=True
    )
    ip_address: Mapped[str | None] = mapped_column(String(45), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(UTC), nullable=False, index=True
    )
    message: Mapped[str | None] = mapped_column(Text, nullable=True)


def record_audit(
    db,
    *,
    module: str,
    action: AuditAction | str,
    resource_type: str,
    resource_id: str | None = None,
    actor_id: uuid.UUID | None = None,
    actor_role: str | None = None,
    metadata: dict[str, Any] | None = None,
    ip_address: str | None = None,
    message: str | None = None,
) -> AuditLog:
    entry = AuditLog(
        actor_id=actor_id,
        actor_role=actor_role,
        module=module,
        action=str(action),
        resource_type=resource_type,
        resource_id=resource_id,
        metadata_json=metadata,
        ip_address=ip_address,
        message=message,
    )
    db.add(entry)
    return entry
