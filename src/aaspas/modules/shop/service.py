import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.exceptions import ForbiddenError, NotFoundError, ValidationAppError
from aaspas.common.geo import haversine_km
from aaspas.common.security.auth import CurrentUser
from aaspas.common.security.rbac import UserRole
from aaspas.modules.auth.models import User
from aaspas.modules.category.models import Category
from aaspas.modules.location.models import Location
from aaspas.modules.location.repository import LocationRepository
from aaspas.modules.location.schemas import LocationResponse
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.visibility import CustomerOfferVisibility, resolve_customer_visibility
from aaspas.modules.shop.events import SHOP_SUBMITTED_FOR_APPROVAL, publish_shop_event
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.photo import ShopPhotoStorage
from aaspas.modules.shop.repository import ShopRepository
from aaspas.modules.shop.schemas import (
    CustomerShopDetailResponse,
    CustomerShopOfferItem,
    OfferStatusCounts,
    ShopDetailResponse,
    ShopOnboardingCreate,
    ShopOnboardingUpdate,
    ShopOwnerDashboardResponse,
    ShopResponse,
)
from aaspas.modules.shop.status import ShopStatus
from aaspas.common.storage import (
    resolve_customer_offer_photo_url,
    resolve_customer_shop_photo_url,
    resolve_shop_photo_url,
)
from aaspas.config import get_settings
from aaspas.modules.shop.utils import unique_slug


