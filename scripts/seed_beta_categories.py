"""Seed minimum Beta categories for external offer imports (Beta Neon only).

Usage:
    PYTHONPATH=src python scripts/seed_beta_categories.py

Reads .env.beta — never targets DEV dev.db.
"""

from __future__ import annotations

import os
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

BETA_CATEGORIES: list[tuple[str, str, int, uuid.UUID | None]] = [
    ("Clothing / Fashion", "clothing-fashion", 1, uuid.UUID("a1000000-0000-4000-8000-000000000001")),
    ("Electronics", "electronics", 2, None),
    ("Food", "food", 3, None),
    ("Restaurant", "restaurant", 4, None),
    ("Pharmacy", "pharmacy", 5, None),
    ("Beauty", "beauty", 6, None),
    ("Fitness", "fitness", 7, None),
    ("Photo Studio", "photo-studio", 8, None),
]


def load_env_file(path: Path) -> None:
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ[key.strip()] = value.strip().strip('"').strip("'")


def main() -> int:
    load_env_file(ROOT / ".env.beta")
    from aaspas.config import get_settings
    from aaspas.database import SessionLocal
    from aaspas.modules.category.models import Category
    from aaspas.modules.external.import_service import ExternalOfferImportService

    get_settings.cache_clear()
    ExternalOfferImportService.assert_beta_import_target()

    db = SessionLocal()
    created = 0
    reused = 0
    try:
        for name, slug, display_order, fixed_id in BETA_CATEGORIES:
            existing = db.query(Category).filter(Category.slug == slug).one_or_none()
            if existing is not None:
                if not existing.is_active:
                    existing.is_active = True
                reused += 1
                continue
            by_name = db.query(Category).filter(Category.name == name).one_or_none()
            if by_name is not None:
                reused += 1
                continue
            category = Category(
                id=fixed_id or uuid.uuid4(),
                name=name,
                slug=slug,
                is_active=True,
                display_order=display_order,
            )
            db.add(category)
            created += 1
        db.commit()
        print(f"Beta categories: created={created} reused={reused}")
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
