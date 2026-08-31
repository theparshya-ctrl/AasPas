import uuid

from collections.abc import Callable

from datetime import UTC, datetime

from typing import Any



from sqlalchemy.exc import IntegrityError

from sqlalchemy.orm import Session



from aaspas.common.events import DomainEvent, event_bus

from aaspas.common.exceptions import NotFoundError

from aaspas.database import SessionLocal

from aaspas.modules.notification.models import Notification

from aaspas.modules.notification.types import EntityType, NotificationType

from aaspas.modules.offer.events import (

    OFFER_APPROVED,

    OFFER_REJECTED,

    OFFER_SUBMITTED_FOR_VERIFICATION,

)

from aaspas.modules.offer.models import Offer

from aaspas.modules.shop.events import SHOP_APPROVED, SHOP_REJECTED, SHOP_SUBMITTED_FOR_APPROVAL

from aaspas.modules.favorite.repository import FavoriteRepository

from aaspas.modules.offer.visibility import resolve_customer_visibility

from aaspas.modules.shop.models import Shop





class NotificationRepository:

    def __init__(self, db: Session) -> None:

        self.db = db



    def list_for_user(

        self,

        user_id: uuid.UUID,

        *,

        limit: int = 50,

        unread_only: bool = False,

    ) -> list[Notification]:

        query = self.db.query(Notification).filter(Notification.user_id == user_id)

        if unread_only:

            query = query.filter(Notification.is_read.is_(False))

        return (

            query.order_by(Notification.created_at.desc())

            .limit(limit)

            .all()

        )



    def count_unread(self, user_id: uuid.UUID) -> int:

        return (

            self.db.query(Notification)

            .filter(Notification.user_id == user_id, Notification.is_read.is_(False))

            .count()

        )



    def get_by_id_for_user(self, notification_id: uuid.UUID, user_id: uuid.UUID) -> Notification | None:

        return (

            self.db.query(Notification)

            .filter(Notification.id == notification_id, Notification.user_id == user_id)

            .one_or_none()

        )



    def get_by_dedupe_key(self, dedupe_key: str) -> Notification | None:

        return (

            self.db.query(Notification)

            .filter(Notification.dedupe_key == dedupe_key)

            .one_or_none()

        )



    def create(self, notification: Notification) -> Notification:

        self.db.add(notification)

        self.db.flush()

        return notification





class NotificationService:

    MODULE = "notification"



    def __init__(self, db: Session) -> None:

        self.db = db

        self.repo = NotificationRepository(db)



    def list_my_notifications(

        self,

        user_id: uuid.UUID,

        *,

        limit: int = 50,

        unread_only: bool = False,

    ) -> tuple[list[Notification], int]:

        notifications = self.repo.list_for_user(user_id, limit=limit, unread_only=unread_only)

        unread_count = self.repo.count_unread(user_id)

        return notifications, unread_count



    def mark_read(self, notification_id: uuid.UUID, user_id: uuid.UUID) -> Notification:

        notification = self.repo.get_by_id_for_user(notification_id, user_id)

        if notification is None:

            raise NotFoundError("Notification not found")

        if not notification.is_read:

            notification.is_read = True

            notification.read_at = datetime.now(UTC)

            self.db.commit()

            self.db.refresh(notification)

        return notification



    def mark_all_read(self, user_id: uuid.UUID) -> int:

        now = datetime.now(UTC)

        updated = (

            self.db.query(Notification)

            .filter(Notification.user_id == user_id, Notification.is_read.is_(False))

            .update({"is_read": True, "read_at": now}, synchronize_session=False)

        )

        self.db.commit()

        return updated



    def create_in_app(

        self,

        *,

        user_id: uuid.UUID,

        notification_type: NotificationType,

        title: str,

        body: str,

        entity_type: EntityType,

        entity_id: uuid.UUID,

        dedupe_key: str,

    ) -> Notification | None:

        if self.repo.get_by_dedupe_key(dedupe_key):

            return None



        now = datetime.now(UTC)

        notification = Notification(

            user_id=user_id,

            channel="in_app",

            notification_type=notification_type.value,

            title=title,

            body=body,

            entity_type=entity_type.value,

            entity_id=entity_id,

            is_read=False,

            dedupe_key=dedupe_key,

            status="delivered",

            sent_at=now,

        )

        try:

            self.repo.create(notification)

            self.db.commit()

            self._push_realtime(notification)

            return notification

        except IntegrityError:

            self.db.rollback()

            return None

    def _push_realtime(self, notification: Notification) -> None:

        from aaspas.modules.notification.schemas import NotificationItemResponse

        from aaspas.modules.notification.ws_manager import notification_ws_manager

        unread_count = self.repo.count_unread(notification.user_id)

        payload = {

            "type": "notification",

            "notification": NotificationItemResponse.from_model(notification).model_dump(

                mode="json",

            ),

            "unread_count": unread_count,

        }

        notification_ws_manager.schedule_push(notification.user_id, payload)



    def handle_event(self, event: DomainEvent) -> None:

        handler = _EVENT_HANDLERS.get(event.event_type)

        if handler is not None:

            handler(self, event)





