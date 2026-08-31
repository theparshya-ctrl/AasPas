from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.exceptions import ConflictError, NotFoundError
from aaspas.common.security.auth import CurrentUser
from aaspas.modules.customer.models import CustomerProfile
from aaspas.modules.customer.repository import CustomerRepository
from aaspas.modules.customer.schemas import (
    CustomerProfileCreate,
    CustomerProfileResponse,
    CustomerProfileUpdate,
)


class CustomerService:
    MODULE = "customer"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = CustomerRepository(db)

    def create_profile(self, user: CurrentUser, data: CustomerProfileCreate) -> CustomerProfileResponse:
        if self.repo.get_by_user_id(user.id):
            raise ConflictError("Customer profile already exists")

        profile = CustomerProfile(user_id=user.id, **data.model_dump())
        self.repo.create(profile)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.CREATE,
            resource_type="customer_profile",
            resource_id=str(profile.id),
            actor_id=user.id,
            actor_role=user.role.value,
        )
        self.db.commit()
        return CustomerProfileResponse.model_validate(profile)

    def get_my_profile(self, user: CurrentUser) -> CustomerProfileResponse:
        profile = self.repo.get_by_user_id(user.id)
        if profile is None:
            raise NotFoundError("Customer profile not found")
        return CustomerProfileResponse.model_validate(profile)

    def update_my_profile(self, user: CurrentUser, data: CustomerProfileUpdate) -> CustomerProfileResponse:
        profile = self.repo.get_by_user_id(user.id)
        if profile is None:
            raise NotFoundError("Customer profile not found")

        for key, value in data.model_dump(exclude_unset=True).items():
            setattr(profile, key, value)

        self.repo.save(profile)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.UPDATE,
            resource_type="customer_profile",
            resource_id=str(profile.id),
            actor_id=user.id,
            actor_role=user.role.value,
        )
        self.db.commit()
        return CustomerProfileResponse.model_validate(profile)
