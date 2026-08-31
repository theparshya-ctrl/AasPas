"""Customer favorites — saved offers and shops.

Revision ID: 004_favorites
Revises: 003_customer_home
Create Date: 2026-08-20
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "004_favorites"
down_revision: Union[str, None] = "003_customer_home"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "favorites",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("offer_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("shop_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["offer_id"], ["offers.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["shop_id"], ["shops.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.CheckConstraint(
            "(offer_id IS NOT NULL AND shop_id IS NULL) OR (offer_id IS NULL AND shop_id IS NOT NULL)",
            name="ck_favorites_single_target",
        ),
        sa.UniqueConstraint("user_id", "offer_id", name="uq_favorites_user_offer"),
        sa.UniqueConstraint("user_id", "shop_id", name="uq_favorites_user_shop"),
    )
    op.create_index(op.f("ix_favorites_user_id"), "favorites", ["user_id"], unique=False)
    op.create_index(op.f("ix_favorites_offer_id"), "favorites", ["offer_id"], unique=False)
    op.create_index(op.f("ix_favorites_shop_id"), "favorites", ["shop_id"], unique=False)


def downgrade() -> None:
    op.drop_index(op.f("ix_favorites_shop_id"), table_name="favorites")
    op.drop_index(op.f("ix_favorites_offer_id"), table_name="favorites")
    op.drop_index(op.f("ix_favorites_user_id"), table_name="favorites")
    op.drop_table("favorites")
