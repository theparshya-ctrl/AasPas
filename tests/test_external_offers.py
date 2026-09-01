"""Tests for CURSOR-042 external offer system."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta

import pytest

from aaspas.common.exceptions import ValidationAppError
from aaspas.common.security.auth import CurrentUser
from aaspas.common.security.rbac import UserRole
from aaspas.common.source_type import SourceType
from aaspas.config import get_settings
from aaspas.modules.category.models import Category
from aaspas.modules.external.constants import (
    EXTERNAL_DATA_OWNER_EMAIL,
    EXTERNAL_DATA_OWNER_FULL_NAME,
)
from aaspas.modules.external.import_service import ExternalOfferImportService
from aaspas.modules.external.schemas import ExternalOfferImportRecord
from aaspas.modules.notification.models import Notification
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.service import OfferService
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from tests.helpers import admin_headers, create_admin_user, register_and_login, register_customer
from tests.pilot_fixtures import FIXED_NOW, NEAR_LAT, NEAR_LON, fixed_now_client, pilot_data
from tests.test_offer_lifecycle import create_active_shop, offer_payload

EXTERNAL_KEY = "ext-test-offer-key-001"


def make_external_record(**overrides) -> dict:
    base = {
        "external_source_key": EXTERNAL_KEY,
        "shop_name": "External Fashion Pimpri",
        "category": "Clothing / Fashion",
        "address": {
            "address_line1": "123 External Road",
            "city": "Pimpri-Chinchwad",
            "state": "MH",
            "postal_code": "411018",
            "country": "IN",
            "latitude": 18.6298,
            "longitude": 73.7997,
        },
        "title": "External Test Offer",
        "description": "Collected from a public source for testing.",
        "discount_type": "percentage",
        "discount_value": 15,
        "starts_at": (FIXED_NOW - timedelta(hours=1)).isoformat(),
        "ends_at": (FIXED_NOW + timedelta(days=3)).isoformat(),
        "source_name": "Public website",
        "source_url": "https://example.com/offers/test-001",
        "collected_at": FIXED_NOW.isoformat(),
    }
    base.update(overrides)
    return base


def import_external(db_session, pilot_data, **overrides) -> Offer:
    record = ExternalOfferImportRecord.model_validate(make_external_record(**overrides))
    action, offer_id = ExternalOfferImportService(db_session).import_record(record, now=FIXED_NOW)
    assert action in {"created", "updated"}
    offer = db_session.get(Offer, offer_id)
    assert offer is not None
    return offer


@pytest.fixture
def external_offer(db_session, pilot_data):
    return import_external(db_session, pilot_data)


class TestExternalOfferImport:
    def test_import_creates_external_offer(self, external_offer):
        assert external_offer.source_type == SourceType.EXTERNAL.value
        assert external_offer.is_verified is False
        assert external_offer.external_source_key == EXTERNAL_KEY
        assert external_offer.source_name == "Public website"

    def test_import_creates_external_shop_with_system_owner(self, db_session, external_offer):
        shop = db_session.get(Shop, external_offer.shop_id)
        assert shop is not None
        assert shop.source_type == SourceType.EXTERNAL.value
        assert shop.external_source_key is not None
        from aaspas.modules.auth.models import User

        owner = db_session.get(User, shop.owner_id)
        assert owner is not None
        assert owner.email == EXTERNAL_DATA_OWNER_EMAIL
        assert owner.is_system_account is True
        assert owner.is_active is False
        assert owner.full_name == EXTERNAL_DATA_OWNER_FULL_NAME

    def test_external_source_key_deduplication(self, db_session, pilot_data, external_offer):
        updated = import_external(
            db_session,
            pilot_data,
            title="Updated External Title",
        )
        assert updated.id == external_offer.id
        assert updated.title == "Updated External Title"

    def test_expired_external_offer_rejected(self, db_session, pilot_data):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-expired-key-001",
                ends_at=(FIXED_NOW - timedelta(days=1)).isoformat(),
            )
        )
        with pytest.raises(ValidationAppError, match="explicitly expired"):
            ExternalOfferImportService(db_session).import_record(record, now=FIXED_NOW)

    def test_import_creates_no_notifications(self, db_session, pilot_data):
        before = db_session.query(Notification).count()
        import_external(
            db_session,
            pilot_data,
            external_source_key="ext-notify-key-001",
        )
        after = db_session.query(Notification).count()
        assert before == after


class TestExternalImportSafety:
    def test_assert_beta_import_target_blocks_sqlite(self, monkeypatch):
        monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
        monkeypatch.setenv("APP_ENV", "staging")
        get_settings.cache_clear()
        with pytest.raises(ValidationAppError, match="SQLite"):
            ExternalOfferImportService.assert_beta_import_target()
        get_settings.cache_clear()

    def test_assert_beta_import_target_blocks_development(self, monkeypatch):
        monkeypatch.setenv("DATABASE_URL", "postgresql://user:pass@neon.example/db")
        monkeypatch.setenv("APP_ENV", "development")
        get_settings.cache_clear()
        with pytest.raises(ValidationAppError, match="development"):
            ExternalOfferImportService.assert_beta_import_target()
        get_settings.cache_clear()


class TestExternalOfferCustomerVisibility:
    def test_home_visibility(self, fixed_now_client, db_session, pilot_data, external_offer):
        response = fixed_now_client.get(
            "/api/v1/home",
            params={"latitude": NEAR_LAT, "longitude": NEAR_LON},
        )
        assert response.status_code == 200
        titles = {item["title"] for item in response.json()["data"]["today_offers"]}
        assert external_offer.title in titles
        item = next(
            o for o in response.json()["data"]["today_offers"] if o["title"] == external_offer.title
        )
        assert item["source_type"] == SourceType.EXTERNAL.value
        assert item["is_verified"] is False

    def test_search_visibility(self, fixed_now_client, db_session, pilot_data, external_offer):
        response = fixed_now_client.get(
            "/api/v1/search",
            params={
                "q": "External",
                "latitude": NEAR_LAT,
                "longitude": NEAR_LON,
            },
        )
        assert response.status_code == 200
        titles = {item["title"] for item in response.json()["data"]["offers"]}
        assert external_offer.title in titles

    def test_category_visibility(self, fixed_now_client, db_session, pilot_data, external_offer):
        category_id = str(pilot_data["category"].id)
        response = fixed_now_client.get(
            f"/api/v1/categories/{category_id}/offers",
            params={"latitude": NEAR_LAT, "longitude": NEAR_LON},
        )
        assert response.status_code == 200
        titles = {item["title"] for item in response.json()["data"]["today_offers"]}
        assert external_offer.title in titles

    def test_offer_detail_fields(self, fixed_now_client, external_offer):
        response = fixed_now_client.get(f"/api/v1/offers/{external_offer.id}")
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["source_type"] == SourceType.EXTERNAL.value
        assert data["is_verified"] is False
        assert data["source_name"] == "Public website"
        assert data["source_url"].startswith("https://")

    def test_favorites_is_verified_mapping(self, fixed_now_client, pilot_data, external_offer):
        headers = register_customer(fixed_now_client, "ext-fav-customer@test.com")
        save = fixed_now_client.post(
            f"/api/v1/favorites/offers/{external_offer.id}",
            headers=headers,
        )
        assert save.status_code == 201
        favorites = fixed_now_client.get(
            "/api/v1/favorites",
            headers=headers,
            params={"latitude": NEAR_LAT, "longitude": NEAR_LON},
        )
        item = favorites.json()["data"]["offers"][0]
        assert item["is_verified"] is False
        assert item["source_type"] == SourceType.EXTERNAL.value
        assert item["source_name"] == "Public website"


class TestExternalOfferGuards:
    def test_approve_offer_rejects_external(self, db_session, external_offer):
        external_offer.status = OfferStatus.PENDING_APPROVAL.value
        db_session.commit()
        admin_user = create_admin_user(db_session, "ext-approve-admin@test.com")
        admin = CurrentUser(
            id=admin_user.id,
            email=admin_user.email,
            role=UserRole.ADMIN,
            shop_id=None,
        )
        service = OfferService(db_session)
        with pytest.raises(ValidationAppError, match="External offers cannot be approved"):
            service.approve_offer(external_offer.id, admin, FIXED_NOW)

    def test_merchant_cannot_create_offer_on_external_shop(self, client, db_session, pilot_data):
        external_offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-guard-shop-key-001",
        )
        owner_headers = register_and_login(client, "ext-real-owner@test.com")
        response = client.post(
            "/api/v1/offers",
            json=offer_payload(str(external_offer.shop_id), title="Should Fail"),
            headers=owner_headers,
        )
        assert response.status_code == 403


class TestSystemOwnerIsolation:
    def test_cannot_register_system_email(self, client):
        response = client.post(
            "/api/v1/auth/register",
            json={
                "email": EXTERNAL_DATA_OWNER_EMAIL,
                "password": "password123",
                "role": "shop_owner",
            },
        )
        assert response.status_code == 422

    def test_system_owner_not_in_admin_user_list(self, client, db_session, pilot_data, external_offer):
        admin = admin_headers(client, db_session, "ext-admin-users@test.com")
        users = client.get("/api/v1/admin/users", headers=admin)
        assert users.status_code == 200
        emails = {item["email"] for item in users.json()["data"]}
        assert EXTERNAL_DATA_OWNER_EMAIL not in emails

    def test_system_owner_cannot_login(self, client, db_session, pilot_data, external_offer):
        response = client.post(
            "/api/v1/auth/login",
            json={"email": EXTERNAL_DATA_OWNER_EMAIL, "password": "any-password"},
        )
        assert response.status_code == 401

    def test_admin_shop_list_masks_system_owner(self, client, db_session, pilot_data, external_offer):
        admin = admin_headers(client, db_session, "ext-admin-shops@test.com")
        shops = client.get("/api/v1/admin/shops", headers=admin)
        assert shops.status_code == 200
        item = next(
            s for s in shops.json()["data"] if s["shop_id"] == str(external_offer.shop_id)
        )
        assert item["source_type"] == SourceType.EXTERNAL.value
        assert item["owner_email"] == EXTERNAL_DATA_OWNER_FULL_NAME


class TestExistingAasPasVerificationUnchanged:
    def test_aaspas_offer_still_verifiable(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client,
            "ext-aaspas-owner@test.com",
            "ext-aaspas-admin@test.com",
            db_session,
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id),
            headers=headers,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        approve = client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        assert approve.status_code == 200
        assert approve.json()["data"]["is_verified"] is True
        assert approve.json()["data"]["source_type"] == SourceType.AASPAS.value

        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert detail.json()["data"]["is_verified"] is True
