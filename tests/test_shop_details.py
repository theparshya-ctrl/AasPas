"""Tests for public Shop Details API."""

import uuid

from tests.pilot_fixtures import NEAR_LAT, NEAR_LON, pilot_data  # noqa: F401


class TestShopDetailsAPI:
    def test_active_shop_accessible(self, fixed_now_client, pilot_data):
        shop_id = str(pilot_data["active_shop"].id)
        response = fixed_now_client.get(f"/api/v1/shops/{shop_id}")
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["shop_id"] == shop_id
        assert data["shop_name"] == "Pimpri Fashion"
        assert data["city"] == "Pimpri-Chinchwad"
        assert data["phone"] == "+919876543210"
        assert data["is_verified"] is True
        assert data["active_offer_count"] == 1
        today_titles = {o["title"] for o in data["today_offers"]}
        soon_titles = {o["title"] for o in data["coming_soon"]}
        assert "Today Offer" in today_titles
        assert "Coming Soon Offer" in soon_titles

    def test_inactive_shop_hidden(self, fixed_now_client, pilot_data):
        shop_id = str(pilot_data["inactive_shop"].id)
        response = fixed_now_client.get(f"/api/v1/shops/{shop_id}")
        assert response.status_code == 404

    def test_invalid_shop_id(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get(f"/api/v1/shops/{uuid.uuid4()}")
        assert response.status_code == 404

    def test_customer_safe_fields_only(self, fixed_now_client, pilot_data):
        shop_id = str(pilot_data["active_shop"].id)
        data = fixed_now_client.get(f"/api/v1/shops/{shop_id}").json()["data"]
        forbidden = {
            "owner_id",
            "slug",
            "photo_storage_key",
            "rejection_reason",
            "submitted_at",
            "approved_at",
            "created_at",
            "updated_at",
            "status",
        }
        assert forbidden.isdisjoint(data.keys())

    def test_distance_when_coordinates_provided(self, fixed_now_client, pilot_data):
        shop_id = str(pilot_data["active_shop"].id)
        response = fixed_now_client.get(
            f"/api/v1/shops/{shop_id}?latitude={NEAR_LAT}&longitude={NEAR_LON}"
        )
        assert response.status_code == 200
        distance = response.json()["data"]["distance_km"]
        assert distance is not None
        assert distance < 5

    def test_public_no_auth_required(self, fixed_now_client, pilot_data):
        shop_id = str(pilot_data["active_shop"].id)
        response = fixed_now_client.get(f"/api/v1/shops/{shop_id}")
        assert response.status_code == 200
