from pathlib import Path
import uuid

from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.status import ShopStatus
from tests.helpers import admin_headers, create_shop, register_and_login, submit_shop
from tests.test_shop_photo_upload import MINIMAL_JPEG


def create_active_shop(client, owner_email: str, admin_email: str, db_session) -> tuple[dict, str, dict]:
    headers = register_and_login(client, owner_email)
    shop_id = create_shop(client, headers).json()["data"]["id"]
    submit_shop(client, shop_id, headers)
    admin = admin_headers(client, db_session, admin_email)
    client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)
    return headers, shop_id, admin


def create_draft_offer(client, shop_id: str, headers: dict) -> str:
    response = client.post(
        "/api/v1/offers",
        json={
            "shop_id": shop_id,
            "title": "Photo Offer",
            "description": "Valid offer description for photo upload.",
            "discount_type": "percentage",
            "discount_value": "10",
            "starts_at": "2026-08-21T10:00:00+00:00",
            "ends_at": "2026-08-28T18:00:00+00:00",
        },
        headers=headers,
    )
    assert response.status_code == 201
    return response.json()["data"]["id"]


class TestOfferPhotoUpload:
    def test_shop_owner_can_upload_offer_photo(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-photo-owner@example.com", "offer-photo-admin@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)

        response = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["photo_url"].startswith("/media/offers/")
        assert data["photo_url"].endswith(".jpg")

        offer = db_session.get(Offer, uuid.UUID(offer_id))
        assert offer.photo_storage_key is not None
        assert offer.photo_storage_key.startswith(f"offers/{shop_id}/{offer_id}/")

    def test_other_owner_cannot_upload_offer_photo(self, client, db_session):
        owner_a = register_and_login(client, "offer-photo-a@example.com")
        owner_b = register_and_login(client, "offer-photo-b@example.com")
        headers_a, shop_id, _admin = create_active_shop(
            client, "offer-photo-a@example.com", "offer-photo-admin2@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers_a)

        response = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=owner_b,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 403

    def test_pending_offer_cannot_upload_photo(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-photo-pending@example.com", "offer-photo-admin3@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )

        response = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 422

    def test_photo_upload_invalidates_merchant_confirmation(self, client, db_session):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-photo-confirm@example.com", "offer-photo-admin4@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        offer = db_session.get(Offer, uuid.UUID(offer_id))
        offer.status = OfferStatus.DRAFT.value
        offer.merchant_confirmed_at = offer.submitted_at
        db_session.commit()

        response = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 200
        db_session.refresh(offer)
        assert offer.merchant_confirmed_at is None

    def test_replacement_updates_storage_key(self, client, db_session):
        from aaspas.config import get_settings

        settings = get_settings()
        headers, shop_id, _admin = create_active_shop(
            client, "offer-photo-replace@example.com", "offer-photo-admin5@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)
        first = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        ).json()["data"]["photo_url"]
        second = client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer2.jpg", MINIMAL_JPEG, "image/jpeg")},
        ).json()["data"]["photo_url"]
        assert first != second

        offer = db_session.get(Offer, uuid.UUID(offer_id))
        file_path = Path(settings.media_root) / offer.photo_storage_key
        assert file_path.is_file()
        assert file_path.stat().st_size > 0

    def test_customer_sees_approved_offer_photo(self, client, db_session, fixed_now_client):
        headers, shop_id, admin = create_active_shop(
            client, "offer-photo-customer@example.com", "offer-photo-admin6@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)
        client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        client.post(
            f"/api/v1/offers/{offer_id}/submit",
            json={"merchant_confirmed": True},
            headers=headers,
        )
        client.post(f"/api/v1/admin/offers/{offer_id}/approve", headers=admin)

        detail = fixed_now_client.get(f"/api/v1/offers/{offer_id}").json()["data"]
        assert detail["photo_url"].startswith("/media/offers/")

    def test_customer_cannot_see_draft_offer_photo(self, client, db_session, fixed_now_client):
        headers, shop_id, _admin = create_active_shop(
            client, "offer-photo-hidden@example.com", "offer-photo-admin7@example.com", db_session
        )
        offer_id = create_draft_offer(client, shop_id, headers)
        client.post(
            f"/api/v1/offers/{offer_id}/photo",
            headers=headers,
            files={"file": ("offer.jpg", MINIMAL_JPEG, "image/jpeg")},
        )

        response = fixed_now_client.get(f"/api/v1/offers/{offer_id}")
        assert response.status_code == 404
