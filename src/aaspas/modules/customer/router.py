from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, require_permission
from aaspas.common.security.rbac import Permission
from aaspas.database import get_db
from aaspas.modules.customer.schemas import (
    CustomerProfileCreate,
    CustomerProfileResponse,
    CustomerProfileUpdate,
)
from aaspas.modules.customer.service import CustomerService

router = APIRouter(tags=["Customer"])


@router.post("", response_model=ApiResponse[CustomerProfileResponse], status_code=201)
def create_profile(
    data: CustomerProfileCreate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.CUSTOMER_UPDATE_SELF))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[CustomerProfileResponse]:
    profile = CustomerService(db).create_profile(user, data)
    return ApiResponse(data=profile)


@router.get("/me", response_model=ApiResponse[CustomerProfileResponse])
def get_my_profile(
    user: Annotated[CurrentUser, Depends(require_permission(Permission.CUSTOMER_READ_SELF))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[CustomerProfileResponse]:
    profile = CustomerService(db).get_my_profile(user)
    return ApiResponse(data=profile)


@router.patch("/me", response_model=ApiResponse[CustomerProfileResponse])
def update_my_profile(
    data: CustomerProfileUpdate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.CUSTOMER_UPDATE_SELF))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[CustomerProfileResponse]:
    profile = CustomerService(db).update_my_profile(user, data)
    return ApiResponse(data=profile)
