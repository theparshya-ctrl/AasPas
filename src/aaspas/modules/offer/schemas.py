import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from aaspas.modules.offer.validation import validate_discount


class OfferCreate(BaseModel):
    shop_id: uuid.UUID
    title: str = Field(min_length=2, max_length=255)
    description: str | None = None
    discount_type: str = Field(default="percentage", max_length=50)
    discount_value: Decimal = Field(gt=0)
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    photo_url: str | None = Field(default=None, max_length=512)
    applicable_products: str | None = None
    min_purchase_amount: Decimal | None = Field(default=None, gt=0)
    terms: str | None = None

    @field_validator("discount_value")
    @classmethod
    def validate_discount_value(cls, v: Decimal, info) -> Decimal:
        discount_type = info.data.get("discount_type", "percentage")
        validate_discount(discount_type, v)
        return v


class OfferUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=2, max_length=255)
    description: str | None = None
    discount_type: str | None = Field(default=None, max_length=50)
    discount_value: Decimal | None = Field(default=None, gt=0)
    status: str | None = Field(default=None, max_length=50)
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    photo_url: str | None = Field(default=None, max_length=512)
    applicable_products: str | None = None
    min_purchase_amount: Decimal | None = Field(default=None, gt=0)
    terms: str | None = None


class OfferSubmitRequest(BaseModel):
    merchant_confirmed: bool = Field(
        description="Merchant must confirm offer authenticity before verification submission",
    )


class OfferRejectRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=2000)


class OfferResponse(BaseModel):
    id: uuid.UUID
    shop_id: uuid.UUID
    title: str
    description: str | None
    discount_type: str
    discount_value: Decimal
    status: str
    starts_at: datetime | None
    ends_at: datetime | None
    photo_url: str | None = None
    applicable_products: str | None = None
    min_purchase_amount: Decimal | None = None
    terms: str | None = None
    merchant_confirmed_at: datetime | None = None
    merchant_confirmed_by: uuid.UUID | None = None
    submitted_at: datetime | None = None
    approved_at: datetime | None = None
    rejected_at: datetime | None = None
    rejection_reason: str | None = None
    is_verified: bool = False
    source_type: str = "AASPAS"
    source_name: str | None = None
    source_url: str | None = None
    collected_at: datetime | None = None
    external_source_key: str | None = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class CustomerOfferDetailShopSummary(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    photo_url: str | None = None
    category: str | None = None
    address_area: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    distance_km: float | None = None


class CustomerOfferDetailResponse(BaseModel):
    offer_id: uuid.UUID
    title: str
    description: str | None = None
    photo_url: str | None = None
    discount_type: str
    discount_value: Decimal
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    status: str
    is_verified: bool = False
    source_type: str = "AASPAS"
    source_name: str | None = None
    source_url: str | None = None
    collected_at: datetime | None = None
    applicable_products: str | None = None
    min_purchase_amount: Decimal | None = None
    terms: str | None = None
    shop: CustomerOfferDetailShopSummary
