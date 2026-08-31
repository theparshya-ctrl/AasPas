from aaspas.common.audit import AuditLog
from aaspas.modules.shop.status import ShopStatus
from tests.helpers import (
    SHOP_ONBOARDING_PAYLOAD,
    admin_headers,
    create_shop,
    register_and_login,
    submit_shop,
)


class TestShopOnboarding:
    def test_create_shop_draft_with_required_fields(self, client):
        headers = register_and_login(client, "owner1@example.com")
        response = create_shop(client, headers)
        assert response.status_code == 201
        data = response.json()["data"]
        assert data["status"] == ShopStatus.DRAFT.value
        assert data["name"] == "Fresh Mart"
        assert data["category"] == "grocery"
        assert data["contact_number"] == "+919876543210"
        assert data["primary_location"]["city"] == "Mumbai"
        assert data["primary_location"]["latitude"] is not None
        assert data["slug"] == "fresh-mart"

    def test_create_shop_validation_missing_contact(self, client):
        headers = register_and_login(client, "owner2@example.com")
        payload = {**SHOP_ONBOARDING_PAYLOAD, "contact_number": "123"}
        response = create_shop(client, headers, payload)
        assert response.status_code == 422

    def test_create_shop_validation_invalid_gps(self, client):
        headers = register_and_login(client, "owner3@example.com")
        payload = {
            **SHOP_ONBOARDING_PAYLOAD,
            "address": {**SHOP_ONBOARDING_PAYLOAD["address"], "latitude": "999"},
        }
        response = create_shop(client, headers, payload)
        assert response.status_code == 422

    def test_submit_moves_to_pending_approval(self, client):
        headers = register_and_login(client, "owner4@example.com")
        shop_id = create_shop(client, headers).json()["data"]["id"]
        response = submit_shop(client, shop_id, headers)
        assert response.status_code == 200
        assert response.json()["data"]["status"] == ShopStatus.PENDING_APPROVAL.value
        assert response.json()["data"]["submitted_at"] is not None

    def test_owner_can_only_access_own_shop(self, client):
        owner_headers = register_and_login(client, "owner5@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]

        other_headers = register_and_login(client, "other5@example.com")
        response = client.get(f"/api/v1/shops/{shop_id}", headers=other_headers)
        assert response.status_code == 403

    def test_admin_approve_activates_shop(self, client, db_session):
        owner_headers = register_and_login(client, "owner6@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        headers = admin_headers(client, db_session, "admin6@example.com")

        pending = client.get("/api/v1/admin/shops/pending", headers=headers)
        assert pending.status_code == 200
        assert len(pending.json()["data"]) == 1

        approve = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=headers)
        assert approve.status_code == 200
        assert approve.json()["data"]["status"] == ShopStatus.ACTIVE.value
        assert approve.json()["data"]["approved_at"] is not None
        assert approve.json()["data"]["rejection_reason"] is None

    def test_admin_reject_stores_reason(self, client, db_session):
        owner_headers = register_and_login(client, "owner7@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        headers = admin_headers(client, db_session, "admin7@example.com")
        reject = client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Incomplete address documentation"},
            headers=headers,
        )
        assert reject.status_code == 200
        assert reject.json()["data"]["status"] == ShopStatus.REJECTED.value
        assert reject.json()["data"]["rejection_reason"] == "Incomplete address documentation"

    def test_resubmit_after_rejection(self, client, db_session):
        owner_headers = register_and_login(client, "owner8@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        admin = admin_headers(client, db_session, "admin8@example.com")
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Fix contact number"},
            headers=admin,
        )

        update = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"contact_number": "+919999999999"},
            headers=owner_headers,
        )
        assert update.status_code == 200

        resubmit = submit_shop(client, shop_id, owner_headers)
        assert resubmit.status_code == 200
        assert resubmit.json()["data"]["status"] == ShopStatus.PENDING_APPROVAL.value
        assert resubmit.json()["data"]["rejection_reason"] is None

    def test_cannot_edit_pending_shop(self, client):
        owner_headers = register_and_login(client, "owner9@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        update = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"name": "Changed Name"},
            headers=owner_headers,
        )
        assert update.status_code == 422

    def test_unauthorized_admin_endpoints(self, client):
        owner_headers = register_and_login(client, "owner10@example.com")
        response = client.get("/api/v1/admin/shops/pending", headers=owner_headers)
        assert response.status_code == 403

    def test_unauthenticated_admin_endpoints(self, client):
        response = client.get("/api/v1/admin/shops/pending")
        assert response.status_code == 401

    def test_audit_log_on_submit(self, client, db_session):
        owner_headers = register_and_login(client, "owner11@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        logs = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == shop_id, AuditLog.module == "shop")
            .all()
        )
        actions = {log.action for log in logs}
        assert "create" in actions
        assert "update" in actions

    def test_events_published_on_lifecycle(self, client, db_session):
        events: list[str] = []

        def capture(event):
            events.append(event.event_type)

        from aaspas.common.events import event_bus

        event_bus.subscribe("shop.submitted_for_approval", capture)
        event_bus.subscribe("shop.approved", capture)
        event_bus.subscribe("shop.activated", capture)

        owner_headers = register_and_login(client, "owner12@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        admin = admin_headers(client, db_session, "admin12@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        assert "shop.submitted_for_approval" in events
        assert "shop.approved" in events
        assert "shop.activated" in events

    def test_owner_cannot_approve_own_shop(self, client):
        owner_headers = register_and_login(client, "owner13@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]
        submit_shop(client, shop_id, owner_headers)

        response = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=owner_headers)
        assert response.status_code == 403

    def test_cannot_approve_non_pending_shop(self, client, db_session):
        owner_headers = register_and_login(client, "owner14@example.com")
        shop_id = create_shop(client, owner_headers).json()["data"]["id"]

        headers = admin_headers(client, db_session, "admin14@example.com")
        response = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=headers)
        assert response.status_code == 422
