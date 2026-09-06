import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.exceptions import ConflictError, UnauthorizedError, ValidationAppError
from aaspas.common.security.auth import (
    create_access_token,
    create_refresh_token,
    decode_refresh_token,
    hash_password,
    verify_password,
)
from aaspas.common.security.rbac import UserRole
from aaspas.config import get_settings
from aaspas.modules.external.constants import EXTERNAL_DATA_OWNER_EMAIL
from aaspas.modules.auth.models import User
from aaspas.modules.auth.repository import AuthRepository
from aaspas.modules.auth.schemas import (
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
    TokenResponse,
    UserResponse,
)

settings = get_settings()


class AuthService:
    MODULE = "auth"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = AuthRepository(db)

    def register(self, data: RegisterRequest) -> UserResponse:
        if data.role not in {UserRole.CUSTOMER, UserRole.SHOP_OWNER}:
            raise ValidationAppError("Registration limited to customer and shop_owner roles")

        if data.email.lower() == EXTERNAL_DATA_OWNER_EMAIL:
            raise ValidationAppError("Email is reserved for system use")

        if self.repo.get_by_email(data.email):
            raise ConflictError("Email already registered")

        user = User(
            email=data.email.lower(),
            password_hash=hash_password(data.password),
            full_name=data.full_name,
            role=data.role.value,
        )
        self.repo.create(user)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.CREATE,
            resource_type="user",
            resource_id=str(user.id),
            actor_id=user.id,
            actor_role=user.role,
            message="User registered",
        )
        self.db.commit()
        return UserResponse.model_validate(user)

    def login(self, data: LoginRequest) -> TokenResponse:
        user = self.repo.get_by_email(data.email)
        if user is None or not verify_password(data.password, user.password_hash):
            raise UnauthorizedError("Invalid email or password")
        if not user.is_active:
            raise UnauthorizedError("Account is inactive")

        token = create_access_token(
            user_id=user.id,
            role=UserRole(user.role),
            shop_id=user.shop_id,
        )
        refresh_token = create_refresh_token(
            user_id=user.id,
            role=UserRole(user.role),
            shop_id=user.shop_id,
        )
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.LOGIN,
            resource_type="user",
            resource_id=str(user.id),
            actor_id=user.id,
            actor_role=user.role,
        )
        self.db.commit()
        return TokenResponse(
            access_token=token,
            expires_in=settings.access_token_expire_minutes * 60,
            refresh_token=refresh_token,
            refresh_expires_in=settings.refresh_token_expire_days * 24 * 60 * 60,
        )

    def refresh(self, data: RefreshRequest) -> TokenResponse:
        payload = decode_refresh_token(data.refresh_token)
        user = self.repo.get_by_id(uuid.UUID(payload.sub))
        if user is None or not user.is_active:
            raise UnauthorizedError("User not found or inactive")
        if UserRole(user.role) != payload.role:
            raise UnauthorizedError("Invalid or expired refresh token")

        access_token = create_access_token(
            user_id=user.id,
            role=UserRole(user.role),
            shop_id=user.shop_id,
        )
        return TokenResponse(
            access_token=access_token,
            expires_in=settings.access_token_expire_minutes * 60,
            refresh_token=data.refresh_token,
            refresh_expires_in=max(
                0,
                int((payload.exp - datetime.now(UTC)).total_seconds()),
            ),
        )

    def get_profile(self, user_id) -> UserResponse:
        user = self.repo.get_by_id(user_id)
        if user is None:
            raise UnauthorizedError("User not found")
        return UserResponse.model_validate(user)
