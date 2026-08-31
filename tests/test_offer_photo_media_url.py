from aaspas.common.storage import (
    resolve_customer_media_photo_url,
    resolve_customer_offer_photo_url,
    resolve_media_photo_url,
)
from aaspas.config import Settings


def test_resolve_media_photo_url_uses_storage_key():
    settings = Settings(media_public_base_url=None)
    url = resolve_media_photo_url(
        settings,
        photo_url="/media/legacy.jpg",
        photo_storage_key="offers/shop-1/offer-1/abc.jpg",
    )
    assert url == "/media/offers/shop-1/offer-1/abc.jpg"


def test_resolve_customer_offer_photo_prefers_offer():
    settings = Settings(media_public_base_url=None)
    url = resolve_customer_offer_photo_url(
        settings,
        offer_photo_url="/media/offers/offer.jpg",
        offer_photo_storage_key="offers/shop-1/offer-1/abc.jpg",
        shop_photo_url="/media/shops/shop.jpg",
        shop_photo_storage_key="shops/shop-1/def.jpg",
    )
    assert url == "/media/offers/shop-1/offer-1/abc.jpg"


def test_resolve_customer_offer_photo_falls_back_to_shop():
    settings = Settings(media_public_base_url="http://reachable.example:8000")
    url = resolve_customer_offer_photo_url(
        settings,
        offer_photo_url=None,
        offer_photo_storage_key=None,
        shop_photo_url="/media/shops/shop.jpg",
        shop_photo_storage_key="shops/shop-1/def.jpg",
    )
    assert url == "/media/shops/shop-1/def.jpg"


def test_resolve_customer_media_photo_url_ignores_public_base():
    settings = Settings(media_public_base_url="http://reachable.example:8000")
    url = resolve_customer_media_photo_url(
        settings,
        photo_url="http://192.168.1.4:8000/media/shops/old.jpg",
        photo_storage_key="shops/shop-1/photo.jpg",
    )
    assert url == "/media/shops/shop-1/photo.jpg"
