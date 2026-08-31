import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field


class HomeLocationInfo(BaseModel):
    latitude: float | None = None
    longitude: float | None = None
    available: bool = False


class HomeOfferItem(BaseModel):
    offer_id: uuid.UUID
    title: str
    description: str | None = None
    photo_url: str | None = None
    discount_type: str
    discount_value: Decimal
    starts_at: datetime
    ends_at: datetime
    shop_id: uuid.UUID
    shop_name: str
    distance_km: float | None = None
    category: str | None = None
    is_saved: bool = False
    is_verified: bool = False


class HomeShopItem(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    photo_url: str | None = None
    category: str | None = None
    address_area: str | None = None
    distance_km: float
    latitude: float
    longitude: float
    is_saved: bool = False


class HomeCategoryItem(BaseModel):
    id: uuid.UUID
    name: str
    slug: str
    display_order: int


class HomeResponse(BaseModel):
    location: HomeLocationInfo
    categories: list[HomeCategoryItem]
    today_offers: list[HomeOfferItem]
    coming_soon: list[HomeOfferItem]
    nearby_shops: list[HomeShopItem]


class HomeQueryParams(BaseModel):
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    radius_km: float | None = Field(default=None, gt=0, le=100)
