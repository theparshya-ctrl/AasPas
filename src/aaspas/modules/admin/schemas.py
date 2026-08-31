import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field


class ShopRejectRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=2000)


class AdminDashboard(BaseModel):
    pending_shops: int
    pending_offers: int = 0
    active_shops: int
    rejected_shops: int
    draft_shops: int


class AdminOfferMerchantSummary(BaseModel):
    user_id: uuid.UUID
    full_name: str | None = None
    email: str


class AdminOfferShopSummary(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    category: str | None = None
    status: str
    is_verified: bool = False
    photo_url: str | None = None


class AdminOfferReviewItem(BaseModel):
    id: uuid.UUID
    shop_id: uuid.UUID
    title: str
    description: str | None = None
    discount_type: str
    discount_value: Decimal
    status: str
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    photo_url: str | None = None
    applicable_products: str | None = None
    min_purchase_amount: Decimal | None = None
    terms: str | None = None
    merchant_confirmed_at: datetime | None = None
    submitted_at: datetime | None = None
    is_verified: bool = False
    shop: AdminOfferShopSummary
    merchant: AdminOfferMerchantSummary


class AdminShopOwnerSummary(BaseModel):
    user_id: uuid.UUID
    full_name: str | None = None
    email: str
    phone: str | None = None


class AdminShopReviewItem(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    description: str | None = None
    category: str | None = None
    photo_url: str | None = None
    contact_number: str | None = None
    business_hours: dict | None = None
    status: str
    submitted_at: datetime | None = None
    approved_at: datetime | None = None
    rejection_reason: str | None = None
    is_verified: bool = False
    address_line1: str | None = None
    address_line2: str | None = None
    area: str | None = None
    city: str | None = None
    pincode: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    owner: AdminShopOwnerSummary


class AdminShopListItem(BaseModel):
    shop_id: uuid.UUID
    shop_name: str
    owner_name: str | None = None
    owner_email: str
    category: str | None = None
    city: str | None = None
    status: str
    is_verified: bool = False
    created_at: datetime
    offer_count: int = 0
    rejection_reason: str | None = None


class AdminAuditEntry(BaseModel):
    message: str | None = None
    action: str
    actor_role: str | None = None
    created_at: datetime


class AdminShopDetail(AdminShopReviewItem):
    created_at: datetime
    offer_count: int = 0
    offer_counts_by_status: dict[str, int] = Field(default_factory=dict)
    audit_entries: list[AdminAuditEntry] = Field(default_factory=list)


class AdminUserListItem(BaseModel):
    user_id: uuid.UUID
    full_name: str | None = None
    email: str
    role: str
    is_active: bool = True
    shop_id: uuid.UUID | None = None
    shop_name: str | None = None
    created_at: datetime
