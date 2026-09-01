import uuid
from datetime import datetime

from sqlalchemy import and_, or_
from sqlalchemy.orm import Session

from aaspas.common.source_type import SourceType
from aaspas.modules.category.models import Category
from aaspas.modules.location.models import Location
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus


class OfferRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_id(self, offer_id: uuid.UUID) -> Offer | None:
        return self.db.get(Offer, offer_id)

    def list_by_shop(self, shop_id: uuid.UUID) -> list[Offer]:
        return self.db.query(Offer).filter(Offer.shop_id == shop_id).all()

    def count_by_status(self, shop_id: uuid.UUID) -> dict[str, int]:
        from sqlalchemy import func

        counts = {status.value: 0 for status in OfferStatus}
        rows = (
            self.db.query(Offer.status, func.count(Offer.id))
            .filter(Offer.shop_id == shop_id)
            .group_by(Offer.status)
            .all()
        )
        for status, count in rows:
            counts[status] = int(count)
        return counts

    def count_by_status_global(self, status: OfferStatus) -> int:
        from sqlalchemy import func

        return (
            self.db.query(func.count(Offer.id))
            .filter(Offer.status == status.value)
            .scalar()
            or 0
        )

    def count_for_shop(self, shop_id: uuid.UUID) -> int:
        from sqlalchemy import func

        return (
            self.db.query(func.count(Offer.id))
            .filter(Offer.shop_id == shop_id)
            .scalar()
            or 0
        )

    def list_pending_for_admin(
        self, limit: int = 50, offset: int = 0
    ) -> list[tuple[Offer, Shop, "User", Category | None]]:
        from aaspas.modules.auth.models import User

        return (
            self.db.query(Offer, Shop, User, Category)
            .join(Shop, Offer.shop_id == Shop.id)
            .join(User, Shop.owner_id == User.id)
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Offer.status == OfferStatus.PENDING_APPROVAL.value)
            .order_by(Offer.submitted_at.asc(), Offer.created_at.asc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def get_pending_for_admin(
        self, offer_id: uuid.UUID
    ) -> tuple[Offer, Shop, "User", Category | None] | None:
        from aaspas.modules.auth.models import User

        return (
            self.db.query(Offer, Shop, User, Category)
            .join(Shop, Offer.shop_id == Shop.id)
            .join(User, Shop.owner_id == User.id)
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Offer.id == offer_id)
            .one_or_none()
        )

    def list_active(self, limit: int = 50) -> list[Offer]:
        return (
            self.db.query(Offer)
            .filter(Offer.status == OfferStatus.ACTIVE.value)
            .order_by(Offer.created_at.desc())
            .limit(limit)
            .all()
        )

    def _customer_base_query(self, now: datetime):
        standard_dated = and_(
            Offer.source_type == SourceType.AASPAS.value,
            Offer.ends_at.isnot(None),
            Offer.ends_at > now,
            Offer.starts_at.isnot(None),
        )
        external_visible = and_(
            Offer.source_type == SourceType.EXTERNAL.value,
            Offer.status.in_([s.value for s in OfferStatus.customer_approved()]),
            or_(Offer.ends_at.is_(None), Offer.ends_at > now),
        )
        return (
            self.db.query(Offer, Shop, Location, Category)
            .join(Shop, Offer.shop_id == Shop.id)
            .join(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Shop.status == ShopStatus.ACTIVE.value)
            .filter(or_(standard_dated, external_visible))
        )

    def get_customer_offer_context(
        self, offer_id: uuid.UUID
    ) -> tuple[Offer, Shop, Location, Category | None] | None:
        return (
            self.db.query(Offer, Shop, Location, Category)
            .join(Shop, Offer.shop_id == Shop.id)
            .join(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Offer.id == offer_id)
            .one_or_none()
        )

    def list_customer_offers_for_shop(
        self, shop_id: uuid.UUID, now: datetime
    ) -> list[tuple[Offer, Shop, Location, Category | None]]:
        return (
            self._customer_base_query(now)
            .filter(Shop.id == shop_id)
            .order_by(Offer.starts_at.asc())
            .all()
        )

    def search_customer_offers(
        self,
        now: datetime,
        *,
        q: str | None = None,
        category_id: uuid.UUID | None = None,
        latitude: float | None = None,
        longitude: float | None = None,
        radius_km: float | None = None,
    ) -> list[tuple[Offer, Shop, Location, Category | None]]:
        from aaspas.common.geo import bounding_box

        query = self._customer_base_query(now)

        if category_id is not None:
            query = query.filter(Category.id == category_id)

        if q:
            pattern = f"%{q}%"
            query = query.filter(
                or_(
                    Offer.title.ilike(pattern),
                    Offer.description.ilike(pattern),
                    Shop.name.ilike(pattern),
                    Category.name.ilike(pattern),
                )
            )

        if latitude is not None and longitude is not None and radius_km is not None:
            min_lat, max_lat, min_lon, max_lon = bounding_box(latitude, longitude, radius_km)
            query = query.filter(
                Location.latitude.isnot(None),
                Location.longitude.isnot(None),
                Location.latitude >= min_lat,
                Location.latitude <= max_lat,
                Location.longitude >= min_lon,
                Location.longitude <= max_lon,
            )

        return query.order_by(Offer.starts_at.desc()).all()

    def list_customer_offers(
        self,
        now: datetime,
        *,
        coming_soon: bool,
        limit: int = 20,
        category_slug: str | None = None,
    ) -> list[tuple[Offer, Shop, Location, Category | None]]:
        query = self._customer_base_query(now)
        if coming_soon:
            query = query.filter(Offer.starts_at.isnot(None), Offer.starts_at > now)
        else:
            query = query.filter(
                or_(
                    and_(Offer.starts_at.isnot(None), Offer.starts_at <= now),
                    and_(
                        Offer.source_type == SourceType.EXTERNAL.value,
                        Offer.starts_at.is_(None),
                    ),
                )
            )

        if category_slug:
            query = query.filter(Category.slug == category_slug)

        return (
            query.order_by(Offer.starts_at.asc() if coming_soon else Offer.starts_at.desc())
            .limit(limit)
            .all()
        )

    def create(self, offer: Offer) -> Offer:
        self.db.add(offer)
        self.db.flush()
        return offer

    def save(self, offer: Offer) -> Offer:
        self.db.add(offer)
        self.db.flush()
        return offer
