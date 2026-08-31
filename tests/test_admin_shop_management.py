from aaspas.modules.shop.status import ShopStatus
from tests.helpers import (
    admin_headers,
    create_shop,
    register_and_login,
    register_customer,
    submit_shop,
)
from tests.test_offer_lifecycle import create_active_shop, offer_payload


class TestAdminShopManagement:
    def _list_shops(self, client, headers, **params):
        response = client.get("/api/v1/admin/shops", headers=headers, params=params)
        assert response.status_code == 200
        return response.json()["data"]

    def test_admin_can_list_shops(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client,
            "admin-mgmt-owner@example.com",
            "admin-mgmt-admin@example.com",
            db_session,
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )

        shops = self._list_shops(client, admin)
        assert len(shops) >= 1
        item = next(shop for shop in shops if shop["shop_id"] == shop_id)
        assert item["shop_name"] == "Fresh Mart"
        assert item["owner_email"] == "admin-mgmt-owner@example.com"
        assert item["status"] == ShopStatus.ACTIVE.value
        assert item["offer_count"] >= 1

    def test_admin_shop_search_by_name(self, client, db_session):
        _owner, shop_id, admin = create_active_shop(
            client,
            "admin-mgmt-search@example.com",
            "admin-mgmt-search-admin@example.com",
            db_session,
        )
        shops = self._list_shops(client, admin, search="Fresh")
        assert any(shop["shop_id"] == shop_id for shop in shops)

    def test_admin_shop_filter_pending(self, client, db_session):
        owner = register_and_login(client, "admin-mgmt-pending@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "admin-mgmt-pending-admin@example.com")

        pending = self._list_shops(client, admin, status=ShopStatus.PENDING_APPROVAL.value)
        assert any(shop["shop_id"] == shop_id for shop in pending)

        active = self._list_shops(client, admin, status=ShopStatus.ACTIVE.value)
        assert all(shop["status"] == ShopStatus.ACTIVE.value for shop in active)

    def test_admin_can_inspect_shop_detail(self, client, db_session):
        _owner, shop_id, admin = create_active_shop(
            client,
            "admin-mgmt-detail@example.com",
            "admin-mgmt-detail-admin@example.com",
            db_session,
        )

        response = client.get(f"/api/v1/admin/shops/{shop_id}/detail", headers=admin)
        assert response.status_code == 200
        detail = response.json()["data"]
        assert detail["shop_id"] == shop_id
        assert detail["owner"]["email"] == "admin-mgmt-detail@example.com"
        assert "offer_counts_by_status" in detail
        assert "audit_entries" in detail
        assert any(entry["message"] for entry in detail["audit_entries"])

    def test_customer_denied_shop_management(self, client, db_session):
        customer = register_customer(client, "admin-mgmt-customer@example.com")
        response = client.get("/api/v1/admin/shops", headers=customer)
        assert response.status_code == 403

    def test_shop_owner_denied_shop_management(self, client, db_session):
        owner = register_and_login(client, "admin-mgmt-owner-deny@example.com")
        create_shop(client, owner)
        response = client.get("/api/v1/admin/shops", headers=owner)
        assert response.status_code == 403
