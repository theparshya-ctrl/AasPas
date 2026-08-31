"""Categories, shop category FK, and offer query indexes.

Revision ID: 003_customer_home
Revises: 002_shop_onboarding
Create Date: 2026-08-19

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "003_customer_home"
down_revision: Union[str, None] = "002_shop_onboarding"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "categories",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("slug", sa.String(length=100), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("display_order", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("name"),
        sa.UniqueConstraint("slug"),
    )
    op.create_index(op.f("ix_categories_slug"), "categories", ["slug"], unique=False)
    op.create_index(op.f("ix_categories_is_active"), "categories", ["is_active"], unique=False)
    op.create_index(op.f("ix_categories_display_order"), "categories", ["display_order"], unique=False)

    op.add_column(
        "shops",
        sa.Column("category_id", postgresql.UUID(as_uuid=True), nullable=True),
    )
    op.create_foreign_key(
        "fk_shops_category_id_categories",
        "shops",
        "categories",
        ["category_id"],
        ["id"],
        ondelete="SET NULL",
    )
    op.create_index(op.f("ix_shops_category_id"), "shops", ["category_id"], unique=False)

    op.create_index("ix_offers_customer_window", "offers", ["status", "starts_at", "ends_at"], unique=False)

    # Seed Clothing / Fashion category (pilot) — fixed UUID for reproducibility
    op.execute(
        """
        INSERT INTO categories (id, name, slug, is_active, display_order, created_at, updated_at)
        VALUES (
            'a1000000-0000-4000-8000-000000000001',
            'Clothing / Fashion',
            'clothing-fashion',
            true,
            1,
            NOW(),
            NOW()
        )
        ON CONFLICT (slug) DO NOTHING
        """
    )


def downgrade() -> None:
    op.drop_index("ix_offers_customer_window", table_name="offers")
    op.drop_index(op.f("ix_shops_category_id"), table_name="shops")
    op.drop_constraint("fk_shops_category_id_categories", "shops", type_="foreignkey")
    op.drop_column("shops", "category_id")
    op.drop_index(op.f("ix_categories_display_order"), table_name="categories")
    op.drop_index(op.f("ix_categories_is_active"), table_name="categories")
    op.drop_index(op.f("ix_categories_slug"), table_name="categories")
    op.drop_table("categories")
