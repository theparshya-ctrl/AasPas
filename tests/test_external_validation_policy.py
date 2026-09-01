"""Tests for CURSOR-045 external validation policy."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta
from decimal import Decimal

import pytest

from aaspas.common.exceptions import ValidationAppError
from aaspas.common.source_type import SourceType
from aaspas.modules.category.models import Category
from aaspas.modules.external.import_service import ExternalOfferImportService
from aaspas.modules.external.schemas import ExternalOfferImportRecord
from aaspas.modules.external.validation import (
    ExternalValidationOutcome,
    classify_import_record,
    is_vague_offer_text,
)
from aaspas.modules.offer.visibility import CustomerOfferVisibility, resolve_customer_visibility
from aaspas.modules.shop.status import ShopStatus
from tests.pilot_fixtures import FIXED_NOW, NEAR_LAT, NEAR_LON, fixed_now_client, pilot_data
from tests.test_external_offers import import_external, make_external_record

import uuid


@pytest.fixture
def external_categories(db_session):
    names = [
        "Clothing / Fashion",
        "Electronics",
        "Restaurant",
        "Pharmacy",
        "Beauty",
        "Fitness",
        "Photo Studio",
    ]
    for index, name in enumerate(names, start=1):
        slug = name.lower().replace(" / ", "-").replace(" ", "-")
        if db_session.query(Category).filter(Category.slug == slug).one_or_none() is None:
            db_session.add(
                Category(
                    id=uuid.uuid4(),
                    name=name,
                    slug=slug,
                    is_active=True,
                    display_order=index,
                )
            )
    db_session.commit()
    return {name for name in names}


class TestExternalValidationPolicy:
    def test_up_to_offer_is_not_vague(self):
        assert not is_vague_offer_text("Save up to 70% on listed vouchers")

    def test_vague_offer_rejected(self):
        assert is_vague_offer_text("Great deals available")

    def test_classify_accepts_up_to_offer(self, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                title="Save up to 70% on listed vouchers",
                discount_type="up_to_percentage",
                discount_value=70,
                category="Restaurant",
                external_source_key="ext-upto-key-001",
            )
        )
        outcome, reasons = classify_import_record(record, active_categories=external_categories)
        assert outcome == ExternalValidationOutcome.VALID
        assert not reasons

    def test_classify_rejects_vague_offer(self, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                title="Great deals available",
                external_source_key="ext-vague-key-001",
            )
        )
        outcome, _reasons = classify_import_record(record, active_categories=external_categories)
        assert outcome == ExternalValidationOutcome.REJECTED

    def test_import_ongoing_external_without_expiry(self, db_session, pilot_data, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-ongoing-key-001",
                ends_at=None,
                starts_at=None,
            )
        )
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-ongoing-key-001",
            ends_at=None,
            starts_at=None,
        )
        assert offer.ends_at is None
        assert offer.source_type == SourceType.EXTERNAL.value
        assert offer.is_verified is False

    def test_ongoing_external_visible_to_customers(self, db_session, pilot_data, external_categories):
        from aaspas.modules.shop.models import Shop

        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-visible-ongoing-001",
            ends_at=None,
            starts_at=None,
        )
        shop = db_session.get(Shop, offer.shop_id)
        assert shop is not None
        shop.status = ShopStatus.ACTIVE.value
        db_session.commit()
        visibility = resolve_customer_visibility(offer, shop, FIXED_NOW)
        assert visibility == CustomerOfferVisibility.ACTIVE

    def test_explicit_expired_external_rejected(self, db_session, pilot_data, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-expired-explicit-001",
                ends_at=(FIXED_NOW - timedelta(days=1)).isoformat(),
            )
        )
        with pytest.raises(ValidationAppError, match="explicitly expired"):
            ExternalOfferImportService(db_session).import_record(record, now=FIXED_NOW)

    def test_up_to_wording_preserved_on_import(self, db_session, pilot_data, external_categories):
        title = "Save up to 70% on listed vouchers"
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-upto-import-001",
            title=title,
            discount_type="up_to_percentage",
            discount_value=70,
            category="Restaurant",
        )
        assert offer.title == title

    def test_home_shows_ongoing_external_offer(self, fixed_now_client, db_session, pilot_data, external_categories):
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-home-ongoing-001",
            ends_at=None,
            starts_at=None,
        )
        response = fixed_now_client.get(
            "/api/v1/home",
            params={"latitude": NEAR_LAT, "longitude": NEAR_LON},
        )
        titles = {item["title"] for item in response.json()["data"]["today_offers"]}
        assert offer.title in titles

    def test_search_ongoing_external_null_ends_at(self, fixed_now_client, db_session, pilot_data, external_categories):
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-search-ongoing-001",
            ends_at=None,
            starts_at=None,
            title="Ongoing Search External Offer",
        )
        response = fixed_now_client.get(
            "/api/v1/search",
            params={"q": "Ongoing Search", "latitude": NEAR_LAT, "longitude": NEAR_LON},
        )
        assert response.status_code == 200
        match = next(o for o in response.json()["data"]["offers"] if o["title"] == offer.title)
        assert match["is_verified"] is False
        assert match["source_type"] == SourceType.EXTERNAL.value
        assert match["ends_at"] is None

    def test_external_visibility_uses_utc_instant_not_calendar_day(
        self, db_session, pilot_data, external_categories
    ):
        from datetime import UTC, datetime

        from aaspas.modules.shop.models import Shop

        collected = datetime(2026, 9, 1, 0, 0, 0, tzinfo=UTC)
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-utc-boundary-001",
            collected_at=collected.isoformat(),
            starts_at=None,
            ends_at=None,
        )
        shop = db_session.get(Shop, offer.shop_id)
        assert shop is not None
        before = datetime(2026, 8, 31, 23, 59, 0, tzinfo=UTC)
        after = datetime(2026, 9, 1, 0, 1, 0, tzinfo=UTC)
        assert resolve_customer_visibility(offer, shop, before) == CustomerOfferVisibility.COMING_SOON
        assert resolve_customer_visibility(offer, shop, after) == CustomerOfferVisibility.ACTIVE

    def test_area_only_address_needs_review(self, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-area-only-001",
                address={
                    "address_line1": "Pimpri",
                    "city": "Pimpri",
                    "country": "IN",
                },
            )
        )
        outcome, reasons = classify_import_record(record, active_categories=external_categories)
        assert outcome == ExternalValidationOutcome.NEEDS_REVIEW
        assert any("area-only" in reason for reason in reasons)
