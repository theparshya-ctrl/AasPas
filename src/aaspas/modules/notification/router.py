import uuid

from typing import Annotated



from fastapi import APIRouter, Depends, Query

from sqlalchemy.orm import Session



from aaspas.common.responses import ApiResponse

from aaspas.common.security.auth import CurrentUser, get_current_user

from aaspas.database import get_db

from aaspas.modules.notification.schemas import NotificationItemResponse, NotificationListResponse

from aaspas.modules.notification.service import NotificationService



router = APIRouter(tags=["Notification"])





def _list_response(

    db: Session,

    user: CurrentUser,

    *,

    unread_only: bool = False,

) -> ApiResponse[NotificationListResponse]:

    notifications, unread_count = NotificationService(db).list_my_notifications(

        user.id,

        unread_only=unread_only,

    )

    return ApiResponse(

        data=NotificationListResponse(

            notifications=[NotificationItemResponse.from_model(n) for n in notifications],

            unread_count=unread_count,

        )

    )





@router.get("", response_model=ApiResponse[NotificationListResponse])

def list_notifications(

    user: Annotated[CurrentUser, Depends(get_current_user)],

    db: Annotated[Session, Depends(get_db)],

    unread_only: bool = Query(default=False),

) -> ApiResponse[NotificationListResponse]:

    return _list_response(db, user, unread_only=unread_only)





@router.get("/me", response_model=ApiResponse[NotificationListResponse])

def list_my_notifications(

    user: Annotated[CurrentUser, Depends(get_current_user)],

    db: Annotated[Session, Depends(get_db)],

    unread_only: bool = Query(default=False),

) -> ApiResponse[NotificationListResponse]:

    return _list_response(db, user, unread_only=unread_only)





@router.post("/{notification_id}/read", response_model=ApiResponse[NotificationItemResponse])

def mark_notification_read(

    notification_id: uuid.UUID,

    user: Annotated[CurrentUser, Depends(get_current_user)],

    db: Annotated[Session, Depends(get_db)],

) -> ApiResponse[NotificationItemResponse]:

    notification = NotificationService(db).mark_read(notification_id, user.id)

    return ApiResponse(data=NotificationItemResponse.from_model(notification))





@router.post("/read-all", response_model=ApiResponse[dict[str, int]])

def mark_all_notifications_read(

    user: Annotated[CurrentUser, Depends(get_current_user)],

    db: Annotated[Session, Depends(get_db)],

) -> ApiResponse[dict[str, int]]:

    updated = NotificationService(db).mark_all_read(user.id)

    return ApiResponse(data={"marked_read": updated})

