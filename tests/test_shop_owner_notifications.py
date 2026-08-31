from aaspas.modules.notification.types import NotificationType
from tests.helpers import (
    admin_headers,
    create_shop,
    register_and_login,
    register_customer,
    submit_shop,
)
from tests.test_offer_lifecycle import create_active_shop, offer_payload


class TestShopOwnerNotifications:
    def _pending_shop(self, client, db_session, owner_email: str, admin_email: str) -> tuple[dict, dict, str]:
        owner = register_and_login(client, owner_email)
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, admin_email)
        return owner, admin, shop_id

    def _list_notifications(self, client, headers):
        response = client.get("/api/v1/notifications", headers=headers)
        assert response.status_code == 200
        return response.json()["data"]

    def test_shop_submission_creates_notification(self, client, db_session):
        owner, _admin, shop_id = self._pending_shop(
            client, db_session, "notif-shop-submit@example.com", "notif-admin-submit@example.com"
        )
        data = self._list_notifications(client, owner)
        assert data["unread_count"] == 1
        notification = data["notifications"][0]
        assert notification["type"] == NotificationType.SHOP_SUBMITTED.value
        assert notification["entity_type"] == "shop"
        assert notification["entity_id"] == shop_id
        assert notification["is_read"] is False
        assert "Fresh Mart" in notification["message"]

    def test_shop_approval_creates_notification(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-shop-approve@example.com", "notif-admin-approve@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        data = self._list_notifications(client, owner)
        types = [item["type"] for item in data["notifications"]]
        assert NotificationType.SHOP_APPROVED.value in types
        approved = next(item for item in data["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value)
        assert approved["entity_id"] == shop_id
        assert approved["is_read"] is False

    def test_shop_rejection_creates_notification_with_reason(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-shop-reject@example.com", "notif-admin-reject@example.com"
        )
        reason = "Address unclear"
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": reason},
            headers=admin,
        )
        data = self._list_notifications(client, owner)
        rejected = next(item for item in data["notifications"] if item["type"] == NotificationType.SHOP_REJECTED.value)
        assert reason in rejected["message"]
        assert rejected["entity_id"] == shop_id

    def test_shop_resubmission_creates_resubmitted_notification(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-shop-resubmit@example.com", "notif-admin-resubmit@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Fix phone number"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"contact_number": "+919999999998"},
            headers=owner,
        )
        submit_shop(client, shop_id, owner)
        data = self._list_notifications(client, owner)
        types = [item["type"] for item in data["notifications"]]
        assert NotificationType.SHOP_RESUBMITTED.value in types

    def test_offer_approval_creates_notification(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client, "notif-offer-approve@example.com", "notif-admin-offer-approve@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        data = self._list_notifications(client, owner)
        approved = next(item for item in data["notifications"] if item["type"] == NotificationType.OFFER_APPROVED.value)
        assert approved["entity_type"] == "offer"
        assert approved["entity_id"] == offer_id

    def test_offer_rejection_creates_notification_with_reason(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client, "notif-offer-reject@example.com", "notif-admin-offer-reject@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id, title="20% Summer Sale"), headers=owner
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        reason = "Discount terms were unclear"
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": reason},
            headers=admin,
        )
        data = self._list_notifications(client, owner)
        rejected = next(item for item in data["notifications"] if item["type"] == NotificationType.OFFER_REJECTED.value)
        assert reason in rejected["message"]
        assert "20% Summer Sale" in rejected["message"]

    def test_offer_resubmission_creates_resubmitted_notification(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client, "notif-offer-resubmit@example.com", "notif-admin-offer-resubmit@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Update terms"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"terms": "Updated terms for customers."},
            headers=owner,
        )
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        data = self._list_notifications(client, owner)
        types = [item["type"] for item in data["notifications"]]
        assert NotificationType.OFFER_RESUBMITTED.value in types

    def test_recipient_ownership_isolation(self, client, db_session):
        owner_a, _admin, _shop_a = self._pending_shop(
            client, db_session, "notif-owner-a@example.com", "notif-admin-iso@example.com"
        )
        owner_b = register_and_login(client, "notif-owner-b@example.com")
        create_shop(client, owner_b)
        data_b = self._list_notifications(client, owner_b)
        assert data_b["unread_count"] == 0
        assert data_b["notifications"] == []

        data_a = self._list_notifications(client, owner_a)
        assert data_a["unread_count"] >= 1

    def test_customer_cannot_see_merchant_notifications(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-owner-customer@example.com", "notif-admin-customer@example.com"
        )
        customer = register_customer(client, "notif-customer@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        owner_data = self._list_notifications(client, owner)
        assert owner_data["unread_count"] >= 1
        customer_data = self._list_notifications(client, customer)
        assert customer_data["unread_count"] == 0
        assert customer_data["notifications"] == []

    def test_mark_read_and_unread_count(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-read@example.com", "notif-admin-read@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        data = self._list_notifications(client, owner)
        assert data["unread_count"] >= 1
        notification_id = data["notifications"][0]["id"]
        mark = client.post(f"/api/v1/notifications/{notification_id}/read", headers=owner)
        assert mark.status_code == 200
        assert mark.json()["data"]["is_read"] is True
        updated = self._list_notifications(client, owner)
        assert updated["unread_count"] < data["unread_count"]
        unread_only = client.get("/api/v1/notifications?unread_only=true", headers=owner).json()["data"]
        assert notification_id not in {item["id"] for item in unread_only["notifications"]}

    def test_mark_all_read(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-read-all@example.com", "notif-admin-read-all@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        assert self._list_notifications(client, owner)["unread_count"] >= 1
        response = client.post("/api/v1/notifications/read-all", headers=owner)
        assert response.status_code == 200
        assert self._list_notifications(client, owner)["unread_count"] == 0

    def test_cannot_mark_another_users_notification(self, client, db_session):
        owner_a, admin, shop_id = self._pending_shop(
            client, db_session, "notif-mark-a@example.com", "notif-admin-mark@example.com"
        )
        owner_b = register_and_login(client, "notif-mark-b@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        notification_id = self._list_notifications(client, owner_a)["notifications"][0]["id"]
        response = client.post(f"/api/v1/notifications/{notification_id}/read", headers=owner_b)
        assert response.status_code == 404

    def test_duplicate_event_is_idempotent(self, client, db_session):
        import uuid as uuid_lib

        from aaspas.common.events import DomainEvent
        from aaspas.modules.notification.service import NotificationService
        from aaspas.modules.shop.events import SHOP_APPROVED
        from aaspas.modules.shop.models import Shop

        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-dedupe@example.com", "notif-admin-dedupe@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        before = self._list_notifications(client, owner)
        approved_count = sum(
            1 for item in before["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value
        )
        assert approved_count == 1

        shop = db_session.get(Shop, uuid_lib.UUID(shop_id))
        event = DomainEvent(
            event_id="fixed-dedupe-event-id",
            event_type=SHOP_APPROVED,
            source_module="test",
            payload={"shop_id": shop_id, "owner_id": str(shop.owner_id)},
        )
        service = NotificationService(db_session)
        service.handle_event(event)
        mid = self._list_notifications(client, owner)
        approved_mid = sum(
            1 for item in mid["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value
        )
        assert approved_mid == approved_count + 1
        service.handle_event(event)
        after = self._list_notifications(client, owner)
        approved_after = sum(
            1 for item in after["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value
        )
        assert approved_after == approved_mid

    def test_shop_approve_does_not_duplicate_for_activated_event(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "notif-no-dup@example.com", "notif-admin-no-dup@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        data = self._list_notifications(client, owner)
        approved = [item for item in data["notifications"] if item["type"] == NotificationType.SHOP_APPROVED.value]
        assert len(approved) == 1