class ShopService:
    MODULE = "shop"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = ShopRepository(db)
        self.location_repo = LocationRepository(db)
        self.settings = get_settings()

    def create_shop(self, user: CurrentUser, data: ShopOnboardingCreate) -> ShopDetailResponse:
        slug = unique_slug(self.db, data.name)
        shop = Shop(
            owner_id=user.id,
            name=data.name,
            slug=slug,
            description=data.description,
            category=data.category,
            contact_number=data.contact_number,
            business_hours=data.business_hours.model_dump(),
            photo_url=data.photo_url,
            status=ShopStatus.DRAFT.value,
        )
        self.repo.create(shop)

        location = self._create_primary_location(shop.id, data.address.model_dump())
        self._link_owner_to_shop(user.id, shop.id)

        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.CREATE,
            resource_type="shop",
            resource_id=str(shop.id),
            actor_id=user.id,
            actor_role=user.role.value,
            message="Shop onboarding profile created as draft",
        )
        self.db.commit()
        return self._to_detail(shop, location)

    def get_shop(self, shop_id: uuid.UUID, user: CurrentUser) -> ShopDetailResponse:
        shop = self._get_accessible_shop(shop_id, user)
        location = self.location_repo.get_primary_by_shop(shop.id)
        return self._to_detail(shop, location)

    def get_customer_shop_detail(
        self,
        shop_id: uuid.UUID,
        now: datetime,
        *,
        latitude: float | None = None,
        longitude: float | None = None,
    ) -> CustomerShopDetailResponse:
        row = self.repo.get_active_with_context(shop_id)
        if row is None:
            raise NotFoundError("Shop not found")

        shop, location, category = row
        offer_repo = OfferRepository(self.db)
        offer_rows = offer_repo.list_customer_offers_for_shop(shop_id, now)

        today_offers: list[CustomerShopOfferItem] = []
        coming_soon: list[CustomerShopOfferItem] = []
        for offer, _shop, _location, _category in offer_rows:
            visibility = resolve_customer_visibility(offer, shop, now)
            if visibility is None:
                continue
            item = self._map_customer_shop_offer(offer, shop, visibility)
            if visibility == CustomerOfferVisibility.ACTIVE:
                today_offers.append(item)
            else:
                coming_soon.append(item)

        distance_km = self._distance_km(latitude, longitude, location)

        return CustomerShopDetailResponse(
            shop_id=shop.id,
            shop_name=shop.name,
            description=shop.description,
            photo_url=self._customer_photo_url(shop),
            category=self._resolve_category_name(shop, category),
            address_line1=location.address_line1,
            address_line2=location.address_line2,
            area=self._format_area(location),
            city=location.city,
            pincode=location.postal_code,
            latitude=float(location.latitude) if location.latitude is not None else None,
            longitude=float(location.longitude) if location.longitude is not None else None,
            phone=shop.contact_number,
            business_hours=shop.business_hours,
            is_verified=shop.approved_at is not None,
            active_offer_count=len(today_offers),
            distance_km=distance_km,
            today_offers=today_offers,
            coming_soon=coming_soon,
        )

    def update_shop(
        self, shop_id: uuid.UUID, user: CurrentUser, data: ShopOnboardingUpdate
    ) -> ShopDetailResponse:
        shop = self._get_accessible_shop(shop_id, user, write=True)
        updates = data.model_dump(exclude_unset=True, exclude={"address", "business_hours"})
        photo_only = set(updates.keys()) == {"photo_url"}

        if ShopStatus(shop.status) not in ShopStatus.editable_by_owner():
            if not (ShopStatus(shop.status) == ShopStatus.ACTIVE and photo_only):
                raise ValidationAppError(
                    "Shop can only be edited while in draft or rejected status",
                    details={"status": shop.status},
                )

        if ShopStatus(shop.status) == ShopStatus.ACTIVE and photo_only:
            shop.photo_url = data.photo_url
            self.repo.save(shop)
            record_audit(
                self.db,
                module=self.MODULE,
                action=AuditAction.UPDATE,
                resource_type="shop",
                resource_id=str(shop.id),
                actor_id=user.id,
                actor_role=user.role.value,
                message="Shop photo updated",
            )
            self.db.commit()
            location = self.location_repo.get_primary_by_shop(shop.id)
            return self._to_detail(shop, location)

        if ShopStatus(shop.status) not in ShopStatus.editable_by_owner():
            raise ValidationAppError(
                "Shop can only be edited while in draft or rejected status",
                details={"status": shop.status},
            )

        for key, value in updates.items():
            setattr(shop, key, value)

        if data.business_hours is not None:
            shop.business_hours = data.business_hours.model_dump()

        location = self.location_repo.get_primary_by_shop(shop.id)
        if data.address is not None:
            address_data = data.address.model_dump()
            if location is None:
                location = self._create_primary_location(shop.id, address_data)
            else:
                for key, value in address_data.items():
                    setattr(location, key, value)
                self.location_repo.save(location)

        self.repo.save(shop)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.UPDATE,
            resource_type="shop",
            resource_id=str(shop.id),
            actor_id=user.id,
            actor_role=user.role.value,
        )
        self.db.commit()
        return self._to_detail(shop, location)

    def upload_my_shop_photo(
        self,
        user: CurrentUser,
        content: bytes,
        content_type: str | None,
    ) -> ShopDetailResponse:
        shops = self.repo.list_by_owner(user.id)
        if not shops:
            raise NotFoundError("No shop found for this account")
        shop = shops[0]
        self._get_accessible_shop(shop.id, user, write=True)

        settings = get_settings()
        photo_storage = ShopPhotoStorage(settings)
        old_storage_key = shop.photo_storage_key
        new_storage_key: str | None = None
        try:
            photo_url, new_storage_key = photo_storage.save_shop_photo(
                shop_id=shop.id,
                content=content,
                declared_content_type=content_type,
            )
            shop.photo_url = photo_url
            shop.photo_storage_key = new_storage_key
            self.repo.save(shop)
            record_audit(
                self.db,
                module=self.MODULE,
                action=AuditAction.UPDATE,
                resource_type="shop",
                resource_id=str(shop.id),
                actor_id=user.id,
                actor_role=user.role.value,
                message="Shop photo uploaded",
            )
            self.db.commit()
            photo_storage.delete_storage_key(old_storage_key)
        except Exception:
            self.db.rollback()
            if new_storage_key:
                photo_storage.delete_storage_key(new_storage_key)
            raise

        location = self.location_repo.get_primary_by_shop(shop.id)
        return self._to_detail(shop, location)

    def submit_for_approval(self, shop_id: uuid.UUID, user: CurrentUser) -> ShopDetailResponse:
        shop = self._get_accessible_shop(shop_id, user, write=True)
        current = ShopStatus(shop.status)
        if current not in ShopStatus.submittable():
            raise ValidationAppError(
                "Shop cannot be submitted in its current status",
                details={"status": shop.status},
            )

        self._validate_ready_for_submission(shop)

        shop.status = ShopStatus.PENDING_APPROVAL.value
        shop.submitted_at = datetime.now(UTC)
        shop.rejection_reason = None
        self.repo.save(shop)

        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.UPDATE,
            resource_type="shop",
            resource_id=str(shop.id),
            actor_id=user.id,
            actor_role=user.role.value,
            message="Shop submitted for admin approval",
        )
        is_resubmit = current == ShopStatus.REJECTED
        publish_shop_event(
            SHOP_SUBMITTED_FOR_APPROVAL,
            str(shop.id),
            owner_id=str(shop.owner_id),
            is_resubmit=is_resubmit,
        )
        self.db.commit()

        location = self.location_repo.get_primary_by_shop(shop.id)
        return self._to_detail(shop, location)

    def list_my_shops(self, user: CurrentUser) -> list[ShopDetailResponse]:
        shops = self.repo.list_by_owner(user.id)
        return [
            self._to_detail(shop, self.location_repo.get_primary_by_shop(shop.id)) for shop in shops
        ]

    def get_owner_dashboard(self, user: CurrentUser) -> ShopOwnerDashboardResponse:
        shops = self.repo.list_by_owner(user.id)
        if not shops:
            raise NotFoundError("No shop found for this account")

        shop = shops[0]
        location = self.location_repo.get_primary_by_shop(shop.id)
        shop_detail = self._to_detail(shop, location)
        status = ShopStatus(shop.status)
        offer_counts = OfferStatusCounts(
            **OfferRepository(self.db).count_by_status(shop.id),
        )

        return ShopOwnerDashboardResponse(
            shop=shop_detail,
            is_verified=shop.approved_at is not None,
            can_edit_profile=status in ShopStatus.editable_by_owner(),
            can_submit_offers=status == ShopStatus.ACTIVE,
            offer_counts=offer_counts,
            status_message=self._owner_status_message(shop),
        )

    @staticmethod
    def _owner_status_message(shop: Shop) -> str | None:
        status = ShopStatus(shop.status)
        if status == ShopStatus.PENDING_APPROVAL:
            return "Pending verification — your shop is awaiting AasPas admin review."
        if status == ShopStatus.REJECTED:
            reason = shop.rejection_reason or "Please update your shop details."
            return f"Shop verification needs changes. {reason}"
        if status == ShopStatus.DRAFT:
            return "Complete your shop profile and submit for approval."
        if status == ShopStatus.ACTIVE:
            return "Approved — your shop is active."
        return None

    def _validate_ready_for_submission(self, shop: Shop) -> None:
        missing: list[str] = []
        if not shop.name:
            missing.append("name")
        if not shop.category:
            missing.append("category")
        if not shop.contact_number:
            missing.append("contact_number")
        if not shop.business_hours:
            missing.append("business_hours")

        location = self.location_repo.get_primary_by_shop(shop.id)
        if location is None:
            missing.append("address")
        elif location.latitude is None or location.longitude is None:
            missing.append("gps_coordinates")

        if missing:
            raise ValidationAppError(
                "Shop profile is incomplete and cannot be submitted",
                details={"missing_fields": missing},
            )

    def _create_primary_location(self, shop_id: uuid.UUID, address_data: dict) -> Location:
        location = Location(
            shop_id=shop_id,
            label="Primary",
            is_primary=True,
            **address_data,
        )
        return self.location_repo.create(location)

    def _link_owner_to_shop(self, user_id: uuid.UUID, shop_id: uuid.UUID) -> None:
        db_user = self.db.get(User, user_id)
        if db_user:
            db_user.shop_id = shop_id
            db_user.role = UserRole.SHOP_OWNER.value

    def _get_accessible_shop(
        self, shop_id: uuid.UUID, user: CurrentUser, write: bool = False
    ) -> Shop:
        shop = self.repo.get_by_id(shop_id)
        if shop is None:
            raise NotFoundError("Shop not found")

        if user.role in {UserRole.ADMIN, UserRole.SUPER_ADMIN}:
            return shop

        if shop.owner_id != user.id:
            raise ForbiddenError("Cannot access another shop's data")

        if write and shop.owner_id != user.id:
            raise ForbiddenError("Cannot modify another shop")

        return shop

    def _to_detail(self, shop: Shop, location: Location | None) -> ShopDetailResponse:
        data = ShopResponse.model_validate(shop).model_dump()
        data["photo_url"] = self._public_photo_url(shop)
        return ShopDetailResponse(
            **data,
            primary_location=LocationResponse.model_validate(location) if location else None,
        )

    def _public_photo_url(self, shop: Shop) -> str | None:
        return resolve_shop_photo_url(
            self.settings,
            photo_url=shop.photo_url,
            photo_storage_key=shop.photo_storage_key,
        )

    def _customer_photo_url(self, shop: Shop) -> str | None:
        return resolve_customer_shop_photo_url(
            self.settings,
            photo_url=shop.photo_url,
            photo_storage_key=shop.photo_storage_key,
        )

    @staticmethod
    def _resolve_category_name(shop: Shop, category: Category | None) -> str | None:
        if category is not None:
            return category.name
        return shop.category

    @staticmethod
    def _format_area(location: Location) -> str | None:
        parts = [location.city]
        if location.state:
            parts.append(location.state)
        return ", ".join(parts) if parts else None

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

    def _map_customer_shop_offer(
        self,
        offer: Offer,
        shop: Shop,
        visibility: CustomerOfferVisibility,
    ) -> CustomerShopOfferItem:
        return CustomerShopOfferItem(
            offer_id=offer.id,
            title=offer.title,
            description=offer.description,
            photo_url=resolve_customer_offer_photo_url(
                self.settings,
                offer_photo_url=offer.photo_url,
                offer_photo_storage_key=offer.photo_storage_key,
                shop_photo_url=shop.photo_url,
                shop_photo_storage_key=shop.photo_storage_key,
            ),
            discount_type=offer.discount_type,
            discount_value=offer.discount_value,
            starts_at=offer.starts_at,
            ends_at=offer.ends_at,
            status=visibility.value,
        )

