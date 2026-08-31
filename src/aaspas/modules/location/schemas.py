import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field


class LocationCreate(BaseModel):
    shop_id: uuid.UUID
    label: str = Field(max_length=255)
    address_line1: str = Field(max_length=255)
    address_line2: str | None = Field(default=None, max_length=255)
    city: str = Field(max_length=100)
    state: str | None = Field(default=None, max_length=100)
    postal_code: str | None = Field(default=None, max_length=20)
    country: str = Field(default="IN", max_length=2)
    latitude: Decimal | None = None
    longitude: Decimal | None = None
    is_primary: bool = False


class LocationResponse(BaseModel):
    id: uuid.UUID
    shop_id: uuid.UUID
    label: str
    address_line1: str
    address_line2: str | None
    city: str
    state: str | None
    postal_code: str | None
    country: str
    latitude: Decimal | None
    longitude: Decimal | None
    is_primary: bool
    created_at: datetime

    model_config = {"from_attributes": True}
