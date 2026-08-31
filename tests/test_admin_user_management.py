from tests.helpers import admin_headers, create_shop, register_and_login, register_customer
from tests.test_offer_lifecycle import create_active_shop


class TestAdminUserManagement:
    def _list_users(self, client, headers, **params):
        response = client.get("/api/v1/admin/users", headers=headers, params=params)
        assert response.status_code == 200
        return response.json()["data"]

    def test_admin_can_list_users(self, client, db_session):
        customer = register_customer(client, "admin-users-customer@example.com")
        owner, shop_id, admin = create_active_shop(
            client,
            "admin-users-owner@example.com",
            "admin-users-admin@example.com",
            db_session,
        )

        users = self._list_users(client, admin)
        emails = {item["email"] for item in users}
        assert "admin-users-customer@example.com" in emails
        assert "admin-users-owner@example.com" in emails

        owner_item = next(item for item in users if item["email"] == "admin-users-owner@example.com")
        assert owner_item["role"] == "shop_owner"
        assert owner_item["shop_id"] == shop_id
        assert owner_item["shop_name"] == "Fresh Mart"

    def test_admin_user_search(self, client, db_session):
        register_customer(client, "admin-users-search@example.com")
        admin = admin_headers(client, db_session, "admin-users-search-admin@example.com")

        users = self._list_users(client, admin, search="search@example.com")
        assert any(user["email"] == "admin-users-search@example.com" for user in users)

    def test_admin_user_role_filter(self, client, db_session):
        register_customer(client, "admin-users-role-customer@example.com")
        admin = admin_headers(client, db_session, "admin-users-role-admin@example.com")

        customers = self._list_users(client, admin, role="customer")
        assert all(user["role"] == "customer" for user in customers)

    def test_customer_denied_user_management(self, client):
        customer = register_customer(client, "admin-users-deny-customer@example.com")
        response = client.get("/api/v1/admin/users", headers=customer)
        assert response.status_code == 403

    def test_shop_owner_denied_user_management(self, client):
        owner = register_and_login(client, "admin-users-deny-owner@example.com")
        create_shop(client, owner)
        response = client.get("/api/v1/admin/users", headers=owner)
        assert response.status_code == 403
