import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, require_permission
from aaspas.common.security.rbac import Permission
from aaspas.common.time import get_now
from aaspas.database import get_db
from aaspas.modules.admin.schemas import (
    AdminDashboard,
    AdminOfferReviewItem,
    AdminShopDetail,
    AdminShopListItem,
    AdminShopReviewItem,
    AdminUserListItem,
    ShopRejectRequest,
)
from aaspas.modules.admin.service import AdminService
from aaspas.modules.offer.schemas import OfferRejectRequest, OfferResponse
from aaspas.modules.offer.service import OfferService

router = APIRouter(tags=["Admin"])


@router.get("/dashboard", response_model=ApiResponse[AdminDashboard])
def admin_dashboard(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_READ_ALL))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminDashboard]:
    """Platform overview including shop onboarding pipeline counts."""
    dashboard = AdminService(db).get_dashboard()
    return ApiResponse(data=dashboard)


@router.get("/shops", response_model=ApiResponse[list[AdminShopListItem]])
def list_shops(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_READ_ALL))],
    db: Annotated[Session, Depends(get_db)],
    status: Annotated[str | None, Query()] = None,
    search: Annotated[str | None, Query(max_length=100)] = None,
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> ApiResponse[list[AdminShopListItem]]:
    """List shops for admin management with optional status filter and search."""
    shops = AdminService(db).list_shops(status=status, search=search, limit=limit, offset=offset)
    return ApiResponse(data=shops, meta={"limit": limit, "offset": offset, "count": len(shops)})


@router.get("/shops/pending", response_model=ApiResponse[list[AdminShopReviewItem]])
def list_pending_shops(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> ApiResponse[list[AdminShopReviewItem]]:
    """List shops awaiting admin approval."""
    shops = AdminService(db).list_pending_shops(limit=limit, offset=offset)
    return ApiResponse(data=shops, meta={"limit": limit, "offset": offset, "count": len(shops)})


@router.get("/shops/{shop_id}/detail", response_model=ApiResponse[AdminShopDetail])
def shop_detail(
    shop_id: uuid.UUID,
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_READ_ALL))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminShopDetail]:
    """Read-only shop management detail including offers summary and audit history."""
    shop = AdminService(db).get_shop_detail(shop_id)
    return ApiResponse(data=shop)


@router.get("/shops/{shop_id}", response_model=ApiResponse[AdminShopReviewItem])
def review_shop(
    shop_id: uuid.UUID,
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminShopReviewItem]:
    """Review full shop onboarding details including owner and location."""
    shop = AdminService(db).get_shop_for_review(shop_id)
    return ApiResponse(data=shop)


@router.post("/shops/{shop_id}/approve", response_model=ApiResponse[AdminShopReviewItem])
def approve_shop(
    shop_id: uuid.UUID,
    admin: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminShopReviewItem]:
    """Approve a pending shop — transitions to ACTIVE and publishes domain events."""
    shop = AdminService(db).approve_shop(shop_id, admin)
    return ApiResponse(data=shop)


@router.post("/shops/{shop_id}/reject", response_model=ApiResponse[AdminShopReviewItem])
def reject_shop(
    shop_id: uuid.UUID,
    data: ShopRejectRequest,
    admin: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminShopReviewItem]:
    """Reject a pending shop with a stored reason — owner may correct and resubmit."""
    shop = AdminService(db).reject_shop(shop_id, data, admin)
    return ApiResponse(data=shop)


@router.get("/offers/pending", response_model=ApiResponse[list[AdminOfferReviewItem]])
def list_pending_offers(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> ApiResponse[list[AdminOfferReviewItem]]:
    """List offers awaiting AasPas verification."""
    offers = AdminService(db).list_pending_offers(limit=limit, offset=offset)
    return ApiResponse(data=offers, meta={"limit": limit, "offset": offset, "count": len(offers)})


@router.get("/offers/{offer_id}", response_model=ApiResponse[AdminOfferReviewItem])
def review_offer(
    offer_id: uuid.UUID,
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[AdminOfferReviewItem]:
    """Review a pending offer with shop and merchant context."""
    offer = AdminService(db).get_offer_for_review(offer_id)
    return ApiResponse(data=offer)


@router.post("/offers/{offer_id}/approve", response_model=ApiResponse[OfferResponse])
def approve_offer(
    offer_id: uuid.UUID,
    admin: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
) -> ApiResponse[OfferResponse]:
    """Approve a pending offer — transitions to scheduled or active based on start time."""
    offer = OfferService(db).approve_offer(offer_id, admin, now)
    return ApiResponse(data=offer)


@router.post("/offers/{offer_id}/reject", response_model=ApiResponse[OfferResponse])
def reject_offer(
    offer_id: uuid.UUID,
    data: OfferRejectRequest,
    admin: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_SHOPS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    """Reject a pending offer with a stored reason."""
    offer = OfferService(db).reject_offer(offer_id, data, admin)
    return ApiResponse(data=offer)


@router.get("/users", response_model=ApiResponse[list[AdminUserListItem]])
def list_users(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_MANAGE_USERS))],
    db: Annotated[Session, Depends(get_db)],
    role: Annotated[str | None, Query()] = None,
    search: Annotated[str | None, Query(max_length=100)] = None,
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> ApiResponse[list[AdminUserListItem]]:
    """List platform users for admin management."""
    users = AdminService(db).list_users(role=role, search=search, limit=limit, offset=offset)
    return ApiResponse(data=users, meta={"limit": limit, "offset": offset, "count": len(users)})
