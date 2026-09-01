"""Pydantic schemas for external offer import records."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from aaspas.modules.offer.validation import validate_discount


class ExternalImportAddress(BaseModel):
    address_line1: str = Field(min_length=1, max_length=255)
    address_line2: str | None = Field(default=None, max_length=255)
    city: str = Field(min_length=1, max_length=100)
    state: str | None = Field(default=None, max_length=100)
    postal_code: str | None = Field(default=None, max_length=20)
    country: str = Field(default="IN", min_length=2, max_length=2)
    latitude: Decimal | None = Field(default=None, description="GPS latitude when verified from public source")
    longitude: Decimal | None = Field(default=None, description="GPS longitude when verified from public source")

    @field_validator("latitude")
    @classmethod
    def validate_latitude(cls, value: Decimal | None) -> Decimal | None:
        if value is None:
            return None
        if value < -90 or value > 90:
            raise ValueError("latitude must be between -90 and 90")
        return value

    @field_validator("longitude")
    @classmethod
    def validate_longitude(cls, value: Decimal | None) -> Decimal | None:
        if value is None:
            return None
        if value < -180 or value > 180:
            raise ValueError("longitude must be between -180 and 180")
        return value


class ExternalOfferImportRecord(BaseModel):
    external_source_key: str = Field(min_length=8, max_length=255)
    shop_external_source_key: str | None = Field(default=None, min_length=8, max_length=255)
    shop_name: str = Field(min_length=2, max_length=255)
    category: str = Field(min_length=1, max_length=100)
    address: ExternalImportAddress
    title: str = Field(min_length=2, max_length=255)
    description: str | None = None
    discount_type: str = Field(default="percentage", max_length=50)
    discount_value: Decimal = Field(gt=0)
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    source_name: str = Field(min_length=1, max_length=255)
    source_url: str = Field(min_length=8, max_length=1024)
    collected_at: datetime

    @field_validator("discount_value")
    @classmethod
    def validate_discount_value(cls, value: Decimal, info) -> Decimal:
        discount_type = info.data.get("discount_type", "percentage")
        validate_discount(discount_type, value)
        return value


class ExternalOfferImportFile(BaseModel):
    offers: list[ExternalOfferImportRecord] = Field(default_factory=list)
