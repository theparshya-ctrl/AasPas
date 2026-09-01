import uuid

from sqlalchemy import func, or_
from sqlalchemy.orm import Session

from aaspas.modules.auth.models import User


class AuthRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_id(self, user_id: uuid.UUID) -> User | None:
        return self.db.get(User, user_id)

    def get_by_email(self, email: str) -> User | None:
        return self.db.query(User).filter(User.email == email.lower()).one_or_none()

    def create(self, user: User) -> User:
        self.db.add(user)
        self.db.flush()
        return user

    def list_for_admin(
        self,
        *,
        role: str | None = None,
        search: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[User]:
        query = self.db.query(User).filter(User.is_system_account.is_(False))
        if role:
            query = query.filter(User.role == role)
        if search:
            term = f"%{search.strip().lower()}%"
            query = query.filter(
                or_(
                    func.lower(User.email).like(term),
                    func.lower(User.full_name).like(term),
                )
            )
        return (
            query.order_by(User.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )
