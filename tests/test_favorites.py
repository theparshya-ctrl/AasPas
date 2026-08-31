"""Tests for customer favorites API."""

import uuid

from tests.helpers import register_customer
from tests.pilot_fixtures import pilot_data  # noqa: F401


def _offer_id(pilot_data, title: str) -> str:
    offer = next(o for o in pilot_data["offers"] if o.title == title)
    return str(offer.id)


class TestFavoritesAPI:
    def test_customer_can_save_offer(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer1@test.com")
        offer_id = _offer_id(pilot_data, "Today Offer")
        response = fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        assert response.status_code == 201
        assert response.json()["data"]["saved"] is True

    def test_customer_can_unsave_offer(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer2@test.com")
        offer_id = _offer_id(pilot_data, "Today Offer")
        fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        response = fixed_now_client.delete(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        assert response.status_code == 200
        assert response.json()["data"]["saved"] is False

    def test_duplicate_save_is_idempotent(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer3@test.com")
        offer_id = _offer_id(pilot_data, "Today Offer")
        first = fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        second = fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        assert first.status_code == 201
        assert second.status_code == 201
        assert first.json()["data"]["favorite_id"] == second.json()["data"]["favorite_id"]

    def test_customer_can_save_shop(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer4@test.com")
        shop_id = str(pilot_data["active_shop"].id)
        response = fixed_now_client.post(f"/api/v1/favorites/shops/{shop_id}", headers=headers)
        assert response.status_code == 201
        assert response.json()["data"]["saved"] is True

    def test_customer_can_unsave_shop(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer5@test.com")
        shop_id = str(pilot_data["active_shop"].id)
        fixed_now_client.post(f"/api/v1/favorites/shops/{shop_id}", headers=headers)
        response = fixed_now_client.delete(f"/api/v1/favorites/shops/{shop_id}", headers=headers)
        assert response.status_code == 200

    def test_favorites_only_visible_to_owner(self, fixed_now_client, pilot_data):
        owner_headers = register_customer(fixed_now_client, "fav-owner6@test.com")
        other_headers = register_customer(fixed_now_client, "fav-other6@test.com")
        offer_id = _offer_id(pilot_data, "Today Offer")
        fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=owner_headers)

        owner_list = fixed_now_client.get("/api/v1/favorites", headers=owner_headers)
        other_list = fixed_now_client.get("/api/v1/favorites", headers=other_headers)
        assert len(owner_list.json()["data"]["offers"]) == 1
        assert len(other_list.json()["data"]["offers"]) == 0

    def test_unauthenticated_access_rejected(self, fixed_now_client, pilot_data):
        offer_id = _offer_id(pilot_data, "Today Offer")
        shop_id = str(pilot_data["active_shop"].id)
        assert fixed_now_client.get("/api/v1/favorites").status_code == 401
        assert fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}").status_code == 401
        assert fixed_now_client.post(f"/api/v1/favorites/shops/{shop_id}").status_code == 401

    def test_draft_offer_cannot_be_saved(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer7@test.com")
        offer_id = _offer_id(pilot_data, "Draft Offer")
        response = fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        assert response.status_code == 422

    def test_expired_offer_cannot_be_newly_saved(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer8@test.com")
        offer_id = _offer_id(pilot_data, "Expired Offer")
        response = fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)
        assert response.status_code == 422

    def test_inactive_shop_cannot_be_saved(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer9@test.com")
        shop_id = str(pilot_data["inactive_shop"].id)
        response = fixed_now_client.post(f"/api/v1/favorites/shops/{shop_id}", headers=headers)
        assert response.status_code == 422

    def test_invalid_offer_id(self, fixed_now_client):
        headers = register_customer(fixed_now_client, "fav-customer10@test.com")
        response = fixed_now_client.post(f"/api/v1/favorites/offers/{uuid.uuid4()}", headers=headers)
        assert response.status_code == 404

    def test_saved_expired_offer_shows_inactive_in_list(self, fixed_now_client, pilot_data, db_session):
        from datetime import timedelta

        from aaspas.modules.offer.status import OfferStatus

        headers = register_customer(fixed_now_client, "fav-customer11@test.com")
        offer = next(o for o in pilot_data["offers"] if o.title == "Today Offer")
        offer_id = str(offer.id)
        fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)

        offer.status = OfferStatus.EXPIRED.value
        db_session.commit()

        listed = fixed_now_client.get("/api/v1/favorites", headers=headers)
        saved = listed.json()["data"]["offers"][0]
        assert saved["offer_id"] == offer_id
        assert saved["is_active"] is False

    def test_home_is_saved_when_authenticated(self, fixed_now_client, pilot_data):
        headers = register_customer(fixed_now_client, "fav-customer12@test.com")
        offer_id = _offer_id(pilot_data, "Today Offer")
        fixed_now_client.post(f"/api/v1/favorites/offers/{offer_id}", headers=headers)

        response = fixed_now_client.get("/api/v1/home", headers=headers)
        today = response.json()["data"]["today_offers"]
        saved_offer = next(o for o in today if o["offer_id"] == offer_id)
        assert saved_offer["is_saved"] is True

    def test_home_works_without_auth(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/home")
        assert response.status_code == 200
        offer = response.json()["data"]["today_offers"][0]
        assert offer.get("is_saved") is False
