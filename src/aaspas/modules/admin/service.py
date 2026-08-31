import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, AuditLog, record_audit
from aaspas.common.exceptions import NotFoundError, ValidationAppError
from aaspas.common.security.auth import CurrentUser
from aaspas.modules.admin.schemas import (
    AdminAuditEntry,
    AdminDashboard,
    AdminOfferMerchantSummary,
    AdminOfferReviewItem,
    AdminOfferShopSummary,
    AdminShopDetail,
    AdminShopListItem,
    AdminShopOwnerSummary,
    AdminShopReviewItem,
    AdminUserListItem,
    ShopRejectRequest,
)
from aaspas.modules.auth.repository import AuthRepository
from aaspas.modules.category.models import Category
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.auth.models import User
from aaspas.modules.shop.models import Shop
from aaspas.modules.location.repository import LocationRepository
from aaspas.modules.shop.events import (
    SHOP_ACTIVATED,
    SHOP_APPROVED,
    SHOP_REJECTED,
    publish_shop_event,
)
from aaspas.modules.shop.repository import ShopRepository
from aaspas.modules.shop.service import ShopService
from aaspas.common.storage import resolve_customer_offer_photo_url, resolve_customer_shop_photo_url
from aaspas.config import get_settings
from aaspas.modules.shop.status import ShopStatus


