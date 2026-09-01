"""Import external unverified offers into Beta PostgreSQL only."""

from __future__ import annotations

import re
import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.exceptions import ValidationAppError
from aaspas.common.source_type import SourceType
from aaspas.config import get_settings
from aaspas.modules.category.models import Category
from aaspas.modules.category.repository import CategoryRepository
from aaspas.modules.external.schemas import ExternalOfferImportRecord
from aaspas.modules.external.system_owner import get_or_create_external_data_owner
from aaspas.modules.external.validation import validate_for_import
from aaspas.modules.location.models import Location
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.repository import ShopRepository
from aaspas.modules.shop.status import ShopStatus


class ExternalOfferImportService:
    MODULE = "external"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.settings = get_settings()
        self.shop_repo = ShopRepository(db)
        self.offer_repo = OfferRepository(db)
        self.category_repo = CategoryRepository(db)

    @staticmethod
    def assert_beta_import_target() -> None:
        settings = get_settings()
        url = settings.database_url
        if url.startswith("sqlite") or "dev.db" in url.lower():
            raise ValidationAppError(
                "External import refused: DEV SQLite database detected",
            )
        if "localhost" in url or "127.0.0.1" in url:
            raise ValidationAppError(
                "External import refused: localhost database detected",
            )
        if settings.app_env == "development":
            raise ValidationAppError(
                "External import refused: APP_ENV=development",
            )

    def import_record(
        self,
        record: ExternalOfferImportRecord,
        *,
        now: datetime,
        allow_needs_review: bool = False,
    ) -> tuple[str, uuid.UUID]:
        active_categories = {category.name for category in self.category_repo.list_active()}
        validate_for_import(
            record,
            active_categories=active_categories,
            now=now,
            allow_needs_review=allow_needs_review,
        )
        owner = get_or_create_external_data_owner(self.db)
        shop = self._upsert_shop(record, owner_id=owner.id)
        offer, created = self._upsert_offer(record, shop=shop, now=now)
        self.db.commit()
        return ("created" if created else "updated", offer.id)

    def import_many(
        self,
        records: list[ExternalOfferImportRecord],
        *,
        now: datetime | None = None,
        allow_needs_review: bool = False,
    ) -> dict[str, int | list[str]]:
        aware_now = now or datetime.now(UTC)
        created = 0
        updated = 0
        skipped = 0
        errors: list[str] = []
        seen_keys: set[str] = set()
        for index, record in enumerate(records, start=1):
            if record.external_source_key in seen_keys:
                skipped += 1
                errors.append(f"row {index}: duplicate external_source_key in import file")
                continue
            seen_keys.add(record.external_source_key)
            try:
                action, _offer_id = self.import_record(
                    record,
                    now=aware_now,
                    allow_needs_review=allow_needs_review,
                )
                if action == "created":
                    created += 1
                else:
                    updated += 1
            except ValidationAppError as exc:
                skipped += 1
                errors.append(f"row {index}: {exc.message}")
            except Exception:
                self.db.rollback()
                skipped += 1
                errors.append(f"row {index}: unexpected error")
        return {
            "created": created,
            "updated": updated,
            "skipped": skipped,
            "errors": errors,
        }

    def _upsert_shop(self, record: ExternalOfferImportRecord, *, owner_id: uuid.UUID) -> Shop:
        shop_key = record.shop_external_source_key or self._default_shop_key(record)
        existing = (
            self.db.query(Shop).filter(Shop.external_source_key == shop_key).one_or_none()
        )
        category = self._resolve_category(record.category)
        slug = self._unique_shop_slug(record.shop_name, shop_key)
        if existing is None:
            shop = Shop(
                owner_id=owner_id,
                name=record.shop_name,
                slug=slug,
                category=record.category,
                category_id=category.id if category else None,
                status=ShopStatus.ACTIVE.value,
                source_type=SourceType.EXTERNAL.value,
                external_source_key=shop_key,
            )
            self.shop_repo.create(shop)
        else:
            shop = existing
            shop.name = record.shop_name
            shop.category = record.category
            shop.category_id = category.id if category else None
            shop.status = ShopStatus.ACTIVE.value
            shop.source_type = SourceType.EXTERNAL.value
            self.shop_repo.save(shop)

        self._upsert_primary_location(shop, record)
        self.db.flush()
        return shop

    def _upsert_primary_location(self, shop: Shop, record: ExternalOfferImportRecord) -> Location:
        location = (
            self.db.query(Location)
            .filter(Location.shop_id == shop.id, Location.is_primary.is_(True))
            .one_or_none()
        )
        address = record.address
        if location is None:
            location = Location(
                shop_id=shop.id,
                label="Primary",
                address_line1=address.address_line1,
                address_line2=address.address_line2,
                city=address.city,
                state=address.state,
                postal_code=address.postal_code,
                country=address.country,
                latitude=address.latitude,
                longitude=address.longitude,
                is_primary=True,
            )
            self.db.add(location)
        else:
            location.address_line1 = address.address_line1
            location.address_line2 = address.address_line2
            location.city = address.city
            location.state = address.state
            location.postal_code = address.postal_code
            location.country = address.country
            location.latitude = address.latitude
            location.longitude = address.longitude
        return location

    def _upsert_offer(
        self, record: ExternalOfferImportRecord, *, shop: Shop, now: datetime
    ) -> tuple[Offer, bool]:
        existing = (
            self.db.query(Offer)
            .filter(Offer.external_source_key == record.external_source_key)
            .one_or_none()
        )
        starts_at = self._optional_aware(record.starts_at) or self._optional_aware(record.collected_at)
        ends_at = self._optional_aware(record.ends_at)
        aware_now = self._ensure_aware(now)

        if ends_at is not None and ends_at <= aware_now:
            status = OfferStatus.EXPIRED.value
        elif starts_at is not None and starts_at > aware_now:
            status = OfferStatus.SCHEDULED.value
        else:
            status = OfferStatus.ACTIVE.value

        payload = {
            "shop_id": shop.id,
            "title": record.title.strip(),
            "description": record.description,
            "discount_type": record.discount_type,
            "discount_value": record.discount_value,
            "starts_at": starts_at,
            "ends_at": ends_at,
            "status": status,
            "source_type": SourceType.EXTERNAL.value,
            "source_name": record.source_name,
            "source_url": record.source_url,
            "collected_at": self._ensure_aware(record.collected_at),
            "external_source_key": record.external_source_key,
            "is_verified": False,
            "merchant_confirmed_at": None,
            "merchant_confirmed_by": None,
            "submitted_at": None,
            "approved_at": None,
            "rejected_at": None,
            "rejection_reason": None,
        }
        if existing is None:
            offer = Offer(**payload)
            self.offer_repo.create(offer)
            return offer, True

        for key, value in payload.items():
            setattr(existing, key, value)
        self.offer_repo.save(existing)
        return existing, False

    def _resolve_category(self, category_name: str) -> Category | None:
        normalized = category_name.strip().lower()
        for category in self.category_repo.list_active():
            if category.name.strip().lower() == normalized:
                return category
        return None

    @staticmethod
    def _default_shop_key(record: ExternalOfferImportRecord) -> str:
        city = record.address.city.strip().lower()
        name = record.shop_name.strip().lower()
        return f"shop:{city}:{name}"[:255]

    @staticmethod
    def _slugify(value: str) -> str:
        slug = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
        return slug[:200] or "external-shop"

    def _unique_shop_slug(self, shop_name: str, shop_key: str) -> str:
        base = self._slugify(f"{shop_name}-{shop_key[-12:]}")
        candidate = base
        suffix = 1
        while self.db.query(Shop.id).filter(Shop.slug == candidate).one_or_none() is not None:
            suffix += 1
            candidate = f"{base}-{suffix}"[:255]
        return candidate

    @staticmethod
    def _ensure_aware(dt: datetime) -> datetime:
        if dt.tzinfo is None:
            return dt.replace(tzinfo=UTC)
        return dt

    @staticmethod
    def _optional_aware(dt: datetime | None) -> datetime | None:
        if dt is None:
            return None
        if dt.tzinfo is None:
            return dt.replace(tzinfo=UTC)
        return dt
