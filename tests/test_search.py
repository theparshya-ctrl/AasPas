"""Tests for customer search API."""

import uuid

from tests.pilot_fixtures import NEAR_LAT, NEAR_LON, PIMPRI_LAT, PIMPRI_LON, pilot_data  # noqa: F401


def _offer_id(pilot_data, title: str) -> str:
    offer = next(o for o in pilot_data["offers"] if o.title == title)
    return str(offer.id)


class TestSearchAPI:
    def test_search_by_offer_title(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Today")
        assert response.status_code == 200
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Today Offer" in titles

    def test_search_by_shop_name(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Pimpri")
        assert response.status_code == 200
        offers = response.json()["data"]["offers"]
        assert len(offers) >= 1
        assert all(o["shop_name"] == "Pimpri Fashion" for o in offers)

    def test_search_by_category_id(self, fixed_now_client, pilot_data):
        category_id = str(pilot_data["category"].id)
        response = fixed_now_client.get(f"/api/v1/search?category_id={category_id}")
        assert response.status_code == 200
        offers = response.json()["data"]["offers"]
        assert len(offers) >= 2
        assert all(o["category"] == "Clothing / Fashion" for o in offers)

    def test_active_offer_returned(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Today")
        offer = response.json()["data"]["offers"][0]
        assert offer["status"] == "active"

    def test_draft_excluded(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Draft")
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Draft Offer" not in titles

    def test_pending_excluded(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Pending")
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Pending Offer" not in titles

    def test_expired_excluded(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Expired")
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Expired Offer" not in titles

    def test_inactive_shop_excluded(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Inactive")
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Inactive Shop Offer" not in titles

    def test_coming_soon_has_status(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Coming")
        offers = response.json()["data"]["offers"]
        assert any(o["title"] == "Coming Soon Offer" and o["status"] == "coming_soon" for o in offers)

    def test_location_sorting_and_distance(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get(
            f"/api/v1/search?latitude={NEAR_LAT}&longitude={NEAR_LON}&radius_km=5"
        )
        assert response.status_code == 200
        offers = response.json()["data"]["offers"]
        assert len(offers) >= 1
        assert offers[0]["distance_km"] is not None
        assert offers[0]["distance_km"] < 5

    def test_radius_filtering(self, fixed_now_client, pilot_data, db_session):
        from decimal import Decimal

        from aaspas.modules.auth.models import User
        from aaspas.modules.location.models import Location
        from aaspas.modules.shop.models import Shop
        from aaspas.modules.shop.status import ShopStatus
        from tests.pilot_fixtures import FAR_LAT, FAR_LON, FIXED_NOW
        from datetime import timedelta
        from aaspas.modules.offer.models import Offer
        from aaspas.modules.offer.status import OfferStatus

        far_owner = User(email="search-far@test.com", password_hash="hash", role="shop_owner")
        db_session.add(far_owner)
        db_session.flush()
        far_shop = Shop(
            owner_id=far_owner.id,
            name="Far Fashion",
            slug="far-fashion-search",
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
        db_session.add(
            Offer(
                shop_id=far_shop.id,
                title="Far Shirt Offer",
                discount_type="percentage",
                discount_value=Decimal("10"),
                status=OfferStatus.ACTIVE.value,
                starts_at=FIXED_NOW - timedelta(hours=1),
                ends_at=FIXED_NOW + timedelta(days=2),
            )
        )
        db_session.commit()

        response = fixed_now_client.get(
            f"/api/v1/search?q=Shirt&latitude={NEAR_LAT}&longitude={NEAR_LON}&radius_km=2"
        )
        titles = {o["title"] for o in response.json()["data"]["offers"]}
        assert "Far Shirt Offer" not in titles

    def test_pagination(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?page=1&page_size=1")
        assert response.status_code == 200
        data = response.json()["data"]
        assert len(data["offers"]) <= 1
        assert data["total"] >= 2
        assert data["page"] == 1
        assert data["page_size"] == 1

    def test_empty_search_result(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=zzzznonexistent")
        assert response.status_code == 200
        assert response.json()["data"]["offers"] == []
        assert response.json()["data"]["total"] == 0

    def test_customer_safe_fields_only(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search?q=Today")
        offer = response.json()["data"]["offers"][0]
        forbidden = {"owner_id", "created_at", "updated_at", "email", "shop"}
        assert forbidden.isdisjoint(offer.keys())
        assert offer["status"] in {"active", "coming_soon"}

    def test_public_no_auth_required(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get("/api/v1/search")
        assert response.status_code == 200


class TestCategoryOffersAPI:
    def test_category_offers_filtering(self, fixed_now_client, pilot_data):
        category_id = str(pilot_data["category"].id)
        response = fixed_now_client.get(f"/api/v1/categories/{category_id}/offers")
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["category_name"] == "Clothing / Fashion"
        today_titles = {o["title"] for o in data["today_offers"]}
        soon_titles = {o["title"] for o in data["coming_soon"]}
        assert "Today Offer" in today_titles
        assert "Coming Soon Offer" in soon_titles
        assert "Draft Offer" not in today_titles | soon_titles

    def test_invalid_category_id(self, fixed_now_client, pilot_data):
        response = fixed_now_client.get(f"/api/v1/categories/{uuid.uuid4()}/offers")
        assert response.status_code == 404

    def test_category_location_sorting(self, fixed_now_client, pilot_data):
        category_id = str(pilot_data["category"].id)
        response = fixed_now_client.get(
            f"/api/v1/categories/{category_id}/offers"
            f"?latitude={NEAR_LAT}&longitude={NEAR_LON}&radius_km=5"
        )
        assert response.status_code == 200
        offers = response.json()["data"]["today_offers"]
        if offers:
            assert offers[0]["distance_km"] is not None

    def test_category_pagination(self, fixed_now_client, pilot_data):
        category_id = str(pilot_data["category"].id)
        response = fixed_now_client.get(
            f"/api/v1/categories/{category_id}/offers?page=1&page_size=1"
        )
        data = response.json()["data"]
        assert len(data["today_offers"]) <= 1
        assert data["total_active"] >= 1
