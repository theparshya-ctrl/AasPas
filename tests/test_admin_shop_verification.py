from aaspas.common.audit import AuditLog
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.status import ShopStatus
from tests.helpers import (
    admin_headers,
    create_shop,
    register_and_login,
    register_customer,
    submit_shop,
)
from tests.test_offer_lifecycle import FIXED_NOW, create_active_shop, offer_payload


class TestAdminShopVerification:
    def _pending_shop(self, client, db_session, owner_email: str, admin_email: str) -> tuple[dict, dict, str]:
        owner = register_and_login(client, owner_email)
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, admin_email)
        return owner, admin, shop_id

    def test_customer_cannot_access_admin_shop_queue(self, client, db_session):
        customer = register_customer(client, "shop-q-customer@example.com")
        response = client.get("/api/v1/admin/shops/pending", headers=customer)
        assert response.status_code == 403

    def test_shop_owner_cannot_access_admin_shop_queue(self, client, db_session):
        owner, _admin, _shop_id = self._pending_shop(
            client, db_session, "shop-q-owner@example.com", "shop-q-admin1@example.com"
        )
        response = client.get("/api/v1/admin/shops/pending", headers=owner)
        assert response.status_code == 403

    def test_admin_can_access_queue(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-owner2@example.com", "shop-q-admin2@example.com"
        )
        response = client.get("/api/v1/admin/shops/pending", headers=admin)
        assert response.status_code == 200
        ids = [item["shop_id"] for item in response.json()["data"]]
        assert shop_id in ids

    def test_pending_shop_appears_in_queue(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-owner3@example.com", "shop-q-admin3@example.com"
        )
        item = next(
            item for item in client.get("/api/v1/admin/shops/pending", headers=admin).json()["data"]
            if item["shop_id"] == shop_id
        )
        assert item["status"] == ShopStatus.PENDING_APPROVAL.value
        assert item["shop_name"] == "Fresh Mart"
        assert item["category"] == "grocery"
        assert item["owner"]["email"] == "shop-q-owner3@example.com"
        assert item["submitted_at"] is not None

    def test_active_shop_does_not_appear_in_pending_queue(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-active@example.com", "shop-q-admin4@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        ids = [item["shop_id"] for item in client.get("/api/v1/admin/shops/pending", headers=admin).json()["data"]]
        assert shop_id not in ids

    def test_rejected_shop_not_in_pending_until_resubmitted(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-reject@example.com", "shop-q-admin5@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Address unclear"},
            headers=admin,
        )
        ids = [item["shop_id"] for item in client.get("/api/v1/admin/shops/pending", headers=admin).json()["data"]]
        assert shop_id not in ids

        client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"contact_number": "+919999999998"},
            headers=owner,
        )
        submit_shop(client, shop_id, owner)
        ids = [item["shop_id"] for item in client.get("/api/v1/admin/shops/pending", headers=admin).json()["data"]]
        assert shop_id in ids

    def test_admin_review_works(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-review@example.com", "shop-q-admin6@example.com"
        )
        response = client.get(f"/api/v1/admin/shops/{shop_id}", headers=admin)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["shop_id"] == shop_id
        assert data["shop_name"] == "Fresh Mart"
        assert data["owner"]["email"] == "shop-q-review@example.com"
        assert data["latitude"] is not None
        assert data["longitude"] is not None

    def test_admin_approve_works(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-approve@example.com", "shop-q-admin7@example.com"
        )
        response = client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["status"] == ShopStatus.ACTIVE.value
        assert data["approved_at"] is not None
        assert data["is_verified"] is True

    def test_admin_reject_requires_reason(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-reject-req@example.com", "shop-q-admin8@example.com"
        )
        response = client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "ab"},
            headers=admin,
        )
        assert response.status_code == 422

    def test_rejection_reason_stored(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-reject-store@example.com", "shop-q-admin9@example.com"
        )
        reason = "Business details incomplete"
        response = client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": reason},
            headers=admin,
        )
        assert response.status_code == 200
        assert response.json()["data"]["rejection_reason"] == reason

    def test_audit_created_on_approval(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-audit-app@example.com", "shop-q-admin10@example.com"
        )
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
        logs = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == shop_id, AuditLog.module == "admin")
            .all()
        )
        assert any("approved" in (log.message or "").lower() for log in logs)

    def test_audit_created_on_rejection(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-audit-rej@example.com", "shop-q-admin11@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Unable to verify shop"},
            headers=admin,
        )
        logs = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_id == shop_id, AuditLog.module == "admin")
            .all()
        )
        assert any("rejected" in (log.message or "").lower() for log in logs)

    def test_shop_owner_sees_pending_status(self, client, db_session):
        owner, _admin, _shop_id = self._pending_shop(
            client, db_session, "shop-q-owner-pend@example.com", "shop-q-admin12@example.com"
        )
        dashboard = client.get("/api/v1/shops/me/dashboard", headers=owner).json()["data"]
        assert dashboard["shop"]["status"] == ShopStatus.PENDING_APPROVAL.value
        assert "Pending verification" in dashboard["status_message"]

    def test_shop_owner_sees_rejection_reason(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-owner-rej@example.com", "shop-q-admin13@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Shop photo insufficient"},
            headers=admin,
        )
        dashboard = client.get("/api/v1/shops/me/dashboard", headers=owner).json()["data"]
        assert dashboard["shop"]["status"] == ShopStatus.REJECTED.value
        assert "Shop verification needs changes" in dashboard["status_message"]
        assert "Shop photo insufficient" in dashboard["status_message"]

    def test_shop_owner_can_edit_rejected_shop(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-owner-edit@example.com", "shop-q-admin14@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Category incorrect"},
            headers=admin,
        )
        update = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"category": "electronics"},
            headers=owner,
        )
        assert update.status_code == 200
        assert update.json()["data"]["category"] == "electronics"

    def test_shop_owner_can_resubmit(self, client, db_session):
        owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-owner-resub@example.com", "shop-q-admin15@example.com"
        )
        client.post(
            f"/api/v1/admin/shops/{shop_id}/reject",
            json={"reason": "Fix contact number"},
            headers=admin,
        )
        client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"contact_number": "+919888888888"},
            headers=owner,
        )
        resubmit = submit_shop(client, shop_id, owner)
        assert resubmit.status_code == 200
        assert resubmit.json()["data"]["status"] == ShopStatus.PENDING_APPROVAL.value

    def test_unapproved_shop_offer_cannot_be_submitted(self, client, db_session):
        owner = register_and_login(client, "shop-q-offer-block@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        response = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        assert response.status_code == 422
        assert "approved" in response.json()["error"]["message"].lower()

    def test_approved_shop_offer_can_be_submitted(self, client, db_session):
        owner, shop_id, _admin = create_active_shop(
            client, "shop-q-offer-ok@example.com", "shop-q-admin16@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        response = client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        assert response.status_code == 200
        assert response.json()["data"]["status"] == OfferStatus.PENDING_APPROVAL.value

    def test_customer_cannot_see_offer_from_unapproved_shop(
        self, client, db_session, fixed_now_client
    ):
        owner = register_and_login(client, "shop-q-cust-hide@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        offer_id = client.post(
            "/api/v1/offers", json=offer_payload(shop_id), headers=owner
        ).json()["data"]["id"]
        assert fixed_now_client.get(f"/api/v1/offers/{offer_id}").status_code == 404

    def test_customer_sees_offer_after_shop_and_offer_verification(
        self, client, db_session, fixed_now_client
    ):
        owner, shop_id, admin = create_active_shop(
            client, "shop-q-cust-see@example.com", "shop-q-admin17@example.com", db_session
        )
        offer_id = client.post(
            "/api/v1/offers",
            json=offer_payload(
                shop_id,
                starts_at=FIXED_NOW.isoformat(),
                ends_at=(FIXED_NOW.replace(hour=23)).isoformat(),
            ),
            headers=owner,
        ).json()["data"]["id"]
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=owner,
        )
        fixed_now_client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)
        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        assert detail["is_verified"] is True

    def test_cross_owner_access_denied(self, client, db_session):
        owner, _admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-cross-a@example.com", "shop-q-admin18@example.com"
        )
        other = register_and_login(client, "shop-q-cross-b@example.com")
        assert client.get(f"/api/v1/shops/{shop_id}", headers=other).status_code == 403
        assert client.patch(f"/api/v1/shops/{shop_id}", json={"name": "Hacked"}, headers=other).status_code == 403

    def test_admin_cross_shop_review_allowed(self, client, db_session):
        _owner, admin, shop_id = self._pending_shop(
            client, db_session, "shop-q-cross-admin@example.com", "shop-q-admin19@example.com"
        )
        response = client.get(f"/api/v1/admin/shops/{shop_id}", headers=admin)
        assert response.status_code == 200
