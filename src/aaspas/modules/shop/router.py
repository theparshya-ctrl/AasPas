import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, File, Query, UploadFile
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, get_current_user_optional, require_permission
from aaspas.common.security.rbac import Permission, UserRole
from aaspas.database import get_db
from aaspas.common.time import get_now
from aaspas.config import get_settings
from aaspas.modules.shop.schemas import (
    CustomerShopDetailResponse,
    ShopDetailResponse,
    ShopOnboardingCreate,
    ShopOnboardingUpdate,
    ShopOwnerDashboardResponse,
)
from aaspas.modules.shop.service import ShopService

router = APIRouter(tags=["Shop"])


@router.post("", response_model=ApiResponse[ShopDetailResponse], status_code=201)
def create_shop(
    data: ShopOnboardingCreate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_CREATE))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[ShopDetailResponse]:
    """Create a shop onboarding profile in DRAFT status with address and GPS coordinates."""
    shop = ShopService(db).create_shop(user, data)
    return ApiResponse(data=shop)


@router.get("/me", response_model=ApiResponse[list[ShopDetailResponse]])
def list_my_shops(
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_READ_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[list[ShopDetailResponse]]:
    """List all shops owned by the authenticated shop owner."""
    shops = ShopService(db).list_my_shops(user)
    return ApiResponse(data=shops)


@router.get("/me/dashboard", response_model=ApiResponse[ShopOwnerDashboardResponse])
def get_my_shop_dashboard(
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_READ_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[ShopOwnerDashboardResponse]:
    """Shop owner dashboard with profile summary and offer counts."""
    dashboard = ShopService(db).get_owner_dashboard(user)
    return ApiResponse(data=dashboard)


@router.post("/me/photo", response_model=ApiResponse[ShopDetailResponse])
async def upload_my_shop_photo(
    file: Annotated[UploadFile, File(...)],
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[ShopDetailResponse]:
    """Upload a shop photo for the authenticated owner's shop (ownership from session)."""
    content = await file.read()
    shop = ShopService(db).upload_my_shop_photo(user, content, file.content_type)
    return ApiResponse(data=shop)


@router.get("/{shop_id}", response_model=ApiResponse[ShopDetailResponse | CustomerShopDetailResponse])
def get_shop(
    shop_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    user: Annotated[CurrentUser | None, Depends(get_current_user_optional)] = None,
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
) -> ApiResponse[ShopDetailResponse | CustomerShopDetailResponse]:
    """Owner/admin detail for authenticated owners; public customer detail for active shops."""
    service = ShopService(db)
    if user is not None and user.role in {UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.SHOP_OWNER}:
        shop = service.get_shop(shop_id, user)
        return ApiResponse(data=shop)

    detail = service.get_customer_shop_detail(
        shop_id,
        now,
        latitude=latitude,
        longitude=longitude,
    )
    return ApiResponse(data=detail)


@router.patch("/{shop_id}", response_model=ApiResponse[ShopDetailResponse])
def update_shop(
    shop_id: uuid.UUID,
    data: ShopOnboardingUpdate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[ShopDetailResponse]:
    """Update a DRAFT or REJECTED shop profile before resubmission."""
    shop = ShopService(db).update_shop(shop_id, user, data)
    return ApiResponse(data=shop)


@router.post("/{shop_id}/submit", response_model=ApiResponse[ShopDetailResponse])
def submit_shop_for_approval(
    shop_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[ShopDetailResponse]:
    """Submit a complete shop profile for admin approval (DRAFT/REJECTED → PENDING_APPROVAL)."""
    shop = ShopService(db).submit_for_approval(shop_id, user)
    return ApiResponse(data=shop)
