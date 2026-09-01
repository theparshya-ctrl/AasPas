import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.events import DomainEvent, event_bus
from aaspas.common.exceptions import ForbiddenError, NotFoundError, ValidationAppError
from aaspas.common.geo import haversine_km
from aaspas.common.security.auth import CurrentUser
from aaspas.common.security.rbac import UserRole
from aaspas.modules.category.models import Category
from aaspas.modules.location.models import Location
from aaspas.common.source_type import SourceType
from aaspas.modules.offer.customer_fields import (
    customer_offer_is_verified,
    customer_offer_source_name,
    customer_offer_source_type,
)
from aaspas.modules.offer.events import (
    OFFER_APPROVED,
    OFFER_REJECTED,
    OFFER_SUBMITTED_FOR_VERIFICATION,
    publish_offer_event,
)
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.photo import OfferPhotoStorage
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.schemas import (
    CustomerOfferDetailResponse,
    CustomerOfferDetailShopSummary,
    OfferCreate,
    OfferRejectRequest,
    OfferResponse,
    OfferSubmitRequest,
    OfferUpdate,
)
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.offer.validation import (
    CRITICAL_OFFER_FIELDS,
    draft_discount_valid,
    validate_offer_for_submit,
)
from aaspas.modules.offer.visibility import CustomerOfferVisibility, resolve_customer_visibility
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.repository import ShopRepository
from aaspas.modules.shop.status import ShopStatus
from aaspas.common.storage import (
    resolve_customer_media_photo_url,
    resolve_customer_offer_photo_url,
    resolve_customer_shop_photo_url,
)
from aaspas.config import get_settings


