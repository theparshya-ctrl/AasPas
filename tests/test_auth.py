import uuid
from datetime import UTC, datetime, timedelta

import pytest
from fastapi.testclient import TestClient

from aaspas.common.security.auth import (
    create_access_token,
    create_refresh_token,
    decode_access_token,
    decode_refresh_token,
    hash_password,
    verify_password,
)
from aaspas.common.security.rbac import UserRole
from aaspas.config import get_settings


def test_password_hashing():
    hashed = hash_password("secret123")
    assert verify_password("secret123", hashed)
    assert not verify_password("wrong", hashed)


def test_jwt_roundtrip():
    user_id = uuid.UUID("550e8400-e29b-41d4-a716-446655440000")
    token = create_access_token(user_id=user_id, role=UserRole.CUSTOMER)
    payload = decode_access_token(token)
    assert payload.sub == str(user_id)
    assert payload.role == UserRole.CUSTOMER
    assert payload.typ == "access"


def test_refresh_token_roundtrip():
    user_id = uuid.UUID("550e8400-e29b-41d4-a716-446655440000")
    token = create_refresh_token(user_id=user_id, role=UserRole.CUSTOMER)
    payload = decode_refresh_token(token)
    assert payload.sub == str(user_id)
    assert payload.role == UserRole.CUSTOMER
    assert payload.typ == "refresh"


def test_access_token_rejects_refresh_type():
    user_id = uuid.UUID("550e8400-e29b-41d4-a716-446655440000")
    refresh = create_refresh_token(user_id=user_id, role=UserRole.CUSTOMER)
    with pytest.raises(Exception):
        decode_access_token(refresh)


def test_login_returns_refresh_token(client: TestClient):
    client.post(
        "/api/v1/auth/register",
        json={
            "email": "refresh-login@example.com",
            "password": "password123",
            "role": "customer",
        },
    )
    login = client.post(
        "/api/v1/auth/login",
        json={"email": "refresh-login@example.com", "password": "password123"},
    )
    assert login.status_code == 200
    data = login.json()["data"]
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["refresh_expires_in"] > data["expires_in"]


def test_refresh_returns_new_access_token(client: TestClient):
    client.post(
        "/api/v1/auth/register",
        json={
            "email": "refresh-flow@example.com",
            "password": "password123",
            "role": "customer",
        },
    )
    login = client.post(
        "/api/v1/auth/login",
        json={"email": "refresh-flow@example.com", "password": "password123"},
    ).json()["data"]
    refresh = client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": login["refresh_token"]},
    )
    assert refresh.status_code == 200
    refreshed = refresh.json()["data"]
    assert refreshed["access_token"]
    assert refreshed["refresh_token"] == login["refresh_token"]
    assert refreshed["expires_in"] > 0

    me = client.get(
        "/api/v1/auth/me",
        headers={"Authorization": f"Bearer {refreshed['access_token']}"},
    )
    assert me.status_code == 200


def test_refresh_invalid_token_rejected(client: TestClient):
    response = client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": "not-a-valid-token"},
    )
    assert response.status_code == 401


def test_expired_refresh_token_rejected(client: TestClient, monkeypatch):
    settings = get_settings()
    user_id = uuid.UUID("550e8400-e29b-41d4-a716-446655440000")
    expired = create_refresh_token(
        user_id=user_id,
        role=UserRole.CUSTOMER,
        expires_delta=timedelta(seconds=-30),
    )
    response = client.post("/api/v1/auth/refresh", json={"refresh_token": expired})
    assert response.status_code == 401
    assert settings.refresh_token_expire_days > 0


def test_expired_access_token_rejected(client: TestClient):
    client.post(
        "/api/v1/auth/register",
        json={
            "email": "expired-access@example.com",
            "password": "password123",
            "role": "customer",
        },
    )
    user_id = uuid.uuid4()
    expired_access = create_access_token(
        user_id=user_id,
        role=UserRole.CUSTOMER,
        expires_delta=timedelta(seconds=-30),
    )
    response = client.get(
        "/api/v1/auth/me",
        headers={"Authorization": f"Bearer {expired_access}"},
    )
    assert response.status_code == 401
