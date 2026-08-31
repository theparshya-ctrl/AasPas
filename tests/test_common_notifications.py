from aaspas.modules.notification.types import NotificationAudience, NotificationType
from tests.helpers import (
    admin_headers,
    create_shop,
    register_and_login,
    register_customer,
    submit_shop,
)


class TestCommonNotifications:
    def _list(self, client, headers):
        response = client.get("/api/v1/notifications/me", headers=headers)
        assert response.status_code == 200
        return response.json()["data"]

    def test_business_notifications_include_audience(self, client, db_session):
        owner = register_and_login(client, "common-notif-owner@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "common-notif-admin@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        data = self._list(client, owner)
        approved = next(
            item for item in data["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value
        )
        assert approved["audience"] == NotificationAudience.BUSINESS.value
        assert data["unread_count"] >= 1

    def test_customer_does_not_see_business_notifications(self, client, db_session):
        owner = register_and_login(client, "common-notif-iso-owner@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "common-notif-iso-admin@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        customer = register_customer(client, "common-notif-customer@example.com")
        owner_data = self._list(client, owner)
        customer_data = self._list(client, customer)

        assert owner_data["unread_count"] >= 1
        assert customer_data["unread_count"] == 0
        assert customer_data["notifications"] == []
