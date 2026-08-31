"""Pilot seed data — Pimpri-Chinchwad × Clothing/Fashion (demo only).

All shop names are clearly fictional demo/pilot data for customer discovery testing.

Usage:
    PYTHONPATH=src python scripts/seed_pilot.py
"""

from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal

from aaspas.common.security.auth import hash_password
from aaspas.database import SessionLocal
from aaspas.modules.auth.models import User
from aaspas.modules.category.models import Category
from aaspas.modules.location.models import Location
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus

CATEGORY_ID = uuid.UUID("a1000000-0000-4000-8000-000000000001")

# Realistic Pimpri-Chinchwad area coordinates (demo placements only)
AREAS = {
    "pimpri": (Decimal("18.6298"), Decimal("73.7997")),
    "chinchwad": (Decimal("18.6278"), Decimal("73.7911")),
    "akurdi": (Decimal("18.6150"), Decimal("73.7650")),
    "nigdi": (Decimal("18.6570"), Decimal("73.7700")),
}

FAR_MUMBAI = (Decimal("19.076090"), Decimal("72.877426"))

DEMO_SHOPS: list[dict] = [
    {
        "slug": "demo-pimpri-style-studio",
        "name": "Demo Pimpri Style Studio",
        "area": "pimpri",
        "address": "Pimpri Market Road",
        "description": "Demo clothing boutique near Pimpri Market",
        "photo": "https://cdn.example.demo/shops/pimpri-style-studio.jpg",
    },
    {
        "slug": "demo-fashion-hub-pimpri",
        "name": "Demo Fashion Hub Pimpri",
        "area": "pimpri",
        "address": "Old Mumbai-Pune Road, Pimpri",
        "description": "Demo multi-brand fashion outlet",
        "photo": "https://cdn.example.demo/shops/fashion-hub-pimpri.jpg",
    },
    {
        "slug": "demo-ethnic-wear-pimpri",
        "name": "Demo Ethnic Wear Pimpri",
        "area": "pimpri",
        "address": "Pimpri Station Road",
        "description": "Demo ethnic and festive wear",
        "photo": "https://cdn.example.demo/shops/ethnic-wear-pimpri.jpg",
    },
    {
        "slug": "demo-chinchwad-trend-mart",
        "name": "Demo Chinchwad Trend Mart",
        "area": "chinchwad",
        "address": "Chinchwad Main Road",
        "description": "Demo trend-focused clothing store",
        "photo": "https://cdn.example.demo/shops/chinchwad-trend-mart.jpg",
    },
    {
        "slug": "demo-chinchwad-denim-co",
        "name": "Demo Chinchwad Denim Co.",
        "area": "chinchwad",
        "address": "Near Elpro City, Chinchwad",
        "description": "Demo denim and casual wear",
        "photo": "https://cdn.example.demo/shops/chinchwad-denim-co.jpg",
    },
    {
        "slug": "demo-akurdi-wardrobe",
        "name": "Demo Akurdi Wardrobe",
        "area": "akurdi",
        "address": "Akurdi Station Road",
        "description": "Demo family clothing store",
        "photo": "https://cdn.example.demo/shops/akurdi-wardrobe.jpg",
    },
    {
        "slug": "demo-akurdi-kids-corner",
        "name": "Demo Akurdi Kids Corner",
        "area": "akurdi",
        "address": "Akurdi Gaothan Road",
        "description": "Demo kids and teens fashion",
        "photo": "https://cdn.example.demo/shops/akurdi-kids-corner.jpg",
    },
    {
        "slug": "demo-nigdi-boutique",
        "name": "Demo Nigdi Boutique",
        "area": "nigdi",
        "address": "Nigdi Pradhikaran Sector 21",
        "description": "Demo boutique for women’s wear",
        "photo": "https://cdn.example.demo/shops/nigdi-boutique.jpg",
    },
    {
        "slug": "demo-nigdi-formal-wear",
        "name": "Demo Nigdi Formal Wear",
        "area": "nigdi",
        "address": "Nigdi Chowk",
        "description": "Demo formal and office wear",
        "photo": "https://cdn.example.demo/shops/nigdi-formal-wear.jpg",
    },
    {
        "slug": "demo-pimpri-sports-fit",
        "name": "Demo Pimpri Sports Fit",
        "area": "pimpri",
        "address": "Pimpri Waghere Road",
        "description": "Demo activewear and sports fashion",
        "photo": "https://cdn.example.demo/shops/pimpri-sports-fit.jpg",
    },
    {
        "slug": "demo-chinchwad-festive",
        "name": "Demo Chinchwad Festive Collection",
        "area": "chinchwad",
        "address": "Chinchwad Gaothan",
        "description": "Demo festive and wedding wear",
        "photo": "https://cdn.example.demo/shops/chinchwad-festive.jpg",
    },
    {
        "slug": "demo-mumbai-fashion-far",
        "name": "Demo Mumbai Fashion Far",
        "area": "far",
        "address": "Andheri West, Mumbai",
        "description": "Demo far-away shop for distance testing",
        "photo": "https://cdn.example.demo/shops/mumbai-fashion-far.jpg",
    },
]

