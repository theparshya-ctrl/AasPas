"""S3/Neon media storage preparation tests."""

from unittest.mock import MagicMock, patch

import pytest

from aaspas.common.storage import (
    S3ObjectStorage,
    resolve_customer_offer_photo_url,
    resolve_customer_shop_photo_url,
    resolve_shop_photo_url,
)
from aaspas.common.storage_factory import get_storage_provider, is_local_media_storage
from aaspas.config import Settings


def test_default_dev_uses_local_storage():
    settings = Settings(media_storage_backend="local")
    provider = get_storage_provider(settings)
    assert is_local_media_storage(settings)
    assert provider.__class__.__name__ == "LocalFileStorage"


def test_s3_storage_requires_credentials():
    settings = Settings(media_storage_backend="s3")
    with pytest.raises(ValueError, match="S3 storage requires"):
        get_storage_provider(settings)


@patch("boto3.client")
def test_s3_storage_save_and_delete(mock_boto_client):
    client = MagicMock()
    mock_boto_client.return_value = client
    storage = S3ObjectStorage(
        endpoint_url="https://example.storage.neon.tech",
        region_name="us-east-2",
        access_key_id="nak_test",
        secret_access_key="nsk_test",
        bucket="aaspas-beta-media",
    )
    key = storage.save(relative_key="shops/x/photo.jpg", content=b"abc")
    assert key == "shops/x/photo.jpg"
    client.put_object.assert_called_once()
    storage.delete(key)
    client.delete_object.assert_called_once()


def test_s3_customer_urls_are_absolute_https():
    settings = Settings(
        media_storage_backend="s3",
        media_public_base_url="https://br-example.storage.c-2.us-east-2.aws.neon.tech/aaspas-beta-media",
    )
    url = resolve_customer_shop_photo_url(
        settings,
        photo_url=None,
        photo_storage_key="shops/shop-1/photo.jpg",
    )
    assert url == (
        "https://br-example.storage.c-2.us-east-2.aws.neon.tech/aaspas-beta-media/shops/shop-1/photo.jpg"
    )


def test_s3_merchant_urls_are_absolute_https():
    settings = Settings(
        media_storage_backend="s3",
        media_public_base_url="https://br-example.storage.c-2.us-east-2.aws.neon.tech/aaspas-beta-media",
    )
    url = resolve_shop_photo_url(
        settings,
        photo_url=None,
        photo_storage_key="offers/shop-1/offer-1/photo.jpg",
    )
    assert url.startswith("https://")


def test_local_customer_urls_remain_relative():
    settings = Settings(media_storage_backend="local")
    url = resolve_customer_offer_photo_url(
        settings,
        offer_photo_url=None,
        offer_photo_storage_key="offers/s/o/photo.jpg",
        shop_photo_url=None,
        shop_photo_storage_key=None,
    )
    assert url == "/media/offers/s/o/photo.jpg"


def test_main_app_skips_static_mount_for_s3(monkeypatch):
    monkeypatch.setenv("APP_ENV", "staging")
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
    monkeypatch.setenv("MEDIA_STORAGE_BACKEND", "s3")
    monkeypatch.setenv("MEDIA_PUBLIC_BASE_URL", "https://cdn.example/bucket")

    from aaspas.config import get_settings

    get_settings.cache_clear()

    from aaspas.main import create_app

    app = create_app()
    mounted_paths = [getattr(route, "path", None) for route in app.routes]
    assert "/media" not in mounted_paths

    get_settings.cache_clear()
