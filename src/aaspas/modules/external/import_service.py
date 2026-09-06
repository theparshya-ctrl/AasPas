"""Import external unverified offers into Beta PostgreSQL only."""

from __future__ import annotations

import re
import uuid
from datetime import UTC, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.exceptions import ValidationAppError
from aaspas.common.source_type import SourceType
from aaspas.config import get_settings
from aaspas.modules.category.models import Category
from aaspas.modules.category.repository import CategoryRepository
from aaspas.modules.external.report import (
    ExternalImportRowOutcome,
    ExternalImportRowResult,
    ExternalOfferImportReport,
    ExternalStaleCandidate,
)
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
        offer, action = self._upsert_offer(record, shop=shop, now=now)
        self.db.commit()
        return action, offer.id

    def import_many(
        self,
        records: list[ExternalOfferImportRecord],
        *,
        now: datetime | None = None,
        allow_needs_review: bool = False,
        source_file: str | None = None,
        mode: str = "import",
        include_stale_report: bool = False,
    ) -> ExternalOfferImportReport:
        aware_now = now or datetime.now(UTC)
        report = ExternalOfferImportReport(
            run_at=aware_now,
            source_file=source_file,
            mode=mode,
            candidates=len(records),
        )
        seen_keys: set[str] = set()
        imported_keys: set[str] = set()

        for index, record in enumerate(records, start=1):
            if record.external_source_key in seen_keys:
                report.apply_row(
                    ExternalImportRowResult(
                        row=index,
                        external_source_key=record.external_source_key,
                        outcome=ExternalImportRowOutcome.DUPLICATE_IN_FILE,
                        message="duplicate external_source_key in import file",
                    )
                )
                continue
            seen_keys.add(record.external_source_key)

            try:
                action, offer_id = self.import_record(
                    record,
                    now=aware_now,
                    allow_needs_review=allow_needs_review,
                )
                imported_keys.add(record.external_source_key)
                outcome = ExternalImportRowOutcome(action)
                report.apply_row(
                    ExternalImportRowResult(
                        row=index,
                        external_source_key=record.external_source_key,
                        outcome=outcome,
                        offer_id=str(offer_id),
                    )
                )
            except ValidationAppError as exc:
                report.apply_row(
                    ExternalImportRowResult(
                        row=index,
                        external_source_key=record.external_source_key,
                        outcome=ExternalImportRowOutcome.REJECTED,
                        message=exc.message,
                    )
                )
            except Exception:
                self.db.rollback()
                report.apply_row(
                    ExternalImportRowResult(
                        row=index,
                        external_source_key=record.external_source_key,
                        outcome=ExternalImportRowOutcome.ERROR,
                        message="unexpected error",
                    )
                )

        if include_stale_report:
            report.stale_candidates = self.find_stale_external_offers(
                input_keys=imported_keys,
                now=aware_now,
            )

        self._record_run_audit(report)
        self.db.commit()
        return report

    def find_stale_external_offers(
        self,
        *,
        input_keys: set[str],
        now: datetime,
    ) -> list[ExternalStaleCandidate]:
        """Active external offers missing from the latest collection file (report only)."""
        aware_now = self._ensure_aware(now)
        stale: list[ExternalStaleCandidate] = []
        offers = (
            self.db.query(Offer)
            .filter(
                Offer.source_type == SourceType.EXTERNAL.value,
                Offer.external_source_key.isnot(None),
            )
            .all()
        )
        for offer in offers:
            key = offer.external_source_key or ""
            if not key or key in input_keys:
                continue
            if offer.status == OfferStatus.EXPIRED.value:
                continue
            if offer.ends_at is not None and self._ensure_aware(offer.ends_at) <= aware_now:
                continue
            shop = self.db.get(Shop, offer.shop_id)
            stale.append(
                ExternalStaleCandidate(
                    external_source_key=key,
                    offer_id=str(offer.id),
                    title=offer.title,
                    shop_name=shop.name if shop else None,
                    last_collected_at=offer.collected_at,
                    status=offer.status,
                )
            )
        return stale

    def deactivate_external_offer(
        self,
        external_source_key: str,
        *,
        reason: str,
        now: datetime | None = None,
        source_verification_url: str | None = None,
    ) -> tuple[Offer, str]:
        """Mark an EXTERNAL offer expired without setting ends_at (record preserved).

        Used after manual stale review when the public source no longer supports the offer
        but does not publish an explicit end date (e.g. vouchers sold out).
        """
        if not reason.strip():
            raise ValidationAppError("Deactivation reason is required")

        offer = (
            self.db.query(Offer)
            .filter(Offer.external_source_key == external_source_key)
            .one_or_none()
        )
        if offer is None:
            raise ValidationAppError(
                f"External offer not found: {external_source_key}",
                details={"external_source_key": external_source_key},
            )
        if offer.source_type != SourceType.EXTERNAL.value:
            raise ValidationAppError("Only EXTERNAL offers can be deactivated via this workflow")
        if offer.is_verified:
            raise ValidationAppError("Cannot deactivate verified offers via external workflow")

        previous_status = offer.status
        if previous_status == OfferStatus.EXPIRED.value:
            return offer, "unchanged"

        offer.status = OfferStatus.EXPIRED.value
        self.offer_repo.save(offer)
        aware_now = self._ensure_aware(now or datetime.now(UTC))
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.ADMIN,
            resource_type="external_offer_deactivation",
            resource_id=str(offer.id),
            message=reason.strip(),
            metadata={
                "external_source_key": external_source_key,
                "previous_status": previous_status,
                "new_status": OfferStatus.EXPIRED.value,
                "ends_at": offer.ends_at.isoformat() if offer.ends_at else None,
                "source_verification_url": source_verification_url,
                "deactivated_at": aware_now.isoformat(),
            },
        )
        self.db.commit()
        return offer, "deactivated"

    def _record_run_audit(self, report: ExternalOfferImportReport) -> None:
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.ADMIN,
            resource_type="external_offer_import",
            resource_id=report.source_file,
            message=(
                f"External offer {report.mode}: created={report.created} "
                f"updated={report.updated} unchanged={report.unchanged} "
                f"rejected={report.rejected} duplicate_in_file={report.duplicate_in_file}"
            ),
            metadata=report.to_summary_dict(),
        )

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
    ) -> tuple[Offer, str]:
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
            return offer, ExternalImportRowOutcome.CREATED.value

        if self._offer_payload_matches(existing, payload, record):
            return existing, ExternalImportRowOutcome.UNCHANGED.value

        for key, value in payload.items():
            setattr(existing, key, value)
        self.offer_repo.save(existing)
        return existing, ExternalImportRowOutcome.UPDATED.value

    def _offer_payload_matches(
        self,
        existing: Offer,
        payload: dict,
        record: ExternalOfferImportRecord,
    ) -> bool:
        compare_fields = (
            "title",
            "description",
            "discount_type",
            "discount_value",
            "starts_at",
            "ends_at",
            "status",
            "source_name",
            "source_url",
            "collected_at",
        )
        for field in compare_fields:
            if not self._values_equal(getattr(existing, field), payload[field]):
                return False
        shop = self.db.get(Shop, existing.shop_id)
        if shop is None or shop.name != record.shop_name.strip():
            return False
        location = (
            self.db.query(Location)
            .filter(Location.shop_id == existing.shop_id, Location.is_primary.is_(True))
            .one_or_none()
        )
        if location is None:
            return False
        address = record.address
        location_fields = {
            "address_line1": address.address_line1,
            "address_line2": address.address_line2,
            "city": address.city,
            "state": address.state,
            "postal_code": address.postal_code,
            "country": address.country,
            "latitude": address.latitude,
            "longitude": address.longitude,
        }
        for field, expected in location_fields.items():
            if not self._values_equal(getattr(location, field), expected):
                return False
        return True

    @staticmethod
    def _values_equal(left, right) -> bool:
        if isinstance(left, Decimal) and isinstance(right, Decimal):
            return left == right
        if left is None or right is None:
            return left is None and right is None
        if isinstance(left, datetime) and isinstance(right, datetime):
            return left.replace(tzinfo=UTC) == right.replace(tzinfo=UTC)
        return left == right

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
