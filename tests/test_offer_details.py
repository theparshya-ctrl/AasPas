"""Tests for public Offer Details API."""

import uuid

from tests.pilot_fixtures import NEAR_LAT, NEAR_LON, pilot_data  # noqa: F401


def _offer_id(pilot_data, title: str) -> str:
    offer = next(o for o in pilot_data["offers"] if o.title == title)
    return str(offer.id)


class TestOfferDetailsAPI:
    def test_active_offer_accessible(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Today Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["offer_id"] == offer_id
        assert data["title"] == "Today Offer"
        assert data["status"] == "active"
        assert data["shop"]["shop_name"] == "Pimpri Fashion"
        assert data["shop"]["category"] == "Clothing / Fashion"

    def test_coming_soon_offer_accessible(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Coming Soon Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 200
        assert response.json()["data"]["status"] == "coming_soon"

    def test_draft_offer_hidden(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Draft Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 404

    def test_pending_offer_hidden(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Pending Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 404

    def test_expired_offer_hidden(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Expired Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 404

    def test_inactive_shop_offer_hidden(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Inactive Shop Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 404

    def test_invalid_offer_id(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get(f"/api/v1/offers/{uuid.uuid4()}")
        assert response.status_code == 404

    def test_customer_safe_fields_only(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Today Offer")
        data = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        forbidden = {
            "owner_id",
            "created_at",
            "updated_at",
            "photo_storage_key",
            "rejection_reason",
            "email",
        }
        assert forbidden.isdisjoint(data.keys())
        assert forbidden.isdisjoint(data["shop"].keys())

    def test_distance_when_coordinates_provided(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Today Offer")
        response = fixed_now_client.get(
            f"/api/v1/offers/{offer_id}?latitude={NEAR_LAT}&longitude={NEAR_LON}"
        )
        assert response.status_code == 200
        distance = response.json()["data"]["shop"]["distance_km"]
        assert distance is not None
        assert distance < 5

    def test_public_no_auth_required(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Today Offer")
        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 200
