import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from aaspas.modules.location.schemas import LocationResponse
from aaspas.modules.shop.status import ShopStatus


class ShopAddressInput(BaseModel):
    address_line1: str = Field(min_length=1, max_length=255)
    address_line2: str | None = Field(default=None, max_length=255)
    city: str = Field(min_length=1, max_length=100)
    state: str | None = Field(default=None, max_length=100)
    postal_code: str | None = Field(default=None, max_length=20)
    country: str = Field(default="IN", min_length=2, max_length=2)
    latitude: Decimal = Field(description="GPS latitude")
    longitude: Decimal = Field(description="GPS longitude")

    @field_validator("latitude")
    @classmethod
    def validate_latitude(cls, v: Decimal) -> Decimal:
        if v < -90 or v > 90:
            raise ValueError("latitude must be between -90 and 90")
        return v

    @field_validator("longitude")
    @classmethod
    def validate_longitude(cls, v: Decimal) -> Decimal:
        if v < -180 or v > 180:
            raise ValueError("longitude must be between -180 and 180")
        return v


class BusinessHoursInput(BaseModel):
    opens_at: str = Field(pattern=r"^([01]\d|2[0-3]):[0-5]\d$", examples=["09:00"])
    closes_at: str = Field(pattern=r"^([01]\d|2[0-3]):[0-5]\d$", examples=["21:00"])


class ShopOnboardingCreate(BaseModel):
    name: str = Field(min_length=2, max_length=255)
    category: str = Field(min_length=1, max_length=100)
    description: str | None = None
    contact_number: str = Field(min_length=7, max_length=20)
    address: ShopAddressInput
    business_hours: BusinessHoursInput
    photo_url: str | None = Field(
        default=None,
        max_length=512,
        description="Public URL placeholder until upload service is integrated",
    )


class ShopOnboardingUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=255)
    category: str | None = Field(default=None, min_length=1, max_length=100)
    description: str | None = None
    contact_number: str | None = Field(default=None, min_length=7, max_length=20)
    address: ShopAddressInput | None = None
    business_hours: BusinessHoursInput | None = None
    photo_url: str | None = Field(default=None, max_length=512)


class ShopResponse(BaseModel):
    id: uuid.UUID
    owner_id: uuid.UUID
    name: str
    slug: str
    description: str | None
    category: str | None
    contact_number: str | None
    business_hours: dict | None
    photo_url: str | None
    photo_storage_key: str | None = None
    status: ShopStatus
    rejection_reason: str | None = None
    submitted_at: datetime | None = None
    approved_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class ShopDetailResponse(ShopResponse):
    primary_location: LocationResponse | None = None


class OfferStatusCounts(BaseModel):
    draft: int = 0
    pending_approval: int = 0
    rejected: int = 0
    scheduled: int = 0
    active: int = 0
    expired: int = 0


class ShopOwnerDashboardResponse(BaseModel):
    shop: ShopDetailResponse
    is_verified: bool = False
    can_edit_profile: bool = False
    can_submit_offers: bool = False
    offer_counts: OfferStatusCounts = Field(default_factory=OfferStatusCounts)
    status_message: str | None = None


class CustomerShopOfferItem(BaseModel):
    offer_id: uuid.UUID
    title: str
    description: str | None = None
    photo_url: str | None = None
    discount_type: str
    discount_value: Decimal
    starts_at: datetime
    ends_at: datetime
    status: str


class CustomerShopDetailResponse(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    description: str | None = None
    photo_url: str | None = None
    category: str | None = None
    address_line1: str | None = None
    address_line2: str | None = None
    area: str | None = None
    city: str | None = None
    pincode: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    phone: str | None = None
    business_hours: dict | None = None
    is_verified: bool = False
    active_offer_count: int = 0
    distance_km: float | None = None
    today_offers: list[CustomerShopOfferItem] = Field(default_factory=list)
    coming_soon: list[CustomerShopOfferItem] = Field(default_factory=list)
