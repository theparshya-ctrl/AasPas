"""Offer photo upload validation and persistence."""

from __future__ import annotations

import uuid

from aaspas.common.storage import build_offer_photo_key
from aaspas.common.storage_factory import get_storage_provider
from aaspas.config import Settings
from aaspas.modules.shop.photo import validate_shop_photo


class OfferPhotoStorage:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = get_storage_provider(settings)

    def save_offer_photo(
        self,
        *,
        shop_id: uuid.UUID,
        offer_id: uuid.UUID,
        content: bytes,
        declared_content_type: str | None,
    ) -> tuple[str, str]:
        validated = validate_shop_photo(
            content,
            declared_content_type,
            self.settings.shop_photo_max_bytes,
        )
        storage_key = build_offer_photo_key(shop_id, offer_id, validated.extension)
        saved_key = self.storage.save(relative_key=storage_key, content=content)
        prefix = self.settings.media_url_prefix.rstrip("/")
        photo_url = f"{prefix}/{saved_key.lstrip('/')}"
        return photo_url, saved_key

    def delete_storage_key(self, storage_key: str | None) -> None:
        if storage_key:
            self.storage.delete(storage_key)
