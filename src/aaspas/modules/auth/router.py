from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, get_current_user
from aaspas.database import get_db
from aaspas.modules.auth.schemas import LoginRequest, RegisterRequest, TokenResponse, UserResponse
from aaspas.modules.auth.service import AuthService

router = APIRouter(tags=["Auth"])


@router.post("/register", response_model=ApiResponse[UserResponse], status_code=201)
def register(data: RegisterRequest, db: Annotated[Session, Depends(get_db)]) -> ApiResponse[UserResponse]:
    user = AuthService(db).register(data)
    return ApiResponse(data=user)


@router.post("/login", response_model=ApiResponse[TokenResponse])
def login(data: LoginRequest, db: Annotated[Session, Depends(get_db)]) -> ApiResponse[TokenResponse]:
    token = AuthService(db).login(data)
    return ApiResponse(data=token)


@router.get("/me", response_model=ApiResponse[UserResponse])
def me(
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[UserResponse]:
    profile = AuthService(db).get_profile(user.id)
    return ApiResponse(data=profile)
