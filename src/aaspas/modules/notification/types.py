"""In-app notification type constants and audience classification."""

from enum import StrEnum


class NotificationType(StrEnum):
    SHOP_SUBMITTED = "SHOP_SUBMITTED"
    SHOP_RESUBMITTED = "SHOP_RESUBMITTED"
    SHOP_APPROVED = "SHOP_APPROVED"
    SHOP_REJECTED = "SHOP_REJECTED"
    OFFER_SUBMITTED = "OFFER_SUBMITTED"
    OFFER_RESUBMITTED = "OFFER_RESUBMITTED"
    OFFER_APPROVED = "OFFER_APPROVED"
    OFFER_REJECTED = "OFFER_REJECTED"
    CUSTOMER_FAVORITE_SHOP_NEW_OFFER = "CUSTOMER_FAVORITE_SHOP_NEW_OFFER"
    CUSTOMER_FAVORITE_OFFER_ACTIVE = "CUSTOMER_FAVORITE_OFFER_ACTIVE"


class EntityType(StrEnum):
    SHOP = "shop"
    OFFER = "offer"


class NotificationAudience(StrEnum):
    BUSINESS = "business"
    CUSTOMER = "customer"
    ADMIN = "admin"


_CUSTOMER_TYPES = frozenset(
    {
        NotificationType.CUSTOMER_FAVORITE_SHOP_NEW_OFFER,
        NotificationType.CUSTOMER_FAVORITE_OFFER_ACTIVE,
    }
)

_BUSINESS_TYPES = frozenset(set(NotificationType) - _CUSTOMER_TYPES)


def notification_audience(notification_type: str) -> NotificationAudience:
    """Classify notifications for common UI sections (authoritative on backend)."""
    try:
        parsed = NotificationType(notification_type)
    except ValueError:
        return NotificationAudience.CUSTOMER
    if parsed in _CUSTOMER_TYPES:
        return NotificationAudience.CUSTOMER
    if parsed in _BUSINESS_TYPES:
        return NotificationAudience.BUSINESS
    return NotificationAudience.CUSTOMER
