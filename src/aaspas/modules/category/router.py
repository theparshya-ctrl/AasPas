import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.config import get_settings
from aaspas.database import get_db
from aaspas.modules.category.repository import CategoryRepository
from aaspas.modules.category.schemas import CategoryOffersResponse, CategoryResponse
from aaspas.common.time import get_now
from aaspas.modules.search.service import SearchService

router = APIRouter(tags=["Category"])


@router.get("", response_model=ApiResponse[list[CategoryResponse]])
def list_categories(
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[list[CategoryResponse]]:
    categories = CategoryRepository(db).list_active()
    return ApiResponse(data=[CategoryResponse.model_validate(c) for c in categories])


@router.get("/{category_id}/offers", response_model=ApiResponse[CategoryOffersResponse])
def list_category_offers(
    category_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
    radius_km: Annotated[float | None, Query(gt=0, le=100)] = None,
    page: Annotated[int, Query(ge=1)] = 1,
    page_size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> ApiResponse[CategoryOffersResponse]:
    """Customer-visible offers for a category, split by active and coming soon."""
    results = SearchService(db, get_settings()).category_offers(
        category_id,
        now,
        latitude=latitude,
        longitude=longitude,
        radius_km=radius_km,
        page=page,
        page_size=page_size,
    )
    return ApiResponse(
        data=results,
        meta={
            "page": results.page,
            "page_size": results.page_size,
            "total_active": results.total_active,
            "total_coming_soon": results.total_coming_soon,
            "location_provided": latitude is not None and longitude is not None,
        },
    )
