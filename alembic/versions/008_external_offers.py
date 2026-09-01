"""External offer source fields and system account flag.

Revision ID: 008_external_offers
Revises: 007_offer_photo_storage
Create Date: 2026-08-31
"""

from alembic import op
import sqlalchemy as sa

revision = "008_external_offers"
down_revision = "007_offer_photo_storage"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("is_system_account", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "shops",
        sa.Column("source_type", sa.String(length=20), nullable=False, server_default="AASPAS"),
    )
    op.add_column(
        "shops",
        sa.Column("external_source_key", sa.String(length=255), nullable=True),
    )
    op.create_index("ix_shops_external_source_key", "shops", ["external_source_key"], unique=True)

    op.add_column(
        "offers",
        sa.Column("source_type", sa.String(length=20), nullable=False, server_default="AASPAS"),
    )
    op.add_column("offers", sa.Column("source_name", sa.String(length=255), nullable=True))
    op.add_column("offers", sa.Column("source_url", sa.String(length=1024), nullable=True))
    op.add_column("offers", sa.Column("collected_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column(
        "offers",
        sa.Column("external_source_key", sa.String(length=255), nullable=True),
    )
    op.create_index("ix_offers_external_source_key", "offers", ["external_source_key"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_offers_external_source_key", table_name="offers")
    op.drop_column("offers", "external_source_key")
    op.drop_column("offers", "collected_at")
    op.drop_column("offers", "source_url")
    op.drop_column("offers", "source_name")
    op.drop_column("offers", "source_type")

    op.drop_index("ix_shops_external_source_key", table_name="shops")
    op.drop_column("shops", "external_source_key")
    op.drop_column("shops", "source_type")

    op.drop_column("users", "is_system_account")
