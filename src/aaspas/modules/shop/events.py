"""Shop domain events — published via the shared in-process event bus."""

from typing import Any

from aaspas.common.events import DomainEvent, event_bus

MODULE = "shop"

SHOP_SUBMITTED_FOR_APPROVAL = "shop.submitted_for_approval"
SHOP_APPROVED = "shop.approved"
SHOP_REJECTED = "shop.rejected"
SHOP_ACTIVATED = "shop.activated"


def publish_shop_event(event_type: str, shop_id: str, **payload: Any) -> None:
    event_bus.publish(
        DomainEvent(
            event_type=event_type,
            source_module=MODULE,
            payload={"shop_id": shop_id, **payload},
        )
    )
