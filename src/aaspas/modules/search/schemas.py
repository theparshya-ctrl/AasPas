import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field


class SearchQuery(BaseModel):
    q: str | None = Field(default=None, max_length=255)
    category_id: uuid.UUID | None = None
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    radius_km: float | None = Field(default=None, gt=0, le=100)
    page: int = Field(default=1, ge=1)
    page_size: int = Field(default=20, ge=1, le=100)


class CustomerSearchOfferItem(BaseModel):
    offer_id: uuid.UUID
    title: str
    description: str | None = None
    photo_url: str | None = None
    discount_type: str
    discount_value: Decimal
    starts_at: datetime
    ends_at: datetime
    status: str
    shop_id: uuid.UUID
    shop_name: str
    category: str | None = None
    distance_km: float | None = None
    is_saved: bool = False
    is_verified: bool = False


class CustomerSearchResults(BaseModel):
    offers: list[CustomerSearchOfferItem] = Field(default_factory=list)
    total: int = 0
    page: int = 1
    page_size: int = 20
    total_pages: int = 0
