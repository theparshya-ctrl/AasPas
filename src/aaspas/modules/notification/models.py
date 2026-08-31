import uuid

from datetime import UTC, datetime



from sqlalchemy import Boolean, DateTime, ForeignKey, String, Text, UniqueConstraint

from sqlalchemy.dialects.postgresql import UUID

from sqlalchemy.orm import Mapped, mapped_column



from aaspas.database import Base





class Notification(Base):

    __tablename__ = "notifications"

    __table_args__ = (UniqueConstraint("dedupe_key", name="uq_notifications_dedupe_key"),)



    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    user_id: Mapped[uuid.UUID] = mapped_column(

        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True

    )

    channel: Mapped[str] = mapped_column(String(50), default="in_app", nullable=False)

    notification_type: Mapped[str] = mapped_column(String(80), default="GENERIC", nullable=False)

    title: Mapped[str] = mapped_column(String(255), nullable=False)

    body: Mapped[str] = mapped_column(Text, nullable=False)

    entity_type: Mapped[str | None] = mapped_column(String(50), nullable=True)

    entity_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)

    is_read: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False, index=True)

    read_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    dedupe_key: Mapped[str | None] = mapped_column(String(255), nullable=True, index=True)

    status: Mapped[str] = mapped_column(String(50), default="pending", nullable=False, index=True)

    sent_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    created_at: Mapped[datetime] = mapped_column(

        DateTime(timezone=True), default=lambda: datetime.now(UTC), nullable=False

    )