class OfferService:
    MODULE = "offer"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.settings = get_settings()
        self.repo = OfferRepository(db)
        self.shop_repo = ShopRepository(db)

    def create_offer(self, user: CurrentUser, data: OfferCreate) -> OfferResponse:
        self._assert_shop_access(data.shop_id, user, write=True)
        shop = self.shop_repo.get_by_id(data.shop_id)
        if shop is not None and shop.source_type == SourceType.EXTERNAL.value:
            raise ForbiddenError("Cannot create offers on external listing shops")
        draft_discount_valid(data.discount_type, data.discount_value)
        offer = Offer(**data.model_dump(), status=OfferStatus.DRAFT.value)
        self.repo.create(offer)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.CREATE,
            resource_type="offer",
            resource_id=str(offer.id),
            actor_id=user.id,
            actor_role=user.role.value,
            message="Offer draft created",
        )
        event_bus.publish(
            DomainEvent(
                event_type="offer.created",
                source_module=self.MODULE,
                payload={"offer_id": str(offer.id), "shop_id": str(offer.shop_id)},
            )
        )
        self.db.commit()
        return self._to_offer_response(offer)

    def upload_offer_photo(
        self,
        offer_id: uuid.UUID,
        user: CurrentUser,
        content: bytes,
        content_type: str | None,
    ) -> OfferResponse:
        offer = self._get_offer_or_404(offer_id)
        if offer.source_type == SourceType.EXTERNAL.value:
            raise ForbiddenError("External offers cannot be edited through merchant flows")
        self._assert_shop_access(offer.shop_id, user, write=True)
        current = OfferStatus(offer.status)
        if current not in OfferStatus.merchant_editable():
            raise ValidationAppError(
                "Offer photo cannot be changed in its current status",
                details={"status": offer.status},
            )

        photo_storage = OfferPhotoStorage(self.settings)
        old_storage_key = offer.photo_storage_key
        new_storage_key: str | None = None
        had_confirmation = offer.merchant_confirmed_at is not None
        try:
            photo_url, new_storage_key = photo_storage.save_offer_photo(
                shop_id=offer.shop_id,
                offer_id=offer.id,
                content=content,
                declared_content_type=content_type,
            )
            offer.photo_url = photo_url
            offer.photo_storage_key = new_storage_key
            if had_confirmation:
                self._invalidate_merchant_confirmation(offer)
            if current == OfferStatus.REJECTED:
                offer.status = OfferStatus.DRAFT.value
                offer.rejection_reason = None
                offer.rejected_at = None
            self.repo.save(offer)
            record_audit(
                self.db,
                module=self.MODULE,
                action=AuditAction.UPDATE,
                resource_type="offer",
                resource_id=str(offer.id),
                actor_id=user.id,
                actor_role=user.role.value,
                message="Offer photo uploaded",
            )
            self.db.commit()
            photo_storage.delete_storage_key(old_storage_key)
        except Exception:
            self.db.rollback()
            if new_storage_key:
                photo_storage.delete_storage_key(new_storage_key)
            raise

        return self._to_offer_response(offer)

    def get_merchant_offer(self, offer_id: uuid.UUID, user: CurrentUser) -> OfferResponse:
        offer = self._get_offer_or_404(offer_id)
        self._assert_shop_access(offer.shop_id, user)
        return self._to_offer_response(offer)

    def get_customer_offer_detail(
        self,
        offer_id: uuid.UUID,
        now: datetime,
        *,
        latitude: float | None = None,
        longitude: float | None = None,
    ) -> CustomerOfferDetailResponse:
        row = self.repo.get_customer_offer_context(offer_id)
        if row is None:
            raise NotFoundError("Offer not found")

        offer, shop, location, category = row
        visibility = resolve_customer_visibility(offer, shop, now)
        if visibility is None:
            raise NotFoundError("Offer not found")

        distance_km = self._distance_km(latitude, longitude, location)
        shop_photo = resolve_customer_shop_photo_url(
            self.settings,
            photo_url=shop.photo_url,
            photo_storage_key=shop.photo_storage_key,
        )
        offer_photo = resolve_customer_offer_photo_url(
            self.settings,
            offer_photo_url=offer.photo_url,
            offer_photo_storage_key=offer.photo_storage_key,
            shop_photo_url=shop.photo_url,
            shop_photo_storage_key=shop.photo_storage_key,
        )

        return CustomerOfferDetailResponse(
            offer_id=offer.id,
            title=offer.title,
            description=offer.description,
            photo_url=offer_photo,
            discount_type=offer.discount_type,
            discount_value=offer.discount_value,
            starts_at=offer.starts_at,
            ends_at=offer.ends_at,
            status=visibility.value,
            is_verified=customer_offer_is_verified(offer),
            source_type=customer_offer_source_type(offer),
            source_name=customer_offer_source_name(offer),
            source_url=offer.source_url if offer.source_type == SourceType.EXTERNAL.value else None,
            collected_at=offer.collected_at if offer.source_type == SourceType.EXTERNAL.value else None,
            applicable_products=offer.applicable_products,
            min_purchase_amount=offer.min_purchase_amount,
            terms=offer.terms,
            shop=CustomerOfferDetailShopSummary(
                shop_id=shop.id,
                shop_name=shop.name,
                photo_url=shop_photo,
                category=self._resolve_category_name(shop, category),
                address_area=self._format_address_area(location),
                latitude=float(location.latitude) if location.latitude is not None else None,
                longitude=float(location.longitude) if location.longitude is not None else None,
                distance_km=distance_km,
            ),
        )

    def list_shop_offers(self, shop_id: uuid.UUID, user: CurrentUser) -> list[OfferResponse]:
        self._assert_shop_access(shop_id, user)
        offers = self.repo.list_by_shop(shop_id)
        return [self._to_offer_response(o) for o in offers]

    def update_offer(
        self, offer_id: uuid.UUID, user: CurrentUser, data: OfferUpdate
    ) -> OfferResponse:
        offer = self._get_offer_or_404(offer_id)
        if offer.source_type == SourceType.EXTERNAL.value:
            raise ForbiddenError("External offers cannot be edited through merchant flows")
        self._assert_shop_access(offer.shop_id, user, write=True)
        current = OfferStatus(offer.status)
        if current not in OfferStatus.merchant_editable():
            raise ValidationAppError(
                "Offer cannot be edited in its current status",
                details={"status": offer.status},
            )

        updates = data.model_dump(exclude_unset=True, exclude={"status"})
        if "discount_type" in updates or "discount_value" in updates:
            draft_discount_valid(
                updates.get("discount_type", offer.discount_type),
                updates.get("discount_value", offer.discount_value),
            )

        critical_changed = self._critical_fields_changed(offer, updates)
        for key, value in updates.items():
            setattr(offer, key, value)

        if critical_changed:
            self._invalidate_merchant_confirmation(offer)

        if current == OfferStatus.REJECTED:
            offer.status = OfferStatus.DRAFT.value
            offer.rejection_reason = None
            offer.rejected_at = None

        self.repo.save(offer)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.UPDATE,
            resource_type="offer",
            resource_id=str(offer.id),
            actor_id=user.id,
            actor_role=user.role.value,
            message="Offer draft updated" if critical_changed else "Offer updated",
        )
        self.db.commit()
        return self._to_offer_response(offer)

    def submit_for_verification(
        self,
        offer_id: uuid.UUID,
        user: CurrentUser,
        data: OfferSubmitRequest,
    ) -> OfferResponse:
        if not data.merchant_confirmed:
            raise ValidationAppError(
                "Merchant confirmation is required before submitting for verification",
                details={"field": "merchant_confirmed"},
            )

        offer = self._get_offer_or_404(offer_id)
        if offer.source_type == SourceType.EXTERNAL.value:
            raise ValidationAppError("External offers cannot be submitted for AasPas verification")
        self._assert_shop_access(offer.shop_id, user, write=True)
        current = OfferStatus(offer.status)
        if current not in OfferStatus.submittable():
            raise ValidationAppError(
                "Offer cannot be submitted in its current status",
                details={"status": offer.status},
            )

        shop = self.shop_repo.get_by_id(offer.shop_id)
        if shop is None or shop.status != ShopStatus.ACTIVE.value:
            raise ValidationAppError(
                "Your shop must be approved before this offer can be submitted for verification.",
                details={"shop_status": shop.status if shop else None},
            )

        validate_offer_for_submit(offer)
        now = datetime.now(UTC)
        offer.merchant_confirmed_at = now
        offer.merchant_confirmed_by = user.id
        offer.submitted_at = now
        offer.status = OfferStatus.PENDING_APPROVAL.value
        offer.rejection_reason = None
        offer.rejected_at = None

        self.repo.save(offer)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.UPDATE,
            resource_type="offer",
            resource_id=str(offer.id),
            actor_id=user.id,
            actor_role=user.role.value,
            message="Offer submitted for AasPas verification",
            metadata={
                "merchant_confirmed_by": str(user.id),
                "merchant_confirmed_at": now.isoformat(),
            },
        )
        is_resubmit = current == OfferStatus.REJECTED or (
            current == OfferStatus.DRAFT and self._offer_had_admin_rejection(offer.id)
        )
        publish_offer_event(
            OFFER_SUBMITTED_FOR_VERIFICATION,
            str(offer.id),
            shop_id=str(offer.shop_id),
            owner_id=str(user.id),
            is_resubmit=is_resubmit,
        )
        self.db.commit()
        return self._to_offer_response(offer)

    def approve_offer(self, offer_id: uuid.UUID, admin: CurrentUser, now: datetime) -> OfferResponse:
        offer = self._get_offer_or_404(offer_id)
        if offer.source_type == SourceType.EXTERNAL.value:
            raise ValidationAppError("External offers cannot be approved as AasPas Verified")
        if OfferStatus(offer.status) != OfferStatus.PENDING_APPROVAL:
            raise ValidationAppError(
                "Only pending offers can be approved",
                details={"status": offer.status},
            )

        shop = self.shop_repo.get_by_id(offer.shop_id)
        if shop is None or shop.status != ShopStatus.ACTIVE.value:
            raise ValidationAppError("Shop must be active to approve offers")

        validate_offer_for_submit(offer)
        approved_at = datetime.now(UTC)
        starts_at = self._ensure_aware(offer.starts_at)
        ends_at = self._ensure_aware(offer.ends_at)
        aware_now = self._ensure_aware(now)
        if ends_at <= aware_now:
            offer.status = OfferStatus.EXPIRED.value
        elif starts_at > aware_now:
            offer.status = OfferStatus.SCHEDULED.value
        else:
            offer.status = OfferStatus.ACTIVE.value

        offer.approved_at = approved_at
        offer.is_verified = True
        offer.rejection_reason = None
        offer.rejected_at = None
        self.repo.save(offer)

        record_audit(
            self.db,
            module="admin",
            action=AuditAction.ADMIN,
            resource_type="offer",
            resource_id=str(offer.id),
            actor_id=admin.id,
            actor_role=admin.role.value,
            message="Offer approved by AasPas admin",
        )
        publish_offer_event(
            OFFER_APPROVED,
            str(offer.id),
            shop_id=str(offer.shop_id),
            admin_id=str(admin.id),
        )
        self.db.commit()
        return self._to_offer_response(offer)

    def reject_offer(
        self, offer_id: uuid.UUID, data: OfferRejectRequest, admin: CurrentUser
    ) -> OfferResponse:
        offer = self._get_offer_or_404(offer_id)
        if OfferStatus(offer.status) != OfferStatus.PENDING_APPROVAL:
            raise ValidationAppError(
                "Only pending offers can be rejected",
                details={"status": offer.status},
            )

        offer.status = OfferStatus.REJECTED.value
        offer.rejection_reason = data.reason
        offer.rejected_at = datetime.now(UTC)
        offer.merchant_confirmed_at = None
        offer.merchant_confirmed_by = None
        offer.submitted_at = None
        offer.is_verified = False
        self.repo.save(offer)

        record_audit(
            self.db,
            module="admin",
            action=AuditAction.ADMIN,
            resource_type="offer",
            resource_id=str(offer.id),
            actor_id=admin.id,
            actor_role=admin.role.value,
            message="Offer rejected by AasPas admin",
            metadata={"reason": data.reason},
        )
        publish_offer_event(
            OFFER_REJECTED,
            str(offer.id),
            shop_id=str(offer.shop_id),
            admin_id=str(admin.id),
            reason=data.reason,
        )
        self.db.commit()
        return self._to_offer_response(offer)

    def _offer_had_admin_rejection(self, offer_id: uuid.UUID) -> bool:
        from aaspas.common.audit import AuditLog

        return (
            self.db.query(AuditLog.id)
            .filter(
                AuditLog.resource_type == "offer",
                AuditLog.resource_id == str(offer_id),
                AuditLog.module == "admin",
                AuditLog.message.like("%rejected%"),
            )
            .first()
            is not None
        )

    def _public_offer_photo_url(self, offer: Offer) -> str | None:
        return resolve_customer_media_photo_url(
            self.settings,
            photo_url=offer.photo_url,
            photo_storage_key=offer.photo_storage_key,
        )

    def _to_offer_response(self, offer: Offer) -> OfferResponse:
        data = OfferResponse.model_validate(offer).model_dump()
        data["photo_url"] = self._public_offer_photo_url(offer)
        return OfferResponse.model_validate(data)

    def _get_offer_or_404(self, offer_id: uuid.UUID) -> Offer:
        offer = self.repo.get_by_id(offer_id)
        if offer is None:
            raise NotFoundError("Offer not found")
        return offer

    def _critical_fields_changed(self, offer: Offer, updates: dict) -> bool:
        for key in updates:
            if key in CRITICAL_OFFER_FIELDS and updates[key] != getattr(offer, key):
                return True
        return False

    @staticmethod
    def _invalidate_merchant_confirmation(offer: Offer) -> None:
        offer.merchant_confirmed_at = None
        offer.merchant_confirmed_by = None
        offer.submitted_at = None

    def _assert_shop_access(
        self, shop_id: uuid.UUID, user: CurrentUser, write: bool = False
    ) -> None:
        shop = self.shop_repo.get_by_id(shop_id)
        if shop is None:
            raise NotFoundError("Shop not found")
        if user.role in {UserRole.ADMIN, UserRole.SUPER_ADMIN}:
            return
        if shop.owner_id != user.id:
            raise ForbiddenError("Cannot access another shop's data")
        if write and shop.owner_id != user.id:
            raise ForbiddenError("Cannot manage offers for another shop")

    @staticmethod
    def _resolve_category_name(shop: Shop, category: Category | None) -> str | None:
        if category is not None:
            return category.name
        return shop.category

    @staticmethod
    def _format_address_area(location: Location) -> str:
        parts = [location.city]
        if location.state:
            parts.append(location.state)
        return ", ".join(parts)

    @staticmethod
    def _distance_km(
        latitude: float | None,
        longitude: float | None,
        location: Location,
    ) -> float | None:
        if latitude is None or longitude is None:
            return None
        if location.latitude is None or location.longitude is None:
            return None
        return round(haversine_km(latitude, longitude, location.latitude, location.longitude), 2)

    @staticmethod
    def _ensure_aware(dt: datetime) -> datetime:
        if dt.tzinfo is None:
            return dt.replace(tzinfo=UTC)
        return dt