def _parse_uuid(value: Any) -> uuid.UUID | None:

    if value is None:

        return None

    try:

        return uuid.UUID(str(value))

    except (TypeError, ValueError):

        return None





def _shop_name(db: Session, shop_id: uuid.UUID) -> str:

    shop = db.get(Shop, shop_id)

    return shop.name if shop else "your shop"





def _offer_title(db: Session, offer_id: uuid.UUID) -> str:

    offer = db.get(Offer, offer_id)

    return offer.title if offer else "your offer"





def _shop_owner_id(db: Session, shop_id: uuid.UUID) -> uuid.UUID | None:

    shop = db.get(Shop, shop_id)

    return shop.owner_id if shop else None





def _handle_shop_submitted(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    owner_id = _parse_uuid(payload.get("owner_id"))

    shop_id = _parse_uuid(payload.get("shop_id"))

    if owner_id is None or shop_id is None:

        return



    shop_name = _shop_name(service.db, shop_id)

    is_resubmit = bool(payload.get("is_resubmit"))

    if is_resubmit:

        notification_type = NotificationType.SHOP_RESUBMITTED

        title = "Shop resubmitted"

        body = f'Your shop "{shop_name}" has been resubmitted for verification.'

    else:

        notification_type = NotificationType.SHOP_SUBMITTED

        title = "Shop submitted for verification"

        body = f'Your shop "{shop_name}" has been submitted for verification.'



    service.create_in_app(

        user_id=owner_id,

        notification_type=notification_type,

        title=title,

        body=body,

        entity_type=EntityType.SHOP,

        entity_id=shop_id,

        dedupe_key=event.event_id,

    )





def _handle_shop_approved(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    owner_id = _parse_uuid(payload.get("owner_id"))

    shop_id = _parse_uuid(payload.get("shop_id"))

    if owner_id is None or shop_id is None:

        return



    shop_name = _shop_name(service.db, shop_id)

    service.create_in_app(

        user_id=owner_id,

        notification_type=NotificationType.SHOP_APPROVED,

        title="Shop approved",

        body=f'Your shop "{shop_name}" has been approved.',

        entity_type=EntityType.SHOP,

        entity_id=shop_id,

        dedupe_key=event.event_id,

    )





def _handle_shop_rejected(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    owner_id = _parse_uuid(payload.get("owner_id"))

    shop_id = _parse_uuid(payload.get("shop_id"))

    reason = str(payload.get("reason") or "").strip()

    if owner_id is None or shop_id is None:

        return



    shop_name = _shop_name(service.db, shop_id)

    body = f'Your shop "{shop_name}" needs changes.'

    if reason:

        body = f'{body} Reason: {reason}'



    service.create_in_app(

        user_id=owner_id,

        notification_type=NotificationType.SHOP_REJECTED,

        title="Shop rejected",

        body=body,

        entity_type=EntityType.SHOP,

        entity_id=shop_id,

        dedupe_key=event.event_id,

    )





def _handle_offer_submitted(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    owner_id = _parse_uuid(payload.get("owner_id"))

    offer_id = _parse_uuid(payload.get("offer_id"))

    if owner_id is None or offer_id is None:

        return



    offer_title = _offer_title(service.db, offer_id)

    is_resubmit = bool(payload.get("is_resubmit"))

    if is_resubmit:

        notification_type = NotificationType.OFFER_RESUBMITTED

        title = "Offer resubmitted"

        body = f'Your offer "{offer_title}" has been resubmitted for verification.'

    else:

        notification_type = NotificationType.OFFER_SUBMITTED

        title = "Offer submitted for verification"

        body = f'Your offer "{offer_title}" has been submitted for verification.'



    service.create_in_app(

        user_id=owner_id,

        notification_type=notification_type,

        title=title,

        body=body,

        entity_type=EntityType.OFFER,

        entity_id=offer_id,

        dedupe_key=event.event_id,

    )





def _customer_notification_dedupe_key(

    notification_type: NotificationType,

    user_id: uuid.UUID,

    offer_id: uuid.UUID,

    event_id: str,

) -> str:

    return f"{notification_type.value}:{user_id}:{offer_id}:{event_id}"





def _notify_customers_on_offer_approved(

    service: NotificationService,

    *,

    event: DomainEvent,

    offer_id: uuid.UUID,

    shop_id: uuid.UUID,

) -> None:

    offer = service.db.get(Offer, offer_id)

    shop = service.db.get(Shop, shop_id)

    if offer is None or shop is None:

        return



    now = datetime.now(UTC)

    if resolve_customer_visibility(offer, shop, now) is None:

        return



    favorite_repo = FavoriteRepository(service.db)

    shop_name = shop.name

    offer_title = offer.title

    shop_favoriter_ids = favorite_repo.list_user_ids_for_shop(shop_id)

    offer_favoriter_ids = favorite_repo.list_user_ids_for_offer(offer_id)



    for user_id in shop_favoriter_ids:

        service.create_in_app(

            user_id=user_id,

            notification_type=NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER,

            title="New offer",

            body=f"New offer from {shop_name}",

            entity_type=EntityType.OFFER,

            entity_id=offer_id,

            dedupe_key=_customer_notification_dedupe_key(

                NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER,

                user_id,

                offer_id,

                event.event_id,

            ),

        )



    for user_id in offer_favoriter_ids:

        service.create_in_app(

            user_id=user_id,

            notification_type=NotificationType.CUSTOMER_FAVORITE_OFFER_ACTIVE,

            title="Saved offer active",

            body=f'Your saved offer "{offer_title}" is now active.',

            entity_type=EntityType.OFFER,

            entity_id=offer_id,

            dedupe_key=_customer_notification_dedupe_key(

                NotificationType.CUSTOMER_FAVORITE_OFFER_ACTIVE,

                user_id,

                offer_id,

                event.event_id,

            ),

        )





def _handle_offer_approved(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    offer_id = _parse_uuid(payload.get("offer_id"))

    shop_id = _parse_uuid(payload.get("shop_id"))

    if offer_id is None or shop_id is None:

        return



    owner_id = _parse_uuid(payload.get("owner_id")) or _shop_owner_id(service.db, shop_id)

    if owner_id is None:

        return



    offer_title = _offer_title(service.db, offer_id)

    service.create_in_app(

        user_id=owner_id,

        notification_type=NotificationType.OFFER_APPROVED,

        title="Offer approved",

        body=f'Your offer "{offer_title}" has been approved.',

        entity_type=EntityType.OFFER,

        entity_id=offer_id,

        dedupe_key=event.event_id,

    )



    _notify_customers_on_offer_approved(

        service,

        event=event,

        offer_id=offer_id,

        shop_id=shop_id,

    )





def _handle_offer_rejected(service: NotificationService, event: DomainEvent) -> None:

    payload = event.payload

    offer_id = _parse_uuid(payload.get("offer_id"))

    shop_id = _parse_uuid(payload.get("shop_id"))

    reason = str(payload.get("reason") or "").strip()

    if offer_id is None or shop_id is None:

        return



    owner_id = _parse_uuid(payload.get("owner_id")) or _shop_owner_id(service.db, shop_id)

    if owner_id is None:

        return



    offer_title = _offer_title(service.db, offer_id)

    body = f'Your offer "{offer_title}" needs changes.'

    if reason:

        body = f'{body} Reason: {reason}'



    service.create_in_app(

        user_id=owner_id,

        notification_type=NotificationType.OFFER_REJECTED,

        title="Offer rejected",

        body=body,

        entity_type=EntityType.OFFER,

        entity_id=offer_id,

        dedupe_key=event.event_id,

    )





_EVENT_HANDLERS: dict[str, Callable[[NotificationService, DomainEvent], None]] = {

    SHOP_SUBMITTED_FOR_APPROVAL: _handle_shop_submitted,

    SHOP_APPROVED: _handle_shop_approved,

    SHOP_REJECTED: _handle_shop_rejected,

    OFFER_SUBMITTED_FOR_VERIFICATION: _handle_offer_submitted,

    OFFER_APPROVED: _handle_offer_approved,

    OFFER_REJECTED: _handle_offer_rejected,

}





def _with_db(handler: Callable[[NotificationService, DomainEvent], None]) -> Callable[[DomainEvent], None]:

    def wrapper(event: DomainEvent) -> None:

        db = SessionLocal()

        try:

            handler(NotificationService(db), event)

        except Exception:

            db.rollback()

            raise

        finally:

            db.close()



    return wrapper





for _event_type, _handler in _EVENT_HANDLERS.items():

    event_bus.subscribe(_event_type, _with_db(_handler))


