from datetime import UTC, datetime, timedelta

from aaspas.common.audit import AuditLog
from aaspas.modules.offer.status import OfferStatus
from tests.test_offer_lifecycle import FIXED_NOW, create_active_shop, offer_payload
from tests.helpers import (
    admin_headers,
    register_and_login,
    register_customer,
)


class TestAdminOfferVerification:
    def _submit_pending_offer(self, client, db_session, owner_email: str, admin_email: str) -> tuple[dict, dict, str, str]:
        owner, shop_id, admin = create_active_shop(client, owner_email, admin_email, db_session)
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        return owner, admin, shop_id, offer_id

    def test_customer_cannot_access_admin_queue(self, client, db_session):
        customer = register_customer(client, "admin-q-customer@example.com")
        response = client.get("/api/v1/admin/offers/pending", headers=customer)
        assert response.status_code == 403

    def test_shop_owner_cannot_access_admin_queue(self, client, db_session):
        owner, _admin, _shop_id, _offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-owner@example.com", "admin-q-admin1@example.com"
        )
        response = client.get("/api/v1/admin/offers/pending", headers=owner)
        assert response.status_code == 403

    def test_admin_can_access_queue(self, client, db_session):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-owner2@example.com", "admin-q-admin2@example.com"
        )
        response = client.get("/api/v1/admin/offers/pending", headers=admin)
        assert response.status_code == 200
        ids = [item["id"] for item in response.json()["data"]]
        assert offer_id in ids

    def test_pending_offers_appear_in_queue(self, client, db_session):
        _owner, admin, shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-owner3@example.com", "admin-q-admin3@example.com"
        )
        item = next(
            item for item in client.get("/api/v1/admin/offers/pending", headers=admin).json()["data"]
            if item["id"] == offer_id
        )
        assert item["status"] == OfferStatus.PENDING_APPROVAL.value
        assert item["shop"]["shop_id"] == shop_id
        assert item["merchant"]["email"] == "admin-q-owner3@example.com"
        assert item["submitted_at"] is not None
        assert item["merchant_confirmed_at"] is not None

    def test_draft_offers_do_not_appear(self, client, db_session):
        owner, shop_id, admin = create_active_shop(
            client, "admin-q-draft@example.com", "admin-q-admin4@example.com", db_session
        )
        draft_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        ids = [item["id"] for item in client.get("/api/v1/admin/offers/pending", headers=admin).json()["data"]]
        assert draft_id not in ids

    def test_approved_offers_do_not_appear(self, client, db_session, fixed_now_client):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-approved@example.com", "admin-q-admin5@example.com"
        )
        fixed_now_client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        ids = [item["id"] for item in client.get("/api/v1/admin/offers/pending", headers=admin).json()["data"]]
        assert offer_id not in ids

    def test_admin_approve(self, client, db_session, fixed_now_client):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-approve@example.com", "admin-q-admin6@example.com"
        )
        response = fixed_now_client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["is_verified"] is True
        assert data["approved_at"] is not None

    def test_admin_reject(self, client, db_session):
        owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-reject@example.com", "admin-q-admin7@example.com"
        )
        response = client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Discount information incorrect"},
            headers=admin,
        )
        assert response.status_code == 200
        assert response.json()["data"]["status"] == OfferStatus.REJECTED.value

    def test_reject_requires_reason(self, client, db_session):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-reject2@example.com", "admin-q-admin8@example.com"
        )
        response = client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "  "},
            headers=admin,
        )
        assert response.status_code == 422

    def test_is_verified_only_after_approval(self, client, db_session):
        owner, admin, shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-verify@example.com", "admin-q-admin9@example.com"
        )
        pending = client.get(f"/api/v1/offers/manage/{offer_id}", headers=owner).json()["data"]
        assert pending["is_verified"] is False
        approved = client.post(
            f"/api/v1/admin/offers/{offer_id}/approve",
            headers=admin,
        ).json()["data"]
        assert approved["is_verified"] is True

    def test_audit_event_on_approval(self, client, db_session):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-audit1@example.com", "admin-q-admin10@example.com"
        )
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        logs = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == offer_id, AuditLog.message.like("%approved%"))
            .all()
        )
        assert logs

    def test_audit_event_on_rejection(self, client, db_session):
        _owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-audit2@example.com", "admin-q-admin11@example.com"
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Invalid terms"},
            headers=admin,
        )
        logs = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == offer_id, AuditLog.message.like("%rejected%"))
            .all()
        )
        assert logs

    def test_approved_future_offer_scheduled(self, client, db_session, fixed_now_client):
        owner, shop_id, admin = create_active_shop(
            client, "admin-q-future@example.com", "admin-q-admin12@example.com", db_session
        )
        future_start = (FIXED_NOW + timedelta(days=2)).isoformat()
        future_end = (FIXED_NOW + timedelta(days=5)).isoformat()
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id, starts_at=future_start, ends_at=future_end),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        approved = fixed_now_client.post(
            f"/api/v1/admin/offers/{offer_id}/approve", headers=admin
        ).json()["data"]
        assert approved["status"] == OfferStatus.SCHEDULED.value
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 200

    def test_approved_current_offer_active(self, client, db_session, fixed_now_client):
        owner, shop_id, admin = create_active_shop(
            client, "admin-q-current@example.com", "admin-q-admin13@example.com", db_session
        )
        start = (FIXED_NOW - timedelta(hours=1)).isoformat()
        end = (FIXED_NOW + timedelta(days=2)).isoformat()
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id, starts_at=start, ends_at=end),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        approved = fixed_now_client.post(
            f"/api/v1/admin/offers/{offer_id}/approve", headers=admin
        ).json()["data"]
        assert approved["status"] == OfferStatus.ACTIVE.value

    def test_expired_offer_hidden(self, client, db_session, fixed_now_client):
        owner, shop_id, admin = create_active_shop(
            client, "admin-q-expired@example.com", "admin-q-admin14@example.com", db_session
        )
        start = (FIXED_NOW - timedelta(days=5)).isoformat()
        end = (FIXED_NOW - timedelta(days=1)).isoformat()
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id, starts_at=start, ends_at=end),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        approved = fixed_now_client.post(
            f"/api/v1/admin/offers/{offer_id}/approve", headers=admin
        ).json()["data"]
        assert approved["status"] == OfferStatus.EXPIRED.value
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404

    def test_merchant_sees_rejection_reason(self, client, db_session):
        owner, admin, shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-reason@example.com", "admin-q-admin15@example.com"
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Image misleading"},
            headers=admin,
        )
        offers = client.get(f"/api/v1/offers/shop/{shop_id}", headers=owner).json()["data"]
        rejected = next(item for item in offers if item["id"] == offer_id)
        assert rejected["rejection_reason"] == "Image misleading"

    def test_merchant_edits_rejected_offer(self, client, db_session):
        owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-edit@example.com", "admin-q-admin16@example.com"
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Offer details unclear"},
            headers=admin,
        )
        response = client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"title": "Corrected Offer Title"},
            headers=owner,
        )
        assert response.status_code == 200
        assert response.json()["data"]["status"] == OfferStatus.DRAFT.value

    def test_edit_invalidates_previous_confirmation(self, client, db_session):
        owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-invalidate@example.com", "admin-q-admin17@example.com"
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Invalid terms"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"description": "Updated description with enough detail for customers."},
            headers=owner,
        )
        data = client.get(f"/api/v1/offers/manage/{offer_id}", headers=owner).json()["data"]
        assert data["merchant_confirmed_at"] is None

    def test_merchant_reconfirms_and_resubmits(self, client, db_session):
        owner, admin, _shop_id, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-resubmit@example.com", "admin-q-admin18@example.com"
        )
        client.post(
            f"/api/v1/admin/offers/{offer_id}/reject",
            json={"reason": "Other"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/offers/{offer_id}",
            json={"terms": "Updated terms and conditions apply."},
            headers=owner,
        )
        resubmit = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        assert resubmit.status_code == 200
        assert resubmit.json()["data"]["status"] == OfferStatus.PENDING_APPROVAL.value

    def test_customer_cannot_access_other_merchant_management(self, client, db_session):
        owner_a, _admin, shop_a, offer_id = self._submit_pending_offer(
            client, db_session, "admin-q-owner-a@example.com", "admin-q-admin19@example.com"
        )
        owner_b = register_and_login(client, "admin-q-owner-b@example.com")
        assert client.get(f"/api/v1/offers/manage/{offer_id}", headers=owner_b).status_code == 403
        assert client.get(f"/api/v1/offers/shop/{shop_a}", headers=owner_b).status_code == 403

    def test_e2e_pending_hidden_until_approved(self, client, db_session, fixed_now_client):
        owner, shop_id, admin = create_active_shop(
            client, "admin-q-e2e@example.com", "admin-q-admin20@example.com", db_session
        )
        start = (FIXED_NOW - timedelta(hours=1)).isoformat()
        end = (FIXED_NOW + timedelta(days=3)).isoformat()
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(shop_id, starts_at=start, ends_at=end),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404
        fixed_now_client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        assert detail["is_verified"] is True
        assert detail["status"] == OfferStatus.ACTIVE.value
