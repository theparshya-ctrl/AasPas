"""Staging environment hides public OpenAPI docs."""

from fastapi.testclient import TestClient


def test_staging_hides_openapi_docs(monkeypatch):
    monkeypatch.setenv("APP_ENV", "staging")
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")

    from aaspas.config import get_settings

    get_settings.cache_clear()

    from aaspas.main import create_app

    client = TestClient(create_app())
    assert client.get("/health").status_code == 200
    assert client.get("/docs").status_code == 404
    assert client.get("/openapi.json").status_code == 404

    get_settings.cache_clear()


def test_development_exposes_openapi_docs(monkeypatch):
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")

    from aaspas.config import get_settings

    get_settings.cache_clear()

    from aaspas.main import create_app

    client = TestClient(create_app())
    assert client.get("/openapi.json").status_code == 200

    get_settings.cache_clear()
