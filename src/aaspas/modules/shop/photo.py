"""Shop photo upload validation and persistence."""

from __future__ import annotations

import imghdr
from dataclasses import dataclass

from aaspas.common.exceptions import ValidationAppError
from aaspas.common.storage import build_shop_photo_key
from aaspas.common.storage_factory import get_storage_provider
from aaspas.config import Settings

ALLOWED_IMAGE_TYPES = {
    "jpeg": "image/jpeg",
    "png": "image/png",
    "webp": "image/webp",
}
EXTENSION_BY_TYPE = {
    "jpeg": "jpg",
    "png": "png",
    "webp": "webp",
}


@dataclass(frozen=True)
class ValidatedImage:
    image_type: str
    extension: str
    content_type: str


def detect_image_type(content: bytes) -> str | None:
    detected = imghdr.what(None, h=content)
    if detected == "jpeg":
        return "jpeg"
    if detected in ALLOWED_IMAGE_TYPES:
        return detected
    if content.startswith(b"RIFF") and content[8:12] == b"WEBP":
        return "webp"
    return None


def validate_shop_photo(content: bytes, declared_content_type: str | None, max_bytes: int) -> ValidatedImage:
    if not content:
        raise ValidationAppError("Photo file is empty")
    if len(content) > max_bytes:
        raise ValidationAppError(
            "Photo file is too large",
            details={"max_bytes": max_bytes, "size_bytes": len(content)},
        )

    image_type = detect_image_type(content)
    if image_type is None:
        raise ValidationAppError("Unsupported image format. Use JPEG, PNG, or WebP.")

    expected_content_type = ALLOWED_IMAGE_TYPES[image_type]
    if declared_content_type and declared_content_type not in {expected_content_type, "image/jpg"}:
        raise ValidationAppError(
            "Image content type does not match file data",
            details={"declared": declared_content_type, "detected": expected_content_type},
        )

    return ValidatedImage(
        image_type=image_type,
        extension=EXTENSION_BY_TYPE[image_type],
        content_type=expected_content_type,
    )


class ShopPhotoStorage:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = get_storage_provider(settings)

    def save_shop_photo(
        self,
        *,
        shop_id,
        content: bytes,
        declared_content_type: str | None,
    ) -> tuple[str, str]:
        validated = validate_shop_photo(
            content,
            declared_content_type,
            self.settings.shop_photo_max_bytes,
        )
        storage_key = build_shop_photo_key(shop_id, validated.extension)
        saved_key = self.storage.save(relative_key=storage_key, content=content)
        prefix = self.settings.media_url_prefix.rstrip("/")
        photo_url = f"{prefix}/{saved_key.lstrip('/')}"
        return photo_url, saved_key

    def delete_storage_key(self, storage_key: str | None) -> None:
        if storage_key:
            self.storage.delete(storage_key)
