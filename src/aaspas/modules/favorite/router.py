import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, get_current_user
from aaspas.database import get_db
from aaspas.modules.favorite.schemas import FavoriteActionResponse, FavoritesResponse
from aaspas.modules.favorite.service import FavoriteService
from aaspas.common.time import get_now

router = APIRouter(tags=["Favorite"])


@router.get("", response_model=ApiResponse[FavoritesResponse])
def list_favorites(
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
) -> ApiResponse[FavoritesResponse]:
    data = FavoriteService(db).list_favorites(
        user, now, latitude=latitude, longitude=longitude
    )
    return ApiResponse(data=data)


@router.post("/offers/{offer_id}", response_model=ApiResponse[FavoriteActionResponse], status_code=201)
def save_offer_favorite(
    offer_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
) -> ApiResponse[FavoriteActionResponse]:
    data = FavoriteService(db).save_offer(user, offer_id, now)
    return ApiResponse(data=data)


@router.delete("/offers/{offer_id}", response_model=ApiResponse[FavoriteActionResponse])
def unsave_offer_favorite(
    offer_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[FavoriteActionResponse]:
    data = FavoriteService(db).unsave_offer(user, offer_id)
    return ApiResponse(data=data)


@router.post("/shops/{shop_id}", response_model=ApiResponse[FavoriteActionResponse], status_code=201)
def save_shop_favorite(
    shop_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[FavoriteActionResponse]:
    data = FavoriteService(db).save_shop(user, shop_id)
    return ApiResponse(data=data)


@router.delete("/shops/{shop_id}", response_model=ApiResponse[FavoriteActionResponse])
def unsave_shop_favorite(
    shop_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[FavoriteActionResponse]:
    data = FavoriteService(db).unsave_shop(user, shop_id)
    return ApiResponse(data=data)