OFFER_TEMPLATES: list[dict] = [
    {"suffix": "Flat 20% Off", "status": OfferStatus.ACTIVE, "start_days": -1, "end_days": 3, "discount": "20"},
    {"suffix": "Buy 2 Get 1", "status": OfferStatus.ACTIVE, "start_days": -2, "end_days": 5, "discount": "15", "type": "bogo"},
    {"suffix": "Weekend Preview", "status": OfferStatus.SCHEDULED, "start_days": 4, "end_days": 10, "discount": "25"},
    {"suffix": "Monsoon Sale Soon", "status": OfferStatus.SCHEDULED, "start_days": 7, "end_days": 14, "discount": "30"},
    {"suffix": "Clearance Expired", "status": OfferStatus.EXPIRED, "start_days": -20, "end_days": -5, "discount": "40"},
    {"suffix": "Draft Internal", "status": OfferStatus.DRAFT, "start_days": 1, "end_days": 7, "discount": "10"},
    {"suffix": "Pending Review", "status": OfferStatus.PENDING_APPROVAL, "start_days": 2, "end_days": 8, "discount": "12"},
]


def seed() -> None:
    db = SessionLocal()
    now = datetime.now(UTC)
    try:
        category = db.get(Category, CATEGORY_ID)
        if category is None:
            category = Category(
                id=CATEGORY_ID,
                name="Clothing / Fashion",
                slug="clothing-fashion",
                is_active=True,
                display_order=1,
            )
            db.add(category)
            db.flush()

        owner = db.query(User).filter(User.email == "pilot-owner@aaspas.demo").one_or_none()
        if owner is None:
            owner = User(
                email="pilot-owner@aaspas.demo",
                password_hash=hash_password("demo-password-change-me"),
                full_name="Pimpri Pilot Owner",
                role="shop_owner",
            )
            db.add(owner)
            db.flush()

        shops_created = 0
        offers_created = 0

        for index, spec in enumerate(DEMO_SHOPS):
            shop = db.query(Shop).filter(Shop.slug == spec["slug"]).one_or_none()
            if shop is None:
                lat, lon = FAR_MUMBAI if spec["area"] == "far" else AREAS[spec["area"]]
                # Slight offset so pins do not stack exactly
                lat = lat + Decimal(str((index % 5) * 0.001))
                lon = lon + Decimal(str((index % 4) * 0.001))

                shop = Shop(
                    owner_id=owner.id,
                    name=spec["name"],
                    slug=spec["slug"],
                    description=spec["description"],
                    category="Clothing / Fashion",
                    category_id=category.id,
                    contact_number=f"+9199999{index:05d}",
                    status=ShopStatus.ACTIVE.value,
                    photo_url=spec["photo"],
                    approved_at=now - timedelta(days=30 + index),
                )
                db.add(shop)
                db.flush()
                db.add(
                    Location(
                        shop_id=shop.id,
                        label="Primary",
                        address_line1=spec["address"],
                        city="Pimpri-Chinchwad" if spec["area"] != "far" else "Mumbai",
                        state="MH",
                        postal_code="4110{}".format(17 + (index % 3)),
                        country="IN",
                        latitude=lat,
                        longitude=lon,
                        is_primary=True,
                    )
                )
                shops_created += 1

            # First shop keeps full lifecycle mix; others get customer-visible variety
            templates = OFFER_TEMPLATES if index == 0 else OFFER_TEMPLATES[:4]
            for template in templates:
                title = f"{spec['name']} — {template['suffix']}"
                existing = db.query(Offer).filter(Offer.title == title).one_or_none()
                if existing:
                    continue
                starts = now + timedelta(days=template["start_days"])
                if template["status"] == OfferStatus.ACTIVE:
                    starts = now + timedelta(hours=-2 - index)
                db.add(
                    Offer(
                        shop_id=shop.id,
                        title=title,
                        description=f"Demo pilot offer at {spec['name']}",
                        discount_type=template.get("type", "percentage"),
                        discount_value=Decimal(template["discount"]),
                        status=template["status"].value,
                        starts_at=starts,
                        ends_at=now + timedelta(days=template["end_days"]),
                    )
                )
                offers_created += 1

        db.commit()
        shop_count = db.query(Shop).filter(Shop.status == ShopStatus.ACTIVE.value).count()
        offer_count = db.query(Offer).count()
        print(
            f"Pilot seed completed (demo data). "
            f"shops_active={shop_count}, offers_total={offer_count}, "
            f"new_shops={shops_created}, new_offers={offers_created}"
        )
    finally:
        db.close()


if __name__ == "__main__":
    seed()
