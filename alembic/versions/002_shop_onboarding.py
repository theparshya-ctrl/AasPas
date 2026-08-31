"""Shop onboarding fields and status workflow.

Revision ID: 002_shop_onboarding
Revises: 001_initial
Create Date: 2026-08-16

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "002_shop_onboarding"
down_revision: Union[str, None] = "001_initial"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("shops", sa.Column("contact_number", sa.String(length=20), nullable=True))
    op.add_column(
        "shops",
        sa.Column("business_hours", postgresql.JSONB(astext_type=sa.Text()), nullable=True),
    )
    op.add_column("shops", sa.Column("photo_url", sa.String(length=512), nullable=True))
    op.add_column("shops", sa.Column("photo_storage_key", sa.String(length=512), nullable=True))
    op.add_column("shops", sa.Column("rejection_reason", sa.Text(), nullable=True))
    op.add_column("shops", sa.Column("submitted_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("shops", sa.Column("approved_at", sa.DateTime(timezone=True), nullable=True))

    # Migrate legacy status values without data loss
    op.execute("UPDATE shops SET status = 'pending_approval' WHERE status = 'pending'")
    op.execute("UPDATE shops SET status = 'draft' WHERE status NOT IN ('active', 'pending_approval', 'rejected')")

    op.create_index("ix_shops_status_submitted_at", "shops", ["status", "submitted_at"], unique=False)
    op.create_index("ix_locations_lat_lng", "locations", ["latitude", "longitude"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_locations_lat_lng", table_name="locations")
    op.drop_index("ix_shops_status_submitted_at", table_name="shops")

    op.execute("UPDATE shops SET status = 'pending' WHERE status = 'pending_approval'")

    op.drop_column("shops", "approved_at")
    op.drop_column("shops", "submitted_at")
    op.drop_column("shops", "rejection_reason")
    op.drop_column("shops", "photo_storage_key")
    op.drop_column("shops", "photo_url")
    op.drop_column("shops", "business_hours")
    op.drop_column("shops", "contact_number")
