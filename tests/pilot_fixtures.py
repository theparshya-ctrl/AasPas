"""Shared pilot-area fixtures for customer journey API tests."""

import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal

import pytest
from fastapi.testclient import TestClient

from aaspas.main import app
from aaspas.modules.auth.models import User
from aaspas.modules.category.models import Category
from aaspas.modules.home.router import get_now
from aaspas.modules.location.models import Location
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus

FIXED_NOW = datetime(2026, 8, 19, 12, 0, 0, tzinfo=UTC)
PIMPRI_LAT = 18.6298
PIMPRI_LON = 73.7997
NEAR_LAT = 18.6310
NEAR_LON = 73.8010
FAR_LAT = 19.076090
FAR_LON = 72.877426


@pytest.fixture
def fixed_now_client(client: TestClient):
    app.dependency_overrides[get_now] = lambda: FIXED_NOW
    yield client
    app.dependency_overrides.pop(get_now, None)


@pytest.fixture
def pilot_data(db_session):
    category = Category(
        id=uuid.UUID("a1000000-0000-4000-8000-000000000001"),
        name="Clothing / Fashion",
        slug="clothing-fashion",
        is_active=True,
        display_order=1,
    )
    db_session.add(category)

    owner = User(
        email="owner-home@test.com",
        password_hash="hash",
        role="shop_owner",
    )
    db_session.add(owner)
    db_session.flush()

    active_shop = Shop(
        owner_id=owner.id,
        name="Pimpri Fashion",
        slug="pimpri-fashion",
        category="Clothing / Fashion",
        category_id=category.id,
        contact_number="+919876543210",
        business_hours={"opens_at": "09:00", "closes_at": "21:00"},
        status=ShopStatus.ACTIVE.value,
        approved_at=FIXED_NOW - timedelta(days=30),
    )
    inactive_shop = Shop(
        owner_id=owner.id,
        name="Draft Shop",
        slug="draft-shop",
        status=ShopStatus.DRAFT.value,
    )
    db_session.add_all([active_shop, inactive_shop])
    db_session.flush()

    db_session.add_all(
        [
            Location(
                shop_id=active_shop.id,
                label="Primary",
                address_line1="Market Road",
                city="Pimpri-Chinchwad",
                state="MH",
                postal_code="411018",
                country="IN",
                latitude=Decimal(str(PIMPRI_LAT)),
                longitude=Decimal(str(PIMPRI_LON)),
                is_primary=True,
            ),
            Location(
                shop_id=inactive_shop.id,
                label="Primary",
                address_line1="Hidden",
                city="Pimpri",
                country="IN",
                latitude=Decimal(str(PIMPRI_LAT)),
                longitude=Decimal(str(PIMPRI_LON)),
                is_primary=True,
            ),
        ]
    )

    offers = [
        Offer(
            shop_id=active_shop.id,
            title="Draft Offer",
            discount_type="percentage",
            discount_value=Decimal("10"),
            status=OfferStatus.DRAFT.value,
            starts_at=FIXED_NOW + timedelta(days=1),
            ends_at=FIXED_NOW + timedelta(days=5),
        ),
        Offer(
            shop_id=active_shop.id,
            title="Pending Offer",
            discount_type="percentage",
            discount_value=Decimal("10"),
            status=OfferStatus.PENDING_APPROVAL.value,
            starts_at=FIXED_NOW + timedelta(days=1),
            ends_at=FIXED_NOW + timedelta(days=5),
        ),
        Offer(
            shop_id=active_shop.id,
            title="Coming Soon Offer",
            discount_type="percentage",
            discount_value=Decimal("15"),
            status=OfferStatus.SCHEDULED.value,
            starts_at=FIXED_NOW + timedelta(days=2),
            ends_at=FIXED_NOW + timedelta(days=7),
        ),
        Offer(
            shop_id=active_shop.id,
            title="Today Offer",
            discount_type="percentage",
            discount_value=Decimal("20"),
            status=OfferStatus.ACTIVE.value,
            starts_at=FIXED_NOW - timedelta(hours=1),
            ends_at=FIXED_NOW + timedelta(days=1),
        ),
        Offer(
            shop_id=active_shop.id,
            title="Expired Offer",
            discount_type="percentage",
            discount_value=Decimal("25"),
            status=OfferStatus.EXPIRED.value,
            starts_at=FIXED_NOW - timedelta(days=5),
            ends_at=FIXED_NOW - timedelta(days=1),
        ),
        Offer(
            shop_id=inactive_shop.id,
            title="Inactive Shop Offer",
            discount_type="percentage",
            discount_value=Decimal("30"),
            status=OfferStatus.ACTIVE.value,
            starts_at=FIXED_NOW - timedelta(hours=1),
            ends_at=FIXED_NOW + timedelta(days=1),
        ),
    ]
    db_session.add_all(offers)
    db_session.commit()
    return {
        "category": category,
        "active_shop": active_shop,
        "inactive_shop": inactive_shop,
        "offers": offers,
    }