class AdminService:
    MODULE = "admin"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.settings = get_settings()
        self.shop_repo = ShopRepository(db)
        self.location_repo = LocationRepository(db)
        self.shop_service = ShopService(db)
        self.offer_repo = OfferRepository(db)
        self.auth_repo = AuthRepository(db)

    def get_dashboard(self) -> AdminDashboard:
        return AdminDashboard(
            pending_shops=self.shop_repo.count_by_status(ShopStatus.PENDING_APPROVAL),
            pending_offers=self.offer_repo.count_by_status_global(OfferStatus.PENDING_APPROVAL),
            active_shops=self.shop_repo.count_by_status(ShopStatus.ACTIVE),
            rejected_shops=self.shop_repo.count_by_status(ShopStatus.REJECTED),
            draft_shops=self.shop_repo.count_by_status(ShopStatus.DRAFT),
        )

    def list_pending_offers(
        self, limit: int = 50, offset: int = 0
    ) -> list[AdminOfferReviewItem]:
        rows = self.offer_repo.list_pending_for_admin(limit=limit, offset=offset)
        return [self._to_review_item(offer, shop, merchant, category) for offer, shop, merchant, category in rows]

    def get_offer_for_review(self, offer_id: uuid.UUID) -> AdminOfferReviewItem:
        row = self.offer_repo.get_pending_for_admin(offer_id)
        if row is None:
            raise NotFoundError("Offer not found")
        offer, shop, merchant, category = row
        if OfferStatus(offer.status) != OfferStatus.PENDING_APPROVAL:
            raise ValidationAppError(
                "Only pending offers can be reviewed",
                details={"status": offer.status},
            )
        return self._to_review_item(offer, shop, merchant, category)

    def _to_review_item(
        self,
        offer: Offer,
        shop: Shop,
        merchant: User,
        category: Category | None,
    ) -> AdminOfferReviewItem:
        category_name = category.name if category is not None else shop.category
        return AdminOfferReviewItem(
            id=offer.id,
            shop_id=offer.shop_id,
            title=offer.title,
            description=offer.description,
            discount_type=offer.discount_type,
            discount_value=offer.discount_value,
            status=offer.status,
            starts_at=offer.starts_at,
            ends_at=offer.ends_at,
            photo_url=resolve_customer_offer_photo_url(
                self.settings,
                offer_photo_url=offer.photo_url,
                offer_photo_storage_key=offer.photo_storage_key,
                shop_photo_url=shop.photo_url,
                shop_photo_storage_key=shop.photo_storage_key,
            ),
            applicable_products=offer.applicable_products,
            min_purchase_amount=offer.min_purchase_amount,
            terms=offer.terms,
            merchant_confirmed_at=offer.merchant_confirmed_at,
            submitted_at=offer.submitted_at,
            is_verified=offer.is_verified,
            shop=AdminOfferShopSummary(
                shop_id=shop.id,
                shop_name=shop.name,
                category=category_name,
                status=shop.status,
                is_verified=shop.approved_at is not None,
                photo_url=resolve_customer_shop_photo_url(
                    self.settings,
                    photo_url=shop.photo_url,
                    photo_storage_key=shop.photo_storage_key,
                ),
            ),
            merchant=AdminOfferMerchantSummary(
                user_id=merchant.id,
                full_name=merchant.full_name,
                email=merchant.email,
            ),
        )

    def list_pending_shops(self, limit: int = 50, offset: int = 0) -> list[AdminShopReviewItem]:
        rows = self.shop_repo.list_pending_with_owner(limit=limit, offset=offset)
        return [self._to_shop_review_item(shop, owner, location) for shop, owner, location in rows]

    def list_shops(
        self,
        *,
        status: str | None = None,
        search: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[AdminShopListItem]:
        rows = self.shop_repo.list_for_admin(
            status=status,
            search=search,
            limit=limit,
            offset=offset,
        )
        items: list[AdminShopListItem] = []
        for shop, owner, location in rows:
            items.append(
                AdminShopListItem(
                    shop_id=shop.id,
                    shop_name=shop.name,
                    owner_name=owner.full_name,
                    owner_email=owner.email,
                    category=shop.category,
                    city=location.city if location else None,
                    status=shop.status,
                    is_verified=shop.approved_at is not None,
                    created_at=shop.created_at,
                    offer_count=self.offer_repo.count_for_shop(shop.id),
                    rejection_reason=shop.rejection_reason,
                )
            )
        return items

    def get_shop_detail(self, shop_id: uuid.UUID) -> AdminShopDetail:
        row = self.shop_repo.get_with_owner(shop_id)
        if row is None:
            raise NotFoundError("Shop not found")
        shop, owner, location = row
        review = self._to_shop_review_item(shop, owner, location)
        offer_counts = self.offer_repo.count_by_status(shop.id)
        audit_entries = self._audit_entries_for_shop(shop.id)
        return AdminShopDetail(
            **review.model_dump(),
            created_at=shop.created_at,
            offer_count=sum(offer_counts.values()),
            offer_counts_by_status=offer_counts,
            audit_entries=audit_entries,
        )

    def list_users(
        self,
        *,
        role: str | None = None,
        search: str | None = None,
        limit: int = 50,
        offset: int = 0,
    ) -> list[AdminUserListItem]:
        users = self.auth_repo.list_for_admin(
            role=role,
            search=search,
            limit=limit,
            offset=offset,
        )
        items: list[AdminUserListItem] = []
        for user in users:
            shop_name: str | None = None
            shop_id = user.shop_id
            if user.role == "shop_owner":
                owned_shops = self.shop_repo.list_by_owner(user.id)
                if owned_shops:
                    shop_id = owned_shops[0].id
                    shop_name = owned_shops[0].name
            items.append(
                AdminUserListItem(
                    user_id=user.id,
                    full_name=user.full_name,
                    email=user.email,
                    role=user.role,
                    is_active=user.is_active,
                    shop_id=shop_id,
                    shop_name=shop_name,
                    created_at=user.created_at,
                )
            )
        return items

    def get_shop_for_review(self, shop_id: uuid.UUID) -> AdminShopReviewItem:
        row = self.shop_repo.get_with_owner(shop_id)
        if row is None:
            raise NotFoundError("Shop not found")
        shop, owner, location = row
        return self._to_shop_review_item(shop, owner, location)

    def approve_shop(self, shop_id: uuid.UUID, admin: CurrentUser) -> AdminShopReviewItem:
        shop = self.shop_repo.get_by_id(shop_id)
        if shop is None:
            raise NotFoundError("Shop not found")

        if ShopStatus(shop.status) == ShopStatus.ACTIVE:
            row = self.shop_repo.get_with_owner(shop_id)
            assert row is not None
            return self._to_shop_review_item(*row)

        if ShopStatus(shop.status) != ShopStatus.PENDING_APPROVAL:
            raise ValidationAppError(
                "Only shops pending approval can be approved",
                details={"status": shop.status},
            )

        self.shop_service._validate_ready_for_submission(shop)
        shop.status = ShopStatus.ACTIVE.value
        shop.approved_at = datetime.now(UTC)
        shop.rejection_reason = None
        self.shop_repo.save(shop)

        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.ADMIN,
            resource_type="shop",
            resource_id=str(shop.id),
            actor_id=admin.id,
            actor_role=admin.role.value,
            message="Shop approved and activated",
            metadata={"admin_id": str(admin.id)},
        )
        publish_shop_event(
            SHOP_APPROVED,
            str(shop.id),
            owner_id=str(shop.owner_id),
            admin_id=str(admin.id),
        )
        publish_shop_event(
            SHOP_ACTIVATED,
            str(shop.id),
            owner_id=str(shop.owner_id),
        )
        self.db.commit()

        row = self.shop_repo.get_with_owner(shop_id)
        assert row is not None
        return self._to_shop_review_item(*row)

    def reject_shop(
        self, shop_id: uuid.UUID, data: ShopRejectRequest, admin: CurrentUser
    ) -> AdminShopReviewItem:
        shop = self._get_pending_shop(shop_id)
        shop.status = ShopStatus.REJECTED.value
        shop.rejection_reason = data.reason.strip()
        shop.approved_at = None
        self.shop_repo.save(shop)

        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.ADMIN,
            resource_type="shop",
            resource_id=str(shop.id),
            actor_id=admin.id,
            actor_role=admin.role.value,
            message=f"Shop rejected: {data.reason[:200]}",
            metadata={"rejection_reason": shop.rejection_reason, "admin_id": str(admin.id)},
        )
        publish_shop_event(
            SHOP_REJECTED,
            str(shop.id),
            owner_id=str(shop.owner_id),
            admin_id=str(admin.id),
            reason=shop.rejection_reason,
        )
        self.db.commit()

        row = self.shop_repo.get_with_owner(shop_id)
        assert row is not None
        return self._to_shop_review_item(*row)

    @staticmethod
    def _to_shop_review_item(
        shop: Shop,
        owner: User,
        location: "Location | None",
    ) -> AdminShopReviewItem:
        from aaspas.modules.location.models import Location

        settings = get_settings()
        area = None
        if location is not None:
            parts = [location.city]
            if location.state:
                parts.append(location.state)
            area = ", ".join(parts)

        return AdminShopReviewItem(
            shop_id=shop.id,
            shop_name=shop.name,
            description=shop.description,
            category=shop.category,
            photo_url=resolve_customer_shop_photo_url(
                settings,
                photo_url=shop.photo_url,
                photo_storage_key=shop.photo_storage_key,
            ),
            contact_number=shop.contact_number,
            business_hours=shop.business_hours,
            status=shop.status,
            submitted_at=shop.submitted_at,
            approved_at=shop.approved_at,
            rejection_reason=shop.rejection_reason,
            is_verified=shop.approved_at is not None,
            address_line1=location.address_line1 if location else None,
            address_line2=location.address_line2 if location else None,
            area=area,
            city=location.city if location else None,
            pincode=location.postal_code if location else None,
            latitude=float(location.latitude) if location and location.latitude is not None else None,
            longitude=float(location.longitude) if location and location.longitude is not None else None,
            owner=AdminShopOwnerSummary(
                user_id=owner.id,
                full_name=owner.full_name,
                email=owner.email,
                phone=shop.contact_number,
            ),
        )

    def _audit_entries_for_shop(self, shop_id: uuid.UUID, *, limit: int = 10) -> list[AdminAuditEntry]:
        rows = (
            self.db.query(AuditLog)
            .filter(
                AuditLog.resource_type == "shop",
                AuditLog.resource_id == str(shop_id),
            )
            .order_by(AuditLog.created_at.desc())
            .limit(limit)
            .all()
        )
        return [
            AdminAuditEntry(
                message=entry.message,
                action=entry.action,
                actor_role=entry.actor_role,
                created_at=entry.created_at,
            )
            for entry in rows
        ]

    def _get_pending_shop(self, shop_id: uuid.UUID):
        shop = self.shop_repo.get_by_id(shop_id)
        if shop is None:
            raise NotFoundError("Shop not found")
        if shop.status != ShopStatus.PENDING_APPROVAL.value:
            raise ValidationAppError(
                "Only shops pending approval can be approved or rejected",
                details={"status": shop.status},
            )
        return shop
