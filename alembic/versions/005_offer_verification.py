"""Offer verification lifecycle columns and rejected status support.

Revision ID: 005_offer_verification
Revises: 004_favorites
Create Date: 2026-08-20
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "005_offer_verification"
down_revision: Union[str, None] = "004_favorites"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("offers", sa.Column("photo_url", sa.String(length=512), nullable=True))
    op.add_column("offers", sa.Column("applicable_products", sa.Text(), nullable=True))
    op.add_column("offers", sa.Column("min_purchase_amount", sa.Numeric(10, 2), nullable=True))
    op.add_column("offers", sa.Column("terms", sa.Text(), nullable=True))
    op.add_column("offers", sa.Column("merchant_confirmed_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column(
        "offers",
        sa.Column("merchant_confirmed_by", postgresql.UUID(as_uuid=True), nullable=True),
    )
    op.add_column("offers", sa.Column("submitted_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("offers", sa.Column("approved_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("offers", sa.Column("rejected_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("offers", sa.Column("rejection_reason", sa.Text(), nullable=True))
    op.add_column(
        "offers",
        sa.Column("is_verified", sa.Boolean(), nullable=False, server_default=sa.false()),
    )


def downgrade() -> None:
    op.drop_column("offers", "is_verified")
    op.drop_column("offers", "rejection_reason")
    op.drop_column("offers", "rejected_at")
    op.drop_column("offers", "approved_at")
    op.drop_column("offers", "submitted_at")
    op.drop_column("offers", "merchant_confirmed_by")
    op.drop_column("offers", "merchant_confirmed_at")
    op.drop_column("offers", "terms")
    op.drop_column("offers", "min_purchase_amount")
    op.drop_column("offers", "applicable_products")
    op.drop_column("offers", "photo_url")
