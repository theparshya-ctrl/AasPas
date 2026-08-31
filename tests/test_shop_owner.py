from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.status import ShopStatus
from tests.helpers import (
    SHOP_ONBOARDING_PAYLOAD,
    admin_headers,
    create_shop,
    register_and_login,
    submit_shop,
)


def create_offer(client, shop_id: str, headers: dict, title: str = "Test Offer"):
    start = "2026-08-21T10:00:00+00:00"
    end = "2026-08-28T18:00:00+00:00"
    return client.post(
        "/api/v1/offers",
        json={
            "shop_id": shop_id,
            "title": title,
            "description": "Valid test offer description.",
            "discount_type": "percentage",
            "discount_value": "10",
            "starts_at": start,
            "ends_at": end,
        },
        headers=headers,
    )


class TestShopOwnerDashboard:
    def test_customer_cannot_access_owner_dashboard(self, client):
        headers = register_and_login(client, "cust-dash@example.com", role="customer")
        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 403

    def test_owner_dashboard_without_shop_returns_not_found(self, client):
        headers = register_and_login(client, "owner-no-shop@example.com")
        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 404

    def test_owner_dashboard_returns_shop_profile(self, client):
        headers = register_and_login(client, "owner-dash@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]

        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["shop"]["id"] == shop_id
        assert data["shop"]["name"] == "Fresh Mart"
        assert data["shop"]["status"] == ShopStatus.DRAFT.value
        assert data["can_edit_profile"] is True
        assert data["is_verified"] is False
        assert data["shop"]["primary_location"]["city"] == "Mumbai"

    def test_owner_dashboard_pending_approval_message(self, client):
        headers = register_and_login(client, "owner-pending@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]
        submit_shop(client, shop_id, headers)

        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["shop"]["status"] == ShopStatus.PENDING_APPROVAL.value
        assert "Pending verification" in data["status_message"]
        assert data["can_edit_profile"] is False
        assert data["can_submit_offers"] is False

    def test_owner_dashboard_active_shop(self, client, db_session):
        headers = register_and_login(client, "owner-active@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]
        submit_shop(client, shop_id, headers)
        admin = admin_headers(client, db_session, "admin-active@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["shop"]["status"] == ShopStatus.ACTIVE.value
        assert data["is_verified"] is True
        assert data["can_edit_profile"] is False
        assert data["can_submit_offers"] is True
        assert "Approved" in data["status_message"]

    def test_owner_dashboard_offer_counts(self, client):
        headers = register_and_login(client, "owner-counts@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]
        create_offer(client, shop_id, headers, title="Draft Offer 1")
        create_offer(client, shop_id, headers, title="Draft Offer 2")

        response = client.get("/api/v1/shops/me/dashboard", headers=headers)
        assert response.status_code == 200
        counts = response.json()["data"]["offer_counts"]
        assert counts["draft"] == 2
        assert counts["active"] == 0
        assert counts["expired"] == 0

    def test_owner_cannot_access_other_shop(self, client):
        owner_headers = register_and_login(client, "owner-own@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]

        other_headers = register_and_login(client, "owner-other@example.com")
        response = client.get(f"/api/v1/shops/{shop_id}", headers=other_headers)
        assert response.status_code == 403

    def test_owner_can_list_own_offers(self, client):
        headers = register_and_login(client, "owner-offers@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]
        create_offer(client, shop_id, headers)

        response = client.get(f"/api/v1/offers/shop/{shop_id}", headers=headers)
        assert response.status_code == 200
        assert len(response.json()["data"]) == 1
        assert response.json()["data"][0]["status"] == OfferStatus.DRAFT.value

    def test_owner_cannot_list_other_shop_offers(self, client):
        owner_headers = register_and_login(client, "owner-offers2@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]

        other_headers = register_and_login(client, "owner-offers3@example.com")
        response = client.get(f"/api/v1/offers/shop/{shop_id}", headers=other_headers)
        assert response.status_code == 403

    def test_role_separation_customer_vs_owner(self, client):
        customer_headers = register_and_login(client, "role-cust@example.com", role="customer")
        owner_headers = register_and_login(client, "role-owner@example.com")

        assert client.get("/api/v1/shops/me", headers=customer_headers).status_code == 403
        assert client.get("/api/v1/shops/me", headers=owner_headers).status_code == 200
