"""Ensure the inactive system owner used by external listing shops exists."""

from __future__ import annotations

import secrets

from sqlalchemy.orm import Session

from aaspas.common.security.auth import hash_password
from aaspas.common.security.rbac import UserRole
from aaspas.modules.auth.models import User
from aaspas.modules.external.constants import (
    EXTERNAL_DATA_OWNER_EMAIL,
    EXTERNAL_DATA_OWNER_FULL_NAME,
)


def get_or_create_external_data_owner(db: Session) -> User:
    user = db.query(User).filter(User.email == EXTERNAL_DATA_OWNER_EMAIL).one_or_none()
    if user is not None:
        if not user.is_system_account:
            user.is_system_account = True
            user.is_active = False
            user.full_name = EXTERNAL_DATA_OWNER_FULL_NAME
            db.add(user)
            db.flush()
        return user

    user = User(
        email=EXTERNAL_DATA_OWNER_EMAIL,
        password_hash=hash_password(secrets.token_urlsafe(48)),
        full_name=EXTERNAL_DATA_OWNER_FULL_NAME,
        role=UserRole.SHOP_OWNER.value,
        is_active=False,
        is_system_account=True,
    )
    db.add(user)
    db.flush()
    return user


def is_external_data_owner(user: User) -> bool:
    return bool(user.is_system_account) or user.email == EXTERNAL_DATA_OWNER_EMAIL
