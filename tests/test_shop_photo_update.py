from aaspas.modules.shop.status import ShopStatus
from tests.helpers import (
    admin_headers,
    create_shop,
    register_and_login,
    submit_shop,
)


class TestShopPhotoUpdate:
    def test_active_shop_owner_can_update_photo_only(self, client, db_session):
        owner = register_and_login(client, "photo-active@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "photo-admin@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        response = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"photo_url": "https://example.com/shop-photo.jpg"},
            headers=owner,
        )
        assert response.status_code == 200
        data = response.json()["data"]
        assert data["photo_url"] == "https://example.com/shop-photo.jpg"
        assert data["status"] == ShopStatus.ACTIVE.value

    def test_active_shop_owner_cannot_update_other_fields(self, client, db_session):
        owner = register_and_login(client, "photo-readonly@example.com")
        shop_id = create_shop(client, owner).json()["data"]["id"]
        submit_shop(client, shop_id, owner)
        admin = admin_headers(client, db_session, "photo-admin-ro@example.com")
        client.post(f"/api/v1/admin/shops/{shop_id}/approve", headers=admin)

        response = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"name": "Renamed Shop"},
            headers=owner,
        )
        assert response.status_code == 422

    def test_other_owner_cannot_update_photo(self, client, db_session):
        owner_a = register_and_login(client, "photo-owner-a@example.com")
        owner_b = register_and_login(client, "photo-owner-b@example.com")
        shop_id = create_shop(client, owner_a).json()["data"]["id"]

        response = client.patch(
            f"/api/v1/shops/{shop_id}",
            json={"photo_url": "https://example.com/hijack.jpg"},
            headers=owner_b,
        )
        assert response.status_code in {403, 404}
