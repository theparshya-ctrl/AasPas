import re

from sqlalchemy.orm import Session

from aaspas.modules.shop.models import Shop


def slugify(name: str) -> str:
    slug = name.lower().strip()
    slug = re.sub(r"[^a-z0-9\s-]", "", slug)
    slug = re.sub(r"[\s-]+", "-", slug).strip("-")
    return slug[:200] or "shop"


def unique_slug(db: Session, name: str) -> str:
    base = slugify(name)
    candidate = base
    suffix = 1
    while db.query(Shop.id).filter(Shop.slug == candidate).first() is not None:
        candidate = f"{base}-{suffix}"
        suffix += 1
    return candidate
