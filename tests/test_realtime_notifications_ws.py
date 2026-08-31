"""Tests for real-time notification WebSocket delivery."""

import queue
import threading
import time

import pytest
from starlette.websockets import WebSocketDisconnect

from aaspas.modules.notification.types import NotificationType
from tests.helpers import admin_headers, create_shop, register_and_login, submit_shop
from tests.test_offer_lifecycle import create_active_shop, offer_payload


def _owner_token(headers: dict) -> str:
    return headers["Authorization"].replace("Bearer ", "")


def _receive_json_with_timeout(ws, timeout_seconds: float = 2.0):
    result_queue: queue.Queue = queue.Queue()

    def _reader() -> None:
        try:
            result_queue.put(("ok", ws.receive_json()))
        except Exception as exc:
            result_queue.put(("err", exc))

    thread = threading.Thread(target=_reader, daemon=True)
    thread.start()
    thread.join(timeout=timeout_seconds)
    if thread.is_alive():
        raise TimeoutError("Timed out waiting for websocket message")
    kind, payload = result_queue.get_nowait()
    if kind == "err":
        raise payload
    return payload


class TestRealtimeNotificationWebSocket:
    def test_unauthenticated_websocket_rejected(self, client):
        with pytest.raises(WebSocketDisconnect):
            with client.websocket_connect("/api/v1/ws/notifications"):
                pass

    def test_invalid_token_rejected(self, client):
        with pytest.raises(WebSocketDisconnect):
            with client.websocket_connect("/api/v1/ws/notifications?token=not-a-valid-token"):
                pass

    def test_authenticated_websocket_connects(self, client, db_session):
        owner = register_and_login(client, "ws-connect@example.com")
        token = _owner_token(owner)
        with client.websocket_connect(f"/api/v1/ws/notifications?token={token}") as _ws:
            pass

    def test_shop_approval_delivers_realtime_notification(self, client, db_session):
        owner = register_and_login(client, "ws-shop-approve@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "ws-admin-shop@example.com")
        token = _owner_token(owner)

        with client.websocket_connect(f"/api/v1/ws/notifications?token={token}") as ws:
            response = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
            assert response.status_code == 200
            message = _receive_json_with_timeout(ws)
            assert message["type"] == "notification"
            assert message["unread_count"] >= 1
            notification = message["notification"]
            assert notification["type"] == NotificationType.SHOP_APPROVED.value
            assert notification["entity_id"] == shop_id
            assert notification["is_read"] is False

    def test_offer_rejection_delivers_realtime_notification(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client,
            "ws-offer-reject@example.com",
            "ws-admin-offer@example.com",
            db_session,
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        token = _owner_token(owner)

        with client.websocket_connect(f"/api/v1/ws/notifications?token={token}") as ws:
            response = client.post(
                f"/api/v1/admin/offers/{offer_id}/reject",
                json={"reason": "Bad photo"},
                headers=admin,
            )
            assert response.status_code == 200
            message = _receive_json_with_timeout(ws)
            assert message["notification"]["type"] == NotificationType.OFFER_REJECTED.value
            assert message["notification"]["entity_id"] == offer_id

    def test_recipient_isolation(self, client, db_session):
        owner_a = register_and_login(client, "ws-owner-a@example.com")
        owner_b = register_and_login(client, "ws-owner-b@example.com")
        shop_id = create_shop(client, owner_a).json()["data"]["id"]
        submit_shop(client, shop_id, owner_a)
        admin = admin_headers(client, db_session, "ws-admin-isolation@example.com")
        token_b = _owner_token(owner_b)

        with client.websocket_connect(f"/api/v1/ws/notifications?token={token_b}") as ws:
            client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
            with pytest.raises(TimeoutError):
                _receive_json_with_timeout(ws, timeout_seconds=1.0)

    def test_disconnected_recipient_still_has_notification_in_db(self, client, db_session):
        owner = register_and_login(client, "ws-offline@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "ws-admin-offline@example.com")

        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        time.sleep(0.2)
        data = client.get("/api/v1/notifications", headers=owner).json()["data"]
        types = [item["type"] for item in data["notifications"]]
        assert NotificationType.SHOP_APPROVED.value in types

    def test_duplicate_event_does_not_double_push(self, client, db_session):
        owner = register_and_login(client, "ws-dedupe@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "ws-admin-dedupe@example.com")
        token = _owner_token(owner)

        with client.websocket_connect(f"/api/v1/ws/notifications?token={token}") as ws:
            first = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
            assert first.status_code == 200
            _receive_json_with_timeout(ws)
            second = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
            assert second.status_code in {200, 400, 409}
            with pytest.raises(TimeoutError):
                _receive_json_with_timeout(ws, timeout_seconds=1.0)

        data = client.get("/api/v1/notifications", headers=owner).json()["data"]
        approved = [
            item for item in data["notifications"]
            if item["type"] == NotificationType.SHOP_APPROVED.value
        ]
        assert len(approved) == 1
