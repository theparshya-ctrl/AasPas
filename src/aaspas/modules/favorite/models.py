import uuid
from datetime import UTC, datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from aaspas.database import Base


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (
        CheckConstraint(
            "(offer_id IS NOT NULL AND shop_id IS NULL) OR (offer_id IS NULL AND shop_id IS NOT NULL)",
            name="ck_favorites_single_target",
        ),
        UniqueConstraint("user_id", "offer_id", name="uq_favorites_user_offer"),
        UniqueConstraint("user_id", "shop_id", name="uq_favorites_user_shop"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    offer_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("offers.id", ondelete="CASCADE"), nullable=True, index=True
    )
    shop_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("shops.id", ondelete="CASCADE"), nullable=True, index=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(UTC), nullable=False
    )
