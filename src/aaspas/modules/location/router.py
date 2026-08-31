import uuid
from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, require_permission
from aaspas.common.security.rbac import Permission
from aaspas.database import get_db
from aaspas.modules.location.schemas import LocationCreate, LocationResponse
from aaspas.modules.location.service import LocationService

router = APIRouter(tags=["Location"])


@router.post("", response_model=ApiResponse[LocationResponse], status_code=201)
def add_location(
    data: LocationCreate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.SHOP_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[LocationResponse]:
    location = LocationService(db).add_location(user, data)
    return ApiResponse(data=location)


@router.get("/shop/{shop_id}", response_model=ApiResponse[list[LocationResponse]])
def list_shop_locations(
    shop_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[list[LocationResponse]]:
    locations = LocationService(db).list_shop_locations(shop_id)
    return ApiResponse(data=locations)
