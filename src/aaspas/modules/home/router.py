from datetime import datetime
from typing import Annotated
import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, get_current_user_optional
from aaspas.common.security.rbac import UserRole
from aaspas.common.time import get_now
from aaspas.config import get_settings
from aaspas.database import get_db
from aaspas.modules.favorite.repository import FavoriteRepository
from aaspas.modules.home.schemas import HomeQueryParams, HomeResponse
from aaspas.modules.home.service import HomeService

router = APIRouter(tags=["Home"])


@router.get("", response_model=ApiResponse[HomeResponse])
def get_home(
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
    radius_km: Annotated[float | None, Query(gt=0, le=100)] = None,
    user: Annotated[CurrentUser | None, Depends(get_current_user_optional)] = None,
) -> ApiResponse[HomeResponse]:
    """Public customer home feed. Optional auth adds is_saved flags for favorites."""
    params = HomeQueryParams(latitude=latitude, longitude=longitude, radius_km=radius_km)
    settings = get_settings()

    saved_offer_ids: set[uuid.UUID] = set()
    saved_shop_ids: set[uuid.UUID] = set()
    if user is not None and user.role == UserRole.CUSTOMER:
        fav_repo = FavoriteRepository(db)
        saved_offer_ids = fav_repo.list_saved_offer_ids(user.id)
        saved_shop_ids = fav_repo.list_saved_shop_ids(user.id)

    data = HomeService(db, settings).get_home(
        params,
        now,
        saved_offer_ids=saved_offer_ids,
        saved_shop_ids=saved_shop_ids,
    )
    return ApiResponse(
        data=data,
        meta={
            "location_provided": params.latitude is not None and params.longitude is not None,
            "nearby_radius_km": params.radius_km or settings.nearby_radius_km,
            "authenticated": user is not None,
        },
    )
