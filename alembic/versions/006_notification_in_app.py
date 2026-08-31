"""In-app notification fields for shop owner lifecycle events.

Revision ID: 006_notification_in_app
Revises: 005_offer_verification
Create Date: 2026-08-26
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "006_notification_in_app"
down_revision: Union[str, None] = "005_offer_verification"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "notifications",
        sa.Column("notification_type", sa.String(length=80), nullable=False, server_default="GENERIC"),
    )
    op.add_column(
        "notifications",
        sa.Column("entity_type", sa.String(length=50), nullable=True),
    )
    op.add_column(
        "notifications",
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=True),
    )
    op.add_column(
        "notifications",
        sa.Column("is_read", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "notifications",
        sa.Column("read_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.add_column(
        "notifications",
        sa.Column("dedupe_key", sa.String(length=255), nullable=True),
    )
    op.create_index(
        op.f("ix_notifications_user_id_is_read"),
        "notifications",
        ["user_id", "is_read"],
        unique=False,
    )
    op.create_index(op.f("ix_notifications_dedupe_key"), "notifications", ["dedupe_key"], unique=True)


def downgrade() -> None:
    op.drop_index(op.f("ix_notifications_dedupe_key"), table_name="notifications")
    op.drop_index(op.f("ix_notifications_user_id_is_read"), table_name="notifications")
    op.drop_column("notifications", "dedupe_key")
    op.drop_column("notifications", "read_at")
    op.drop_column("notifications", "is_read")
    op.drop_column("notifications", "entity_id")
    op.drop_column("notifications", "entity_type")
    op.drop_column("notifications", "notification_type")
