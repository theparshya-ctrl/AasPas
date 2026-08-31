import uuid

from datetime import datetime



from pydantic import BaseModel, Field

from aaspas.modules.notification.types import notification_audience





class NotificationItemResponse(BaseModel):

    id: uuid.UUID

    type: str

    audience: str

    title: str

    message: str

    entity_type: str | None

    entity_id: uuid.UUID | None

    is_read: bool

    created_at: datetime



    model_config = {"from_attributes": True}



    @classmethod

    def from_model(cls, notification) -> "NotificationItemResponse":

        return cls(

            id=notification.id,

            type=notification.notification_type,

            audience=notification_audience(notification.notification_type).value,

            title=notification.title,

            message=notification.body,

            entity_type=notification.entity_type,

            entity_id=notification.entity_id,

            is_read=notification.is_read,

            created_at=notification.created_at,

        )





class NotificationListResponse(BaseModel):

    notifications: list[NotificationItemResponse]

    unread_count: int





class NotificationResponse(BaseModel):

    id: uuid.UUID

    user_id: uuid.UUID

    channel: str

    type: str

    title: str

    body: str

    entity_type: str | None

    entity_id: uuid.UUID | None

    is_read: bool

    status: str

    sent_at: datetime | None

    created_at: datetime



    model_config = {"from_attributes": True}



    @classmethod

    def from_model(cls, notification) -> "NotificationResponse":

        return cls(

            id=notification.id,

            user_id=notification.user_id,

            channel=notification.channel,

            type=notification.notification_type,

            title=notification.title,

            body=notification.body,

            entity_type=notification.entity_type,

            entity_id=notification.entity_id,

            is_read=notification.is_read,

            status=notification.status,

            sent_at=notification.sent_at,

            created_at=notification.created_at,

        )





class SendNotificationRequest(BaseModel):

    user_id: uuid.UUID

    title: str = Field(max_length=255)

    body: str

    channel: str = Field(default="in_app", max_length=50)

