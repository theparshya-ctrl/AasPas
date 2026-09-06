from __future__ import annotations

import json
from pathlib import Path

from fastapi import HTTPException, status

from aaspas.config import Settings, get_settings
from aaspas.modules.app.schemas import AppVersionInfo

_REPO_ROOT = Path(__file__).resolve().parents[4]
_DEFAULT_VERSION_FILE = _REPO_ROOT / "deploy" / "beta" / "app_version.json"


def _version_metadata_path() -> Path:
    settings = get_settings()
    configured = settings.beta_app_version_file
    if configured:
        return Path(configured)
    return _DEFAULT_VERSION_FILE


def _load_version_metadata() -> dict:
    path = _version_metadata_path()
    if not path.is_file():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta app version metadata is not available",
        )
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta app version metadata is invalid",
        ) from exc
    if not isinstance(payload, dict):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta app version metadata is invalid",
        )
    return payload


def resolve_beta_apk_download_url(settings: Settings) -> str | None:
    if settings.beta_apk_download_url:
        url = settings.beta_apk_download_url.strip()
        if url.startswith("https://"):
            return url
        return None
    base = settings.media_public_base_url
    if not base:
        return None
    key = settings.beta_apk_storage_key.strip().lstrip("/")
    if not key:
        return None
    return f"{base.rstrip('/')}/{key}"


def get_beta_app_version() -> AppVersionInfo:
    settings = get_settings()
    if settings.app_env == "development":
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")

    metadata = _load_version_metadata()
    download_url = resolve_beta_apk_download_url(settings)
    if not download_url:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta APK download URL is not configured",
        )

    try:
        latest_version_name = str(metadata["latest_version_name"]).strip()
        latest_version_code = int(metadata["latest_version_code"])
        mandatory = bool(metadata.get("mandatory", False))
        release_notes_raw = metadata.get("release_notes", [])
        release_notes = (
            [str(note).strip() for note in release_notes_raw if str(note).strip()]
            if isinstance(release_notes_raw, list)
            else []
        )
    except (KeyError, TypeError, ValueError) as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta app version metadata is invalid",
        ) from exc

    if not latest_version_name or latest_version_code < 1:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Beta app version metadata is invalid",
        )

    return AppVersionInfo(
        latest_version_name=latest_version_name,
        latest_version_code=latest_version_code,
        download_url=download_url,
        mandatory=mandatory,
        release_notes=release_notes,
    )
