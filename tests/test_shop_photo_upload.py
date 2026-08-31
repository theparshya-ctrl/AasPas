from aaspas.modules.shop.status import ShopStatus
from tests.helpers import admin_headers, create_shop, register_and_login, submit_shop

MINIMAL_JPEG = (
    b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00"
    b"\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.\' \",#\x1c\x1c(7),01444\x1f\'9=82<.342"
    b"\xff\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x14\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x03"
    b"\xff\xc4\x00\x14\x10\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xff\xda\x00\x08\x01\x01\x00\x00?\x00\x7f\xff\xd9"
)
MINIMAL_PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde"
    b"\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
)


class TestShopPhotoUpload:
    def test_shop_owner_can_upload_photo(self, client, db_session):
        owner = register_and_login(client, "upload-owner@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]

        response = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("shop.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["id"] == shop_id
        assert data["photo_url"].startswith("/media/shops/")
        assert data["photo_url"].endswith(".jpg")

    def test_other_owner_cannot_upload_photo(self, client, db_session):
        owner_a = register_and_login(client, "upload-owner-a@example.com")
        owner_b = register_and_login(client, "upload-owner-b@example.com")
        create_shop(client, owner_a)

        response = client.post(
            "/api/v1/shops/me/photo",
            headers=owner_b,
            files={"file": ("shop.jpg", MINIMAL_JPEG, "image/jpeg")},
        )
        assert response.status_code == 404

    def test_rejects_invalid_mime(self, client, db_session):
        owner = register_and_login(client, "upload-invalid@example.com")
        create_shop(client, owner)

        response = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("notes.txt", b"not-an-image", "text/plain")},
        )
        assert response.status_code == 422

    def test_rejects_oversized_image(self, client, db_session, monkeypatch):
        from aaspas.config import get_settings

        monkeypatch.setenv("SHOP_PHOTO_MAX_BYTES", "1024")
        get_settings.cache_clear()
        owner = register_and_login(client, "upload-big@example.com")
        create_shop(client, owner)

        response = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("big.jpg", MINIMAL_JPEG * 20, "image/jpeg")},
        )
        assert response.status_code == 422
        get_settings.cache_clear()

    def test_replacement_updates_url_and_keeps_active_status(self, client, db_session):
        owner = register_and_login(client, "upload-replace@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "upload-admin@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        first = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("one.jpg", MINIMAL_JPEG, "image/jpeg")},
        ).json()["data"]
        second = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("two.png", MINIMAL_PNG, "image/png")},
        ).json()["data"]

        assert first["photo_url"] != second["photo_url"]
        assert second["status"] == ShopStatus.ACTIVE.value
        assert second["photo_url"].endswith(".png")

    def test_customer_sees_latest_photo_url(self, client, db_session):
        owner = register_and_login(client, "upload-customer@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "upload-admin-customer@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        uploaded = client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("shop.jpg", MINIMAL_JPEG, "image/jpeg")},
        ).json()["data"]["photo_url"]

        detail = client.get(f"/api/v1/shops/{shop_id}").json()["data"]
        assert detail["photo_url"] == uploaded

    def test_customer_api_rewrites_photo_url_when_media_public_base_set(
        self, client, db_session, monkeypatch
    ):
        from aaspas.config import get_settings

        monkeypatch.setenv("MEDIA_PUBLIC_BASE_URL", "http://reachable.example:8000")
        get_settings.cache_clear()
        owner = register_and_login(client, "upload-public-base@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "upload-admin-public-base@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        client.post(
            "/api/v1/shops/me/photo",
            headers=owner,
            files={"file": ("shop.jpg", MINIMAL_JPEG, "image/jpeg")},
        )

        detail = client.get(f"/api/v1/shops/{shop_id}").json()["data"]
        assert detail["photo_url"].startswith("/media/shops/")
        get_settings.cache_clear()
