"""Offer domain events — published via the shared in-process event bus."""

from typing import Any

from aaspas.common.events import DomainEvent, event_bus

MODULE = "offer"

OFFER_SUBMITTED_FOR_VERIFICATION = "offer.submitted_for_verification"
OFFER_APPROVED = "offer.approved"
OFFER_REJECTED = "offer.rejected"


def publish_offer_event(event_type: str, offer_id: str, **payload: Any) -> None:
    event_bus.publish(
        DomainEvent(
            event_type=event_type,
            source_module=MODULE,
            payload={"offer_id": offer_id, **payload},
        )
    )
