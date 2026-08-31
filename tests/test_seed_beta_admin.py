"""Beta admin seed script guards."""

import importlib.util
from pathlib import Path

import pytest


def _load_seed_main():
    path = Path(__file__).resolve().parent.parent / "scripts" / "seed_beta_admin.py"
    spec = importlib.util.spec_from_file_location("seed_beta_admin", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module.main()


def test_seed_beta_admin_requires_credentials(monkeypatch):
    monkeypatch.setenv("APP_ENV", "staging")
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
    monkeypatch.delenv("BETA_ADMIN_EMAIL", raising=False)
    monkeypatch.delenv("BETA_ADMIN_PASSWORD", raising=False)

    from aaspas.config import get_settings

    get_settings.cache_clear()
    assert _load_seed_main() == 1
    get_settings.cache_clear()


def test_seed_beta_admin_refuses_development(monkeypatch):
    monkeypatch.setenv("APP_ENV", "development")
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
    monkeypatch.setenv("BETA_ADMIN_EMAIL", "admin@test.com")
    monkeypatch.setenv("BETA_ADMIN_PASSWORD", "long-password-1")

    from aaspas.config import get_settings

    get_settings.cache_clear()
    assert _load_seed_main() == 1
    get_settings.cache_clear()
