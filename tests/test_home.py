"""Tests for Customer Home backend foundation."""

import uuid
from datetime import timedelta
from decimal import Decimal

from aaspas.modules.auth.models import User
from aaspas.modules.location.models import Location
from aaspas.modules.offer.visibility import CustomerOfferVisibility, resolve_customer_visibility
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus
from tests.pilot_fixtures import (
    FAR_LAT,
    FAR_LON,
    FIXED_NOW,
    NEAR_LAT,
    NEAR_LON,
    fixed_now_client,
    pilot_data,
)
class TestCategory:
    def test_list_active_categories(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/categories")
        assert response.status_code == 200
        data = response.json()["data"]
        assert len(data) == 1
        assert data[0]["slug"] == "clothing-fashion"


class TestOfferVisibility:
    def test_draft_not_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Draft Offer")
        assert resolve_customer_visibility(offer, pilot_data["active_shop"], FIXED_NOW) is None

    def test_pending_not_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Pending Offer")
        assert resolve_customer_visibility(offer, pilot_data["active_shop"], FIXED_NOW) is None

    def test_coming_soon_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Coming Soon Offer")
        assert (
            resolve_customer_visibility(offer, pilot_data["active_shop"], FIXED_NOW)
            == CustomerOfferVisibility.COMING_SOON
        )

    def test_today_active_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Today Offer")
        assert (
            resolve_customer_visibility(offer, pilot_data["active_shop"], FIXED_NOW)
            == CustomerOfferVisibility.ACTIVE
        )

    def test_expired_not_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Expired Offer")
        assert resolve_customer_visibility(offer, pilot_data["active_shop"], FIXED_NOW) is None

    def test_inactive_shop_offer_not_visible(self, pilot_data):
        offer = next(o for o in pilot_data["offers"] if o.title == "Inactive Shop Offer")
        assert resolve_customer_visibility(offer, pilot_data["inactive_shop"], FIXED_NOW) is None


class TestHomeAPI:
    def test_home_without_coordinates(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/home")
        assert response.status_code == 200
        body = response.json()
        assert body["success"] is True
        assert body["data"]["location"]["available"] is False
        assert len(body["data"]["categories"]) == 1
        assert len(body["data"]["nearby_shops"]) == 0

        today_titles = {o["title"] for o in body["data"]["today_offers"]}
        soon_titles = {o["title"] for o in body["data"]["coming_soon"]}
        assert "Today Offer" in today_titles
        assert "Coming Soon Offer" in soon_titles
        assert "Draft Offer" not in today_titles | soon_titles
        assert "Expired Offer" not in today_titles | soon_titles

    def test_home_with_coordinates_nearby_sorted(self, fixed_now_client, pilot_data, db_session):
        far_owner = User(email="far@test.com", password_hash="hash", role="shop_owner")
        db_session.add(far_owner)
        db_session.flush()
        far_shop = Shop(
            owner_id=far_owner.id,
            name="Far Mumbai Shop",
            slug="far-mumbai",
            category_id=pilot_data["category"].id,
            status=ShopStatus.ACTIVE.value,
        )
        db_session.add(far_shop)
        db_session.flush()
        db_session.add(
            Location(
                shop_id=far_shop.id,
                label="Primary",
                address_line1="Far",
                city="Mumbai",
                country="IN",
                latitude=Decimal(str(FAR_LAT)),
                longitude=Decimal(str(FAR_LON)),
                is_primary=True,
            )
        )
        db_session.commit()

        response = fixed_now_client.get(
            f"/api/v1/home?latitude={NEAR_LAT}&longitude={NEAR_LON}&radius_km=5"
        )
        assert response.status_code == 200
        nearby = response.json()["data"]["nearby_shops"]
        assert len(nearby) == 1
        assert nearby[0]["shop_name"] == "Pimpri Fashion"
        assert nearby[0]["distance_km"] < 5

    def test_far_shop_excluded_by_radius(self, fixed_now_client, pilot_data, db_session):
        far_owner = User(email="far2@test.com", password_hash="hash", role="shop_owner")
        db_session.add(far_owner)
        db_session.flush()
        far_shop = Shop(
            owner_id=far_owner.id,
            name="Far Mumbai Shop",
            slug="far-mumbai-2",
            status=ShopStatus.ACTIVE.value,
        )
        db_session.add(far_shop)
        db_session.flush()
        db_session.add(
            Location(
                shop_id=far_shop.id,
                label="Primary",
                address_line1="Far",
                city="Mumbai",
                country="IN",
                latitude=Decimal(str(FAR_LAT)),
                longitude=Decimal(str(FAR_LON)),
                is_primary=True,
            )
        )
        db_session.commit()

        response = fixed_now_client.get(
            f"/api/v1/home?latitude={NEAR_LAT}&longitude={NEAR_LON}&radius_km=2"
        )
        nearby_names = {s["shop_name"] for s in response.json()["data"]["nearby_shops"]}
        assert "Far Mumbai Shop" not in nearby_names

    def test_home_public_no_auth_required(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/home")
        assert response.status_code == 200

    def test_invalid_latitude_rejected(self, fixed_now_client):
        response = fixed_now_client.get("/api/v1/home?latitude=999&longitude=73.8")
        assert response.status_code == 422

    def test_offer_items_exclude_sensitive_fields(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/home")
        offer = response.json()["data"]["today_offers"][0]
        assert "owner_id" not in offer
        assert "email" not in offer
        assert offer["category"] == "Clothing / Fashion"
