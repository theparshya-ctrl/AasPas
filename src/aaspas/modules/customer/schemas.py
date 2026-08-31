import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class CustomerProfileCreate(BaseModel):
    phone: str | None = Field(default=None, max_length=20)
    preferred_language: str = Field(default="en", max_length=10)
    bio: str | None = None


class CustomerProfileUpdate(BaseModel):
    phone: str | None = Field(default=None, max_length=20)
    preferred_language: str | None = Field(default=None, max_length=10)
    bio: str | None = None


class CustomerProfileResponse(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    phone: str | None
    preferred_language: str
    bio: str | None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}
