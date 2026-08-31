"""Storage backend selection — local filesystem (DEV) or S3-compatible (Beta/Neon)."""

from __future__ import annotations

from aaspas.common.storage import LocalFileStorage, S3ObjectStorage, StorageProvider
from aaspas.config import Settings


def get_storage_provider(settings: Settings) -> StorageProvider:
    backend = settings.media_storage_backend.lower()
    if backend == "local":
        return LocalFileStorage(settings.media_root)
    if backend == "s3":
        return S3ObjectStorage(
            endpoint_url=settings.s3_endpoint_url,
            region_name=settings.s3_region,
            access_key_id=settings.s3_access_key_id,
            secret_access_key=settings.s3_secret_access_key,
            bucket=settings.s3_bucket_name,
        )
    raise ValueError(f"Unsupported media_storage_backend: {settings.media_storage_backend!r}")


def is_local_media_storage(settings: Settings) -> bool:
    return settings.media_storage_backend.lower() == "local"
