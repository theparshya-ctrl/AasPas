"""Add offer photo_storage_key for local media uploads.

Revision ID: 007_offer_photo_storage
Revises: 006_notification_in_app
Create Date: 2026-08-26
"""

from alembic import op
import sqlalchemy as sa

revision = "007_offer_photo_storage"
down_revision = "006_notification_in_app"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("offers", sa.Column("photo_storage_key", sa.String(length=512), nullable=True))


def downgrade() -> None:
    op.drop_column("offers", "photo_storage_key")
