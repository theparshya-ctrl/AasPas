"""Customer offer visibility — timestamp rules are the source of truth."""

from datetime import datetime
from enum import StrEnum

from aaspas.common.source_type import SourceType
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.status import OfferStatus
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus


class CustomerOfferVisibility(StrEnum):
    COMING_SOON = "coming_soon"
    ACTIVE = "active"


def resolve_customer_visibility(
    offer: Offer, shop: Shop, now: datetime
) -> CustomerOfferVisibility | None:
    """Return customer-visible state or None if the offer must be hidden."""
    if offer.status in {
        OfferStatus.DRAFT,
        OfferStatus.PENDING_APPROVAL,
        OfferStatus.REJECTED,
        OfferStatus.EXPIRED,
    }:
        return None

    if OfferStatus(offer.status) not in OfferStatus.customer_approved():
        return None

    if shop.status != ShopStatus.ACTIVE.value:
        return None

    if offer.source_type == SourceType.EXTERNAL.value:
        return _external_visibility(offer, now)

    if offer.starts_at is None or offer.ends_at is None:
        return None

    starts_at = _ensure_aware(offer.starts_at)
    ends_at = _ensure_aware(offer.ends_at)
    now = _ensure_aware(now)

    if ends_at <= now:
        return None

    if starts_at > now:
        return CustomerOfferVisibility.COMING_SOON

    if starts_at <= now < ends_at:
        return CustomerOfferVisibility.ACTIVE

    return None


def _external_visibility(offer: Offer, now: datetime) -> CustomerOfferVisibility | None:
    now = _ensure_aware(now)
    if offer.ends_at is not None:
        ends_at = _ensure_aware(offer.ends_at)
        if ends_at <= now:
            return None
    effective_start = offer.starts_at or offer.collected_at
    if effective_start is not None and _ensure_aware(effective_start) > now:
        return CustomerOfferVisibility.COMING_SOON
    return CustomerOfferVisibility.ACTIVE


def _ensure_aware(dt: datetime) -> datetime:
    if dt.tzinfo is None:
        from datetime import UTC

        return dt.replace(tzinfo=UTC)
    return dt
