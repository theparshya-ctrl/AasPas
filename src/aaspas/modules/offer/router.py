import uuid
from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends, File, Query, UploadFile
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, require_permission
from aaspas.common.security.rbac import Permission
from aaspas.database import get_db
from aaspas.common.time import get_now
from aaspas.modules.offer.schemas import (
    CustomerOfferDetailResponse,
    OfferCreate,
    OfferResponse,
    OfferSubmitRequest,
    OfferUpdate,
)
from aaspas.modules.offer.service import OfferService

router = APIRouter(tags=["Offer"])


@router.post("", response_model=ApiResponse[OfferResponse], status_code=201)
def create_offer(
    data: OfferCreate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_CREATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    offer = OfferService(db).create_offer(user, data)
    return ApiResponse(data=offer)


@router.get("/shop/{shop_id}", response_model=ApiResponse[list[OfferResponse]])
def list_shop_offers(
    shop_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_READ))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[list[OfferResponse]]:
    offers = OfferService(db).list_shop_offers(shop_id, user)
    return ApiResponse(data=offers)


@router.get("/manage/{offer_id}", response_model=ApiResponse[OfferResponse])
def get_merchant_offer(
    offer_id: uuid.UUID,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_READ))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    """Full offer detail for shop owner management (includes non-customer-visible states)."""
    offer = OfferService(db).get_merchant_offer(offer_id, user)
    return ApiResponse(data=offer)


@router.post("/{offer_id}/submit", response_model=ApiResponse[OfferResponse])
def submit_offer_for_verification(
    offer_id: uuid.UUID,
    data: OfferSubmitRequest,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    offer = OfferService(db).submit_for_verification(offer_id, user, data)
    return ApiResponse(data=offer)


@router.get("/{offer_id}", response_model=ApiResponse[CustomerOfferDetailResponse])
def get_offer(
    offer_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
    now: Annotated[datetime, Depends(get_now)],
    latitude: Annotated[float | None, Query(ge=-90, le=90)] = None,
    longitude: Annotated[float | None, Query(ge=-180, le=180)] = None,
) -> ApiResponse[CustomerOfferDetailResponse]:
    """Public customer offer detail with visibility enforcement."""
    offer = OfferService(db).get_customer_offer_detail(
        offer_id,
        now,
        latitude=latitude,
        longitude=longitude,
    )
    return ApiResponse(data=offer)


@router.post("/{offer_id}/photo", response_model=ApiResponse[OfferResponse])
async def upload_offer_photo(
    offer_id: uuid.UUID,
    file: Annotated[UploadFile, File(...)],
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    """Upload an offer photo (ownership derived server-side from offer_id)."""
    content = await file.read()
    offer = OfferService(db).upload_offer_photo(offer_id, user, content, file.content_type)
    return ApiResponse(data=offer)


@router.patch("/{offer_id}", response_model=ApiResponse[OfferResponse])
def update_offer(
    offer_id: uuid.UUID,
    data: OfferUpdate,
    user: Annotated[CurrentUser, Depends(require_permission(Permission.OFFER_UPDATE_OWN))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[OfferResponse]:
    offer = OfferService(db).update_offer(offer_id, user, data)
    return ApiResponse(data=offer)
