import uuid

from aaspas.common.security.auth import (
    create_access_token,
    decode_access_token,
    hash_password,
    verify_password,
)
from aaspas.common.security.rbac import UserRole


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
