import uuid
from datetime import UTC, datetime, timedelta
from typing import Annotated

import bcrypt
from fastapi import Depends, Header
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from pydantic import BaseModel
from sqlalchemy.orm import Session

from aaspas.common.exceptions import ForbiddenError, UnauthorizedError
from aaspas.common.security.rbac import Permission, UserRole, has_permission
from aaspas.config import get_settings
from aaspas.database import get_db

settings = get_settings()
bearer_scheme = HTTPBearer(auto_error=False)


class TokenPayload(BaseModel):
    sub: str
    role: UserRole
    shop_id: str | None = None
    exp: datetime
    typ: str = "access"


class RefreshTokenPayload(BaseModel):
    sub: str
    role: UserRole
    shop_id: str | None = None
    exp: datetime
    typ: str = "refresh"


class CurrentUser(BaseModel):
    id: uuid.UUID
    email: str
    role: UserRole
    shop_id: uuid.UUID | None = None
    is_active: bool = True


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def verify_password(plain: str, hashed: str) -> bool:
    return bcrypt.checkpw(plain.encode(), hashed.encode())


def create_access_token(
    *,
    user_id: uuid.UUID,
    role: UserRole,
    shop_id: uuid.UUID | None = None,
    expires_delta: timedelta | None = None,
) -> str:
    expire = datetime.now(UTC) + (
        expires_delta or timedelta(minutes=settings.access_token_expire_minutes)
    )
    payload = {
        "sub": str(user_id),
        "role": role.value,
        "shop_id": str(shop_id) if shop_id else None,
        "typ": "access",
        "exp": expire,
    }
    return jwt.encode(payload, settings.secret_key, algorithm="HS256")


def create_refresh_token(
    *,
    user_id: uuid.UUID,
    role: UserRole,
    shop_id: uuid.UUID | None = None,
    expires_delta: timedelta | None = None,
) -> str:
    expire = datetime.now(UTC) + (
        expires_delta or timedelta(days=settings.refresh_token_expire_days)
    )
    payload = {
        "sub": str(user_id),
        "role": role.value,
        "shop_id": str(shop_id) if shop_id else None,
        "typ": "refresh",
        "exp": expire,
    }
    return jwt.encode(payload, settings.secret_key, algorithm="HS256")


def decode_access_token(token: str) -> TokenPayload:
    try:
        data = jwt.decode(token, settings.secret_key, algorithms=["HS256"])
        if data.get("typ", "access") != "access":
            raise UnauthorizedError("Invalid or expired token")
        return TokenPayload(
            sub=data["sub"],
            role=UserRole(data["role"]),
            shop_id=data.get("shop_id"),
            exp=datetime.fromtimestamp(data["exp"], tz=UTC),
        )
    except (JWTError, KeyError, ValueError) as exc:
        raise UnauthorizedError("Invalid or expired token") from exc


def decode_refresh_token(token: str) -> RefreshTokenPayload:
    try:
        data = jwt.decode(token, settings.secret_key, algorithms=["HS256"])
        if data.get("typ") != "refresh":
            raise UnauthorizedError("Invalid or expired refresh token")
        return RefreshTokenPayload(
            sub=data["sub"],
            role=UserRole(data["role"]),
            shop_id=data.get("shop_id"),
            exp=datetime.fromtimestamp(data["exp"], tz=UTC),
        )
    except (JWTError, KeyError, ValueError) as exc:
        raise UnauthorizedError("Invalid or expired refresh token") from exc


async def get_current_user_optional(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)],
    db: Annotated[Session, Depends(get_db)],
) -> CurrentUser | None:
    if credentials is None:
        return None

    from aaspas.modules.auth.repository import AuthRepository

    payload = decode_access_token(credentials.credentials)
    repo = AuthRepository(db)
    user = repo.get_by_id(uuid.UUID(payload.sub))
    if user is None or not user.is_active:
        raise UnauthorizedError("User not found or inactive")

    return CurrentUser(
        id=user.id,
        email=user.email,
        role=UserRole(user.role),
        shop_id=user.shop_id,
        is_active=user.is_active,
    )


async def get_current_user(
    user: Annotated[CurrentUser | None, Depends(get_current_user_optional)],
) -> CurrentUser:
    if user is None:
        raise UnauthorizedError()
    return user


def require_permission(permission: Permission):
    async def _checker(user: Annotated[CurrentUser, Depends(get_current_user)]) -> CurrentUser:
        if not has_permission(user.role, permission):
            raise ForbiddenError(f"Missing permission: {permission.value}")
        return user

    return _checker


def require_roles(*roles: UserRole):
    async def _checker(user: Annotated[CurrentUser, Depends(get_current_user)]) -> CurrentUser:
        if user.role not in roles:
            raise ForbiddenError("Insufficient role")
        return user

    return _checker


def require_shop_access(shop_id: uuid.UUID):
    """Ensure shop owners/staff can only access their own shop data."""

    async def _checker(user: Annotated[CurrentUser, Depends(get_current_user)]) -> CurrentUser:
        if user.role in {UserRole.ADMIN, UserRole.SUPER_ADMIN}:
            return user
        if user.shop_id != shop_id:
            raise ForbiddenError("Cannot access another shop's data")
        return user

    return _checker


def get_request_id(x_request_id: Annotated[str | None, Header()] = None) -> str:
    return x_request_id or str(uuid.uuid4())
