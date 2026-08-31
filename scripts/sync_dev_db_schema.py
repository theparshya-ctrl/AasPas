"""Ensure local dev.db has columns required by the current SQLAlchemy models.

The phone dev script creates dev.db with create_all once. If models change later,
existing dev.db files can drift. This script adds missing offer verification columns.
"""

from __future__ import annotations

import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "dev.db"

OFFER_COLUMNS: list[tuple[str, str]] = [
    ("photo_url", "VARCHAR(512)"),
    ("applicable_products", "TEXT"),
    ("min_purchase_amount", "NUMERIC(10, 2)"),
    ("terms", "TEXT"),
    ("merchant_confirmed_at", "DATETIME"),
    ("merchant_confirmed_by", "UUID"),
    ("submitted_at", "DATETIME"),
    ("approved_at", "DATETIME"),
    ("rejected_at", "DATETIME"),
    ("rejection_reason", "TEXT"),
    ("is_verified", "BOOLEAN NOT NULL DEFAULT 0"),
]


def sync() -> None:
    if not DB_PATH.exists():
        return
    conn = sqlite3.connect(DB_PATH)
    try:
        existing = {row[1] for row in conn.execute("PRAGMA table_info(offers)").fetchall()}
        if not existing:
            return
        for name, col_type in OFFER_COLUMNS:
            if name not in existing:
                conn.execute(f"ALTER TABLE offers ADD COLUMN {name} {col_type}")
        conn.commit()
    finally:
        conn.close()


if __name__ == "__main__":
    sync()
    print("dev.db schema sync complete")
