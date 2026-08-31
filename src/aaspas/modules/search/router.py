import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, get_current_user_optional
from aaspas.common.security.rbac import UserRole
from aaspas.config import get_settings
from aaspas.database import get_db
from aaspas.common.time import get_now
from aaspas.modules.search.schemas import CustomerSearchResults, SearchQuery
from aaspas.modules.search.service import SearchService

router = APIRouter(tags=["Search"])


@router.get("", response_model=ApiResponse[CustomerSearchResults])
def search(
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    q: Annotated[str | None, Query(max_length=255)] = None,
    category_id: Annotated[uuid.UUID | None, Query()] = None,
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
    radius_km: Annotated[float | None, Query(gt=0, le=100)] = None,
    page: Annotated[int, Query(ge=1)] = 1,
    page_size: Annotated[int, Query(ge=1, le=100)] = 20,
    user: Annotated[CurrentUser | None, Depends(get_current_user_optional)] = None,
) -> ApiResponse[CustomerSearchResults]:
    """Public customer offer search. Optional auth adds is_saved flags."""
    query = SearchQuery(
        q=q,
        category_id=category_id,
        latitude=latitude,
        longitude=longitude,
        radius_km=radius_km,
        page=page,
        page_size=page_size,
    )
    user_id = user.id if user is not None and user.role == UserRole.CUSTOMER else None
    results = SearchService(db, get_settings()).search(query, now, user_id=user_id)
    return ApiResponse(
        data=results,
        meta={
            "page": results.page,
            "page_size": results.page_size,
            "total": results.total,
            "total_pages": results.total_pages,
            "location_provided": latitude is not None and longitude is not None,
            "authenticated": user is not None,
        },
    )
