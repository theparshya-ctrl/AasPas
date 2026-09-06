import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def version_file(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Path:
    path = tmp_path / "app_version.json"
    path.write_text(
        json.dumps(
            {
                "latest_version_name": "0.1.3",
                "latest_version_code": 6,
                "mandatory": False,
                "release_notes": ["Improved session persistence", "Performance improvements"],
            }
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("APP_ENV", "staging")
    monkeypatch.setenv("BETA_APP_VERSION_FILE", str(path))
    monkeypatch.setenv("MEDIA_PUBLIC_BASE_URL", "https://cdn.example/bucket")
    from aaspas.config import get_settings

    get_settings.cache_clear()
    return path


def test_app_version_returns_metadata(client: TestClient, version_file: Path):
    response = client.get("/api/v1/app/version")
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    data = body["data"]
    assert data["latest_version_name"] == "0.1.3"
    assert data["latest_version_code"] == 6
    assert data["download_url"] == "https://cdn.example/bucket/beta/android/latest.apk"
    assert data["mandatory"] is False
    assert len(data["release_notes"]) == 2


def test_app_version_requires_no_auth(client: TestClient, version_file: Path):
    response = client.get("/api/v1/app/version")
    assert response.status_code == 200
    assert response.json()["success"] is True


def test_app_version_hidden_in_development(client: TestClient, version_file: Path, monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("APP_ENV", "development")
    from aaspas.config import get_settings

    get_settings.cache_clear()
    response = client.get("/api/v1/app/version")
    assert response.status_code == 404


def test_app_version_invalid_metadata(client: TestClient, tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    bad = tmp_path / "bad.json"
    bad.write_text('{"latest_version_name": ""}', encoding="utf-8")
    monkeypatch.setenv("APP_ENV", "staging")
    monkeypatch.setenv("BETA_APP_VERSION_FILE", str(bad))
    monkeypatch.setenv("MEDIA_PUBLIC_BASE_URL", "https://cdn.example/bucket")
    from aaspas.config import get_settings

    get_settings.cache_clear()
    response = client.get("/api/v1/app/version")
    assert response.status_code == 503


def test_app_version_missing_download_config(client: TestClient, version_file: Path, monkeypatch: pytest.MonkeyPatch):
    monkeypatch.delenv("MEDIA_PUBLIC_BASE_URL", raising=False)
    monkeypatch.delenv("BETA_APK_DOWNLOAD_URL", raising=False)
    from aaspas.config import get_settings

    get_settings.cache_clear()
    response = client.get("/api/v1/app/version")
    assert response.status_code == 503
