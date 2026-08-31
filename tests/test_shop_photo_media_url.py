import uuid

from aaspas.common.storage import resolve_shop_photo_url
from aaspas.config import Settings


def test_resolve_shop_photo_url_uses_media_public_base_when_storage_key_present():
    settings = Settings(
        media_public_base_url="http://reachable.example:8000",
        media_url_prefix="/media",
    )
    resolved = resolve_shop_photo_url(
        settings,
        photo_url="http://192.168.1.4:8000/media/shops/old.jpg",
        photo_storage_key=f"shops/{uuid.uuid4()}/photo.jpg",
    )
    assert resolved.startswith("http://reachable.example:8000/media/shops/")


def test_resolve_shop_photo_url_falls_back_without_public_base():
    settings = Settings(media_public_base_url=None)
    storage_key = "shops/x/photo.jpg"
    resolved = resolve_shop_photo_url(
        settings,
        photo_url="http://127.0.0.1:8000/media/shops/photo.jpg",
        photo_storage_key=storage_key,
    )
    assert resolved == "/media/shops/x/photo.jpg"
