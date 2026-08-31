"""Local and S3-compatible object storage for media uploads."""

from __future__ import annotations

import uuid
from abc import ABC, abstractmethod
from pathlib import Path

from aaspas.config import Settings


class StorageProvider(ABC):
    @abstractmethod
    def save(self, *, relative_key: str, content: bytes) -> str:
        """Persist bytes and return the relative storage key."""

    @abstractmethod
    def delete(self, relative_key: str) -> None:
        """Remove a stored object if it exists."""

    @abstractmethod
    def resolve_path(self, relative_key: str) -> Path:
        """Resolve a relative key to an on-disk path."""


class LocalFileStorage(StorageProvider):
    def __init__(self, root: str | Path) -> None:
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)

    def save(self, *, relative_key: str, content: bytes) -> str:
        safe_key = relative_key.replace("\\", "/").lstrip("/")
        target = self.root / safe_key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
        return safe_key

    def delete(self, relative_key: str) -> None:
        if not relative_key:
            return
        target = self.root / relative_key.replace("\\", "/").lstrip("/")
        if target.is_file():
            target.unlink()

    def resolve_path(self, relative_key: str) -> Path:
        return self.root / relative_key.replace("\\", "/").lstrip("/")


class S3ObjectStorage(StorageProvider):
    """S3-compatible storage (Neon Object Storage, R2, AWS S3, etc.) via boto3."""

    def __init__(
        self,
        *,
        endpoint_url: str | None,
        region_name: str,
        access_key_id: str | None,
        secret_access_key: str | None,
        bucket: str | None,
    ) -> None:
        if not endpoint_url or not access_key_id or not secret_access_key or not bucket:
            raise ValueError(
                "S3 storage requires s3_endpoint_url, s3_access_key_id, "
                "s3_secret_access_key, and s3_bucket_name"
            )
        self.bucket = bucket
        self._endpoint_url = endpoint_url
        import boto3

        self._client = boto3.client(
            "s3",
            region_name=region_name,
            endpoint_url=endpoint_url,
            aws_access_key_id=access_key_id,
            aws_secret_access_key=secret_access_key,
        )

    def save(self, *, relative_key: str, content: bytes) -> str:
        safe_key = relative_key.replace("\\", "/").lstrip("/")
        self._client.put_object(Bucket=self.bucket, Key=safe_key, Body=content)
        return safe_key

    def delete(self, relative_key: str) -> None:
        if not relative_key:
            return
        safe_key = relative_key.replace("\\", "/").lstrip("/")
        self._client.delete_object(Bucket=self.bucket, Key=safe_key)

    def resolve_path(self, relative_key: str) -> Path:
        raise NotImplementedError("S3 objects are not available on the local filesystem")


def _absolute_object_url(settings: Settings, storage_key: str) -> str | None:
    base = settings.media_public_base_url
    if not base:
        return None
    return f"{base.rstrip('/')}/{storage_key.lstrip('/')}"


def _uses_absolute_media_urls(settings: Settings) -> bool:
    return settings.media_storage_backend.lower() == "s3"


def build_shop_photo_key(shop_id: uuid.UUID, extension: str) -> str:
    ext = extension.lower().lstrip(".")
    return f"shops/{shop_id}/{uuid.uuid4()}.{ext}"


def build_offer_photo_key(shop_id: uuid.UUID, offer_id: uuid.UUID, extension: str) -> str:
    ext = extension.lower().lstrip(".")
    return f"offers/{shop_id}/{offer_id}/{uuid.uuid4()}.{ext}"


def resolve_media_photo_url(
    settings: Settings,
    *,
    photo_url: str | None,
    photo_storage_key: str | None,
) -> str | None:
    """Build a reachable photo URL from a storage key or legacy URL."""
    if photo_storage_key:
        if _uses_absolute_media_urls(settings):
            return _absolute_object_url(settings, photo_storage_key)
        base = settings.media_public_base_url
        if base:
            return build_public_media_url(base, settings.media_url_prefix, photo_storage_key)
        prefix = settings.media_url_prefix.rstrip("/")
        key = photo_storage_key.lstrip("/")
        return f"{prefix}/{key}"
    return photo_url


def build_public_media_url(base_url: str, media_prefix: str, storage_key: str) -> str:
    prefix = media_prefix.rstrip("/")
    key = storage_key.lstrip("/")
    return f"{base_url.rstrip('/')}{prefix}/{key}"


def resolve_shop_photo_url(
    settings: Settings,
    *,
    photo_url: str | None,
    photo_storage_key: str | None,
) -> str | None:
    """Build a merchant/admin photo URL from the storage key (may use public base)."""
    return resolve_media_photo_url(
        settings,
        photo_url=photo_url,
        photo_storage_key=photo_storage_key,
    )


def resolve_customer_media_photo_url(
    settings: Settings,
    *,
    photo_url: str | None,
    photo_storage_key: str | None,
) -> str | None:
    """Build a relative /media/... URL for customer apps (Android resolves the host)."""
    if photo_storage_key:
        if _uses_absolute_media_urls(settings):
            return _absolute_object_url(settings, photo_storage_key)
        prefix = settings.media_url_prefix.rstrip("/")
        key = photo_storage_key.lstrip("/")
        return f"{prefix}/{key}"
    if photo_url and photo_url.startswith("/"):
        return photo_url
    return photo_url


def resolve_customer_shop_photo_url(
    settings: Settings,
    *,
    photo_url: str | None,
    photo_storage_key: str | None,
) -> str | None:
    """Customer-facing shop photo URL (always relative when stored locally)."""
    return resolve_customer_media_photo_url(
        settings,
        photo_url=photo_url,
        photo_storage_key=photo_storage_key,
    )


def resolve_customer_offer_photo_url(
    settings: Settings,
    *,
    offer_photo_url: str | None,
    offer_photo_storage_key: str | None,
    shop_photo_url: str | None,
    shop_photo_storage_key: str | None,
) -> str | None:
    """Prefer uploaded offer photo; fall back to shop photo for customer display."""
    offer_photo = resolve_customer_media_photo_url(
        settings,
        photo_url=offer_photo_url,
        photo_storage_key=offer_photo_storage_key,
    )
    if offer_photo:
        return offer_photo
    return resolve_customer_shop_photo_url(
        settings,
        photo_url=shop_photo_url,
        photo_storage_key=shop_photo_storage_key,
    )
