"""Offer field validation for merchant draft and verification submission."""

from decimal import Decimal

from aaspas.common.exceptions import ValidationAppError
from aaspas.modules.offer.models import Offer

CRITICAL_OFFER_FIELDS = frozenset(
    {
        "title",
        "description",
        "discount_type",
        "discount_value",
        "starts_at",
        "ends_at",
        "photo_url",
        "applicable_products",
        "min_purchase_amount",
        "terms",
    }
)


def validate_discount(discount_type: str, discount_value: Decimal) -> None:
    if discount_type == "percentage":
        if discount_value <= 0 or discount_value > 100:
            raise ValidationAppError(
                "Percentage discount must be between 0 and 100",
                details={"field": "discount_value"},
            )
        return
    if discount_type == "fixed":
        if discount_value <= 0:
            raise ValidationAppError(
                "Fixed discount must be greater than 0",
                details={"field": "discount_value"},
            )
        return
    raise ValidationAppError(
        "Unsupported discount type",
        details={"field": "discount_type", "value": discount_type},
    )


def validate_offer_for_submit(offer: Offer) -> None:
    missing: list[str] = []
    title = (offer.title or "").strip()
    if len(title) < 2:
        missing.append("title")

    description = (offer.description or "").strip()
    if len(description) < 10:
        missing.append("description")

    if offer.discount_type is None or offer.discount_value is None:
        missing.append("discount")
    else:
        validate_discount(offer.discount_type, offer.discount_value)

    if offer.starts_at is None:
        missing.append("starts_at")
    if offer.ends_at is None:
        missing.append("ends_at")
    if offer.starts_at is not None and offer.ends_at is not None and offer.starts_at >= offer.ends_at:
        missing.append("date_range")

    if missing:
        raise ValidationAppError(
            "Offer is incomplete and cannot be submitted for verification",
            details={"missing_fields": missing},
        )


def draft_discount_valid(discount_type: str | None, discount_value: Decimal | None) -> None:
    if discount_type is None and discount_value is None:
        return
    if discount_type is None or discount_value is None:
        raise ValidationAppError(
            "Both discount type and value are required when setting a discount",
            details={"field": "discount"},
        )
    validate_discount(discount_type, discount_value)
