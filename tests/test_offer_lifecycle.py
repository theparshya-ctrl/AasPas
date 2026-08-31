from datetime import UTC, datetime, timedelta

from aaspas.common.audit import AuditLog
from aaspas.modules.offer.status import OfferStatus
from tests.helpers import (
    SHOP_ONBOARDING_PAYLOAD,
    admin_headers,
    create_shop,
    register_and_login,
    submit_shop,
)

FIXED_NOW = datetime(2026, 8, 19, 12, 0, 0, tzinfo=UTC)


def offer_payload(
    shop_id: str,
    *,
    title: str = "Summer Sale",
    starts_at: str | None = None,
    ends_at: str | None = None,
) -> dict:
    start = starts_at or (FIXED_NOW + timedelta(days=1)).isoformat()
    end = ends_at or (FIXED_NOW + timedelta(days=7)).isoformat()
    return {
        "shop_id": shop_id,
        "title": title,
        "description": "Valid offer description for customers.",
        "discount_type": "percentage",
        "discount_value": "15",
        "starts_at": start,
        "ends_at": end,
        "applicable_products": "Shirts, Jeans",
        "terms": "One per customer.",
    }


def create_active_shop(client, owner_email: str, admin_email: str, db_session) -> tuple[dict, str, dict]:
    headers = register_and_login(client, owner_email)
    shop_id = create_shop(client, headers).json()["data"]["id"]
    submit_shop(client, shop_id, headers)
    admin = admin_headers(client, db_session, admin_email)
    client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
    return headers, shop_id, admin


class TestOfferLifecycle:
    def test_create_draft(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-draft@example.com", "admin-offer-draft@example.com", db_session
        )
        response = client.post("/api/v1/offers", json=offer_payload(shop_id), headers=headers)
        assert response.status_code == 201
        data = response.json()["data"]
        assert data["status"] == OfferStatus.DRAFT.value
        assert data["merchant_confirmed_at"] is None

    def test_save_draft_update(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-save@example.com", "admin-offer-save@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        response = client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"title": "Updated Draft Title"},
            headers=headers,
        )
        assert response.status_code == 200
        assert response.json()["data"]["title"] == "Updated Draft Title"

    def test_merchant_confirmation_required(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-confirm@example.com", "admin-offer-confirm@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        response = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": False},
            headers=headers,
        )
        assert response.status_code == 422

    def test_submit_sets_confirmation_and_pending(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-submit@example.com", "admin-offer-submit@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        response = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["status"] == OfferStatus.PENDING_APPROVAL.value
        assert data["merchant_confirmed_at"] is not None
        assert data["submitted_at"] is not None

    def test_customer_cannot_see_pending_offer(self, client, db_session, fixed_now_client):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-hidden@example.com", "admin-offer-hidden@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404

    def test_admin_approve_future_offer_scheduled(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client, "offer-future@example.com", "admin-offer-future@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(
                shop_id,
                starts_at=(FIXED_NOW + timedelta(days=2)).isoformat(),
                ends_at=(FIXED_NOW + timedelta(days=7)).isoformat(),
            ),
            headers=headers,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        approve = client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        assert approve.status_code == 200
        assert approve.json()["data"]["status"] == OfferStatus.SCHEDULED.value
        assert approve.json()["data"]["is_verified"] is True

        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        assert detail["status"] == "coming_soon"

    def test_admin_approve_current_offer_active(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client, "offer-active@example.com", "admin-offer-active@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(
                shop_id,
                starts_at=(FIXED_NOW - timedelta(hours=1)).isoformat(),
                ends_at=(FIXED_NOW + timedelta(days=1)).isoformat(),
            ),
            headers=headers,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        approve = client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        assert approve.status_code == 200
        assert approve.json()["data"]["status"] == OfferStatus.ACTIVE.value

        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        assert detail["status"] == "active"
        assert detail["is_verified"] is True

    def test_expired_offer_hidden(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client, "offer-expired@example.com", "admin-offer-expired@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(
                shop_id,
                starts_at=(FIXED_NOW - timedelta(days=5)).isoformat(),
                ends_at=(FIXED_NOW - timedelta(days=1)).isoformat(),
            ),
            headers=headers,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404

    def test_rejected_offer_hidden(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client, "offer-reject@example.com", "admin-offer-reject@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        reject = client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Discount terms unclear"},
            headers=admin,
        )
        assert reject.status_code == 200
        assert reject.json()["data"]["status"] == OfferStatus.REJECTED.value
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404

    def test_edit_after_confirmation_invalidates_confirmation(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-edit@example.com", "admin-offer-edit@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        # pending cannot edit
        blocked = client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"title": "Changed"},
            headers=headers,
        )
        assert blocked.status_code == 422

    def test_resubmit_after_rejection(self, client, db_session):
        headers, shop_id, admin = create_active_shop(
            client, "offer-resubmit@example.com", "admin-offer-resubmit@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Fix discount"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"description": "Updated valid offer description."},
            headers=headers,
        )
        resubmit = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        assert resubmit.status_code == 200
        assert resubmit.json()["data"]["status"] == OfferStatus.PENDING_APPROVAL.value

    def test_other_shop_ownership_denied(self, client, db_session):
        owner_headers, shop_id, admin = create_active_shop(
            client, "offer-own@example.com", "admin-offer-own@example.com", db_session
        )
        other_headers = register_and_login(client, "offer-other@example.com")
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner_headers
        ).json()["data"]["id"]
        assert client.get(f"/api/v1/offers/manage/{offer_id}", headers=other_headers).status_code == 403

    def test_customer_role_denied(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-owner2@example.com", "admin-offer-owner2@example.com", db_session
        )
        customer_headers = register_and_login(client, "offer-customer@example.com", role="customer")
        assert client.post("/api/v1/offers", json=offer_payload(shop_id), headers=customer_headers).status_code == 403

    def test_audit_event_on_submit(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-audit@example.com", "admin-offer-audit@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        audit = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == offer_id, AuditLog.message.like("%verification%"))
            .one_or_none()
        )
        assert audit is not None

    def test_critical_edit_clears_confirmation_on_draft(self, client, db_session):
        headers, shop_id, admin = create_active_shop(
            client, "offer-clear@example.com", "admin-offer-clear@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=headers
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Try again"},
            headers=admin,
        )
        updated = client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"discount_value": "20"},
            headers=headers,
        ).json()["data"]
        assert updated["merchant_confirmed_at"] is None
        resubmit_without_confirm = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": False},
            headers=headers,
        )
        assert resubmit_without_confirm.status_code == 422
