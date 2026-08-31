import uuid
from abc import ABC, abstractmethod

from sqlalchemy.orm import Session

from aaspas.modules.customer.models import CustomerProfile


class CustomerReader(ABC):
    """Public interface for other modules — use instead of direct DB access."""

    @abstractmethod
    def get_by_user_id(self, user_id: uuid.UUID) -> CustomerProfile | None: ...


class CustomerRepository(CustomerReader):
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_user_id(self, user_id: uuid.UUID) -> CustomerProfile | None:
        return self.db.query(CustomerProfile).filter(CustomerProfile.user_id == user_id).one_or_none()

    def get_by_id(self, profile_id: uuid.UUID) -> CustomerProfile | None:
        return self.db.get(CustomerProfile, profile_id)

    def create(self, profile: CustomerProfile) -> CustomerProfile:
        self.db.add(profile)
        self.db.flush()
        return profile

    def save(self, profile: CustomerProfile) -> CustomerProfile:
        self.db.add(profile)
        self.db.flush()
        return profile
