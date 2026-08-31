import uuid
from abc import ABC, abstractmethod

from sqlalchemy import and_, func, or_
from sqlalchemy.orm import Session

from aaspas.modules.category.models import Category
from aaspas.modules.location.models import Location
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus


class ShopReader(ABC):
    @abstractmethod
    def get_by_id(self, shop_id: uuid.UUID) -> Shop | None: ...


class ShopRepository(ShopReader):
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_id(self, shop_id: uuid.UUID) -> Shop | None:
        return self.db.get(Shop, shop_id)

    def get_active_with_context(
        self, shop_id: uuid.UUID
    ) -> tuple[Shop, Location, Category | None] | None:
        row = (
            self.db.query(Shop, Location, Category)
            .join(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Shop.id == shop_id)
            .filter(Shop.status == ShopStatus.ACTIVE.value)
            .one_or_none()
        )
        return row

    def get_by_slug(self, slug: str) -> Shop | None:
        return self.db.query(Shop).filter(Shop.slug == slug).one_or_none()

    def list_by_owner(self, owner_id: uuid.UUID) -> list[Shop]:
        return (
            self.db.query(Shop)
            .filter(Shop.owner_id == owner_id)
            .order_by(Shop.created_at.desc())
            .all()
        )

    def list_pending_with_owner(
        self, limit: int = 100, offset: int = 0
    ) -> list[tuple[Shop, "User", Location | None]]:
        from aaspas.modules.auth.models import User

        rows = (
            self.db.query(Shop, User, Location)
            .join(User, Shop.owner_id == User.id)
            .outerjoin(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .filter(Shop.status == ShopStatus.PENDING_APPROVAL.value)
            .order_by(Shop.submitted_at.asc(), Shop.created_at.asc())
            .offset(offset)
            .limit(limit)
            .all()
        )
        return rows

    def get_with_owner(
        self, shop_id: uuid.UUID
    ) -> tuple[Shop, "User", Location | None] | None:
        from aaspas.modules.auth.models import User

        return (
            self.db.query(Shop, User, Location)
            .join(User, Shop.owner_id == User.id)
            .outerjoin(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .filter(Shop.id == shop_id)
            .one_or_none()
        )

    def list_by_status(self, status: ShopStatus | str, limit: int = 100, offset: int = 0) -> list[Shop]:
        return (
            self.db.query(Shop)
            .filter(Shop.status == str(status))
            .order_by(Shop.submitted_at.asc(), Shop.created_at.asc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def count_by_status(self, status: ShopStatus | str) -> int:
        return self.db.query(Shop).filter(Shop.status == str(status)).count()

    def list_for_admin(
        self,
        *,
        status: str | None = None,
        search: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[tuple[Shop, "User", Location | None]]:
        from aaspas.modules.auth.models import User

        query = (
            self.db.query(Shop, User, Location)
            .join(User, Shop.owner_id == User.id)
            .outerjoin(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
        )
        if status:
            query = query.filter(Shop.status == status)
        if search:
            term = f"%{search.strip().lower()}%"
            query = query.filter(
                or_(
                    func.lower(Shop.name).like(term),
                    func.lower(User.full_name).like(term),
                    func.lower(User.email).like(term),
                )
            )
        return (
            query.order_by(Shop.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def create(self, shop: Shop) -> Shop:
        self.db.add(shop)
        self.db.flush()
        return shop

    def save(self, shop: Shop) -> Shop:
        self.db.add(shop)
        self.db.flush()
        return shop
