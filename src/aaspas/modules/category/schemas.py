import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from aaspas.modules.search.schemas import CustomerSearchOfferItem


class CategoryResponse(BaseModel):
    id: uuid.UUID
    name: str
    slug: str
    is_active: bool
    display_order: int

    model_config = {"from_attributes": True}


class CategoryDetailResponse(CategoryResponse):
    created_at: datetime
    updated_at: datetime


class CategoryOffersResponse(BaseModel):
    category_id: uuid.UUID
    category_name: str
    category_slug: str
    today_offers: list[CustomerSearchOfferItem] = Field(default_factory=list)
    coming_soon: list[CustomerSearchOfferItem] = Field(default_factory=list)
    total_active: int = 0
    total_coming_soon: int = 0
    page: int = 1
    page_size: int = 20
    total_pages: int = 0
