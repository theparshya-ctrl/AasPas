import uuid

from datetime import UTC, datetime, timedelta

from aaspas.common.events import DomainEvent
from aaspas.modules.favorite.models import Favorite
from aaspas.modules.notification.service import NotificationService
from aaspas.modules.notification.types import NotificationAudience, NotificationType
from aaspas.modules.offer.events import OFFER_APPROVED
from tests.helpers import register_customer
from tests.test_offer_lifecycle import create_active_shop, offer_payload


def _active_offer_payload(shop_id: str, *, title: str = "Summer Sale") -> dict:
    now = datetime.now(UTC)
    return offer_payload(
        shop_id,
        title=title,
        starts_at=(now - timedelta(hours=1)).isoformat(),
        ends_at=(now + timedelta(days=7)).isoformat(),
    )


class TestCustomerNotifications:
    def _list(self, client, headers):
        response = client.get("/api/v1/notifications/me", headers=headers)
        assert response.status_code == 200
        return response.json()["data"]

    def _create_pending_offer(self, client, owner, shop_id, *, title: str = "Summer Sale") -> str:
        offer_id = client.post(
            "/api/v1/offers",
            json=_active_offer_payload(shop_id, title=title),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        return offer_id

    def test_favorite_shop_customer_receives_new_offer_notification(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client,
            "cust-notif-shop-owner@example.com",
            "cust-notif-shop-admin@example.com",
            db_session,
        )
        customer = register_customer(client, "cust-notif-shop-fan@example.com")
        client.post(f"/api/v1/favorites/shops/{shop_id}", headers=customer)

        offer_id = self._create_pending_offer(client, owner, shop_id)
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)

        data = self._list(client, customer)
        notification = next(
            item
            for item in data["notifications"]
            if item["type"] == NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER.value
        )
        assert notification["audience"] == NotificationAudience.CUSTOMER.value
        assert notification["entity_type"] == "offer"
        assert notification["entity_id"] == offer_id
        assert "Fresh Mart" in notification["message"]

    def test_favorite_offer_customer_receives_active_notification_on_approval(
        self, client, db_session
    ):
        owner, shop_id, admin = create_active_shop(
            client,
            "cust-notif-offer-owner@example.com",
            "cust-notif-offer-admin@example.com",
            db_session,
        )
        customer = register_customer(client, "cust-notif-offer-fan@example.com")
        customer_id = uuid.UUID(
            client.get("/api/v1/auth/me", headers=customer).json()["data"]["id"]
        )
        offer_id = self._create_pending_offer(client, owner, shop_id, title="Weekend Deal")
        db_session.add(
            Favorite(user_id=customer_id, offer_id=uuid.UUID(offer_id))
        )
        db_session.commit()

        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)

        data = self._list(client, customer)
        notification = next(
            item
            for item in data["notifications"]
            if item["type"] == NotificationType.CUSTOMER_FAVORITE_OFFER_ACTIVE.value
        )
        assert notification["audience"] == NotificationAudience.CUSTOMER.value
        assert notification["entity_type"] == "offer"
        assert notification["entity_id"] == offer_id
        assert "Weekend Deal" in notification["message"]

    def test_non_favorite_customer_does_not_receive_notification(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client,
            "cust-notif-none-owner@example.com",
            "cust-notif-none-admin@example.com",
            db_session,
        )
        fan = register_customer(client, "cust-notif-fan@example.com")
        other = register_customer(client, "cust-notif-other@example.com")
        client.post(f"/api/v1/favorites/shops/{shop_id}", headers=fan)

        offer_id = self._create_pending_offer(client, owner, shop_id)
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)

        fan_data = self._list(client, fan)
        assert any(
            item["type"] == NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER.value
            for item in fan_data["notifications"]
        )
        other_data = self._list(client, other)
        assert other_data["notifications"] == []
        assert other_data["unread_count"] == 0

    def test_pending_offer_does_not_notify_customer(self, client, db_session):
        owner, shop_id, _admin = create_active_shop(
            client,
            "cust-notif-pending-owner@example.com",
            "cust-notif-pending-admin@example.com",
            db_session,
        )
        customer = register_customer(client, "cust-notif-pending-fan@example.com")
        client.post(f"/api/v1/favorites/shops/{shop_id}", headers=customer)
        self._create_pending_offer(client, owner, shop_id)

        data = self._list(client, customer)
        assert data["notifications"] == []
        assert data["unread_count"] == 0

    def test_rejected_offer_does_not_notify_customer(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client,
            "cust-notif-reject-owner@example.com",
            "cust-notif-reject-admin@example.com",
            db_session,
        )
        customer = register_customer(client, "cust-notif-reject-fan@example.com")
        client.post(f"/api/v1/favorites/shops/{shop_id}", headers=customer)
        offer_id = self._create_pending_offer(client, owner, shop_id)
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Not valid"},
            headers=admin,
        )

        data = self._list(client, customer)
        assert data["notifications"] == []
        assert data["unread_count"] == 0

    def test_duplicate_approval_event_does_not_duplicate_customer_notification(
        self, client, db_session
    ):
        owner, shop_id, admin = create_active_shop(
            client,
            "cust-notif-dedupe-owner@example.com",
            "cust-notif-dedupe-admin@example.com",
            db_session,
        )
        customer = register_customer(client, "cust-notif-dedupe-fan@example.com")
        client.post(f"/api/v1/favorites/shops/{shop_id}", headers=customer)
        offer_id = self._create_pending_offer(client, owner, shop_id)
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)

        before = self._list(client, customer)
        shop_offer_count = sum(
            1
            for item in before["notifications"]
            if item["type"] == NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER.value
        )
        assert shop_offer_count == 1

        event = DomainEvent(
            event_id="cust-notif-fixed-event-id",
            event_type=OFFER_APPROVED,
            source_module="test",
            payload={"offer_id": offer_id, "shop_id": shop_id},
        )
        service = NotificationService(db_session)
        service.handle_event(event)
        mid = self._list(client, customer)
        mid_count = sum(
            1
            for item in mid["notifications"]
            if item["type"] == NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER.value
        )
        assert mid_count == shop_offer_count + 1

        service.handle_event(event)
        after = self._list(client, customer)
        after_count = sum(
            1
            for item in after["notifications"]
            if item["type"] == NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER.value
        )
        assert after_count == mid_count
