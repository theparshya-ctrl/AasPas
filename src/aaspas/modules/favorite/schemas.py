import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel


class FavoriteOfferItem(BaseModel):
    favorite_id: uuid.UUID
    offer_id: uuid.UUID
    title: str
    description: str | None = None
    photo_url: str | None = None
    discount_type: str
    discount_value: Decimal
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    status: str | None = None
    shop_id: uuid.UUID
    shop_name: str
    category: str | None = None
    distance_km: float | None = None
    is_active: bool
    saved_at: datetime


class FavoriteShopItem(BaseModel):
    favorite_id: uuid.UUID
    shop_id: uuid.UUID
    shop_name: str
    photo_url: str | None = None
    category: str | None = None
    address_area: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    distance_km: float | None = None
    is_active: bool
    saved_at: datetime


class FavoritesResponse(BaseModel):
    offers: list[FavoriteOfferItem]
    shops: list[FavoriteShopItem]


class FavoriteActionResponse(BaseModel):
    favorite_id: uuid.UUID
    offer_id: uuid.UUID | None = None
    shop_id: uuid.UUID | None = None
    saved: bool
