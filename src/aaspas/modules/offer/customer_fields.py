"""Customer-facing offer source/verification presentation helpers."""

from aaspas.common.source_type import SourceType
from aaspas.modules.offer.models import Offer


def customer_offer_is_verified(offer: Offer) -> bool:
    return offer.source_type == SourceType.AASPAS.value and offer.is_verified


def customer_offer_source_type(offer: Offer) -> str:
    return offer.source_type


def customer_offer_source_name(offer: Offer) -> str | None:
    if offer.source_type != SourceType.EXTERNAL.value:
        return None
    return offer.source_name
