"""External offer import validation — CURSOR-045 policy."""

from __future__ import annotations

import re
from enum import StrEnum

from aaspas.common.exceptions import ValidationAppError
from aaspas.modules.external.schemas import ExternalImportAddress, ExternalOfferImportRecord

VAGUE_OFFER_PATTERNS = (
    re.compile(r"\bgreat deals\b", re.I),
    re.compile(r"\bhuge discounts?\b", re.I),
    re.compile(r"\bbest prices?\b", re.I),
    re.compile(r"\bamazing offers?\b", re.I),
    re.compile(r"\bvisit us\b", re.I),
    re.compile(r"\bcall now\b", re.I),
)

AREA_ONLY_VALUES = frozenset(
    {
        "pimpri",
        "chinchwad",
        "hadapsar",
        "manjri",
        "manjri budruk",
        "saswad",
        "pune",
        "pimpri-chinchwad",
    }
)


class ExternalValidationOutcome(StrEnum):
    VALID = "VALID"
    NEEDS_REVIEW = "NEEDS_REVIEW"
    REJECTED = "REJECTED"


def is_vague_offer_text(title: str, description: str | None = None) -> bool:
    combined = f"{title} {description or ''}".strip()
    if len(combined) < 8:
        return True
    return any(pattern.search(combined) for pattern in VAGUE_OFFER_PATTERNS)


def has_identifiable_discount(record: ExternalOfferImportRecord) -> bool:
    title = record.title.strip()
    if is_vague_offer_text(title, record.description):
        return False
    if record.discount_value is not None and record.discount_value > 0:
        return True
    return bool(re.search(r"[\d₹%]|off|save|buy|get|flat", title, re.I))


def assess_address(address: ExternalImportAddress) -> tuple[ExternalValidationOutcome, list[str]]:
    line = address.address_line1.strip().lower()
    city = address.city.strip()
    if len(city) < 2:
        return ExternalValidationOutcome.NEEDS_REVIEW, ["city missing or too vague"]
    if line in AREA_ONLY_VALUES:
        return ExternalValidationOutcome.NEEDS_REVIEW, ["address appears to be area-only without street or venue detail"]
    if len(address.address_line1.strip()) < 5:
        return ExternalValidationOutcome.NEEDS_REVIEW, ["street or venue detail is insufficient"]
    return ExternalValidationOutcome.VALID, []


def classify_import_record(
    record: ExternalOfferImportRecord,
    *,
    active_categories: set[str],
) -> tuple[ExternalValidationOutcome, list[str]]:
    reasons: list[str] = []

    if not record.source_name.strip():
        return ExternalValidationOutcome.REJECTED, ["source_name is required"]
    if not record.source_url.startswith(("http://", "https://")):
        return ExternalValidationOutcome.REJECTED, ["source_url must be http(s)"]
    if not record.collected_at:
        return ExternalValidationOutcome.REJECTED, ["collected_at is required"]
    if not record.external_source_key or len(record.external_source_key.strip()) < 8:
        return ExternalValidationOutcome.REJECTED, ["external_source_key must be at least 8 characters"]

    if is_vague_offer_text(record.title, record.description):
        return ExternalValidationOutcome.REJECTED, ["offer wording is too vague to identify a specific promotion"]

    if not has_identifiable_discount(record):
        return ExternalValidationOutcome.REJECTED, ["discount or offer amount cannot be determined"]

    normalized_category = record.category.strip().lower()
    active_normalized = {name.strip().lower() for name in active_categories}
    if normalized_category not in active_normalized:
        reasons.append(
            f"category '{record.category}' does not match an active category "
            f"({', '.join(sorted(active_categories))})"
        )

    address_outcome, address_reasons = assess_address(record.address)
    if address_outcome == ExternalValidationOutcome.NEEDS_REVIEW:
        return ExternalValidationOutcome.NEEDS_REVIEW, address_reasons + reasons

    if record.ends_at is not None:
        starts = record.starts_at or record.collected_at
        if starts is not None and record.ends_at <= starts:
            return ExternalValidationOutcome.REJECTED, ["ends_at must be after starts_at/collected_at"]

    if reasons:
        return ExternalValidationOutcome.NEEDS_REVIEW, reasons
    return ExternalValidationOutcome.VALID, []


def validate_for_import(
    record: ExternalOfferImportRecord,
    *,
    active_categories: set[str],
    now,
    allow_needs_review: bool = False,
) -> None:
    """Raise ValidationAppError when a record must not be imported."""
    from datetime import UTC

    if record.ends_at is not None:
        aware_now = now if now.tzinfo else now.replace(tzinfo=UTC)
        ends = record.ends_at if record.ends_at.tzinfo else record.ends_at.replace(tzinfo=UTC)
        if ends <= aware_now:
            raise ValidationAppError("External offer is explicitly expired")

    outcome, reasons = classify_import_record(record, active_categories=active_categories)
    if outcome == ExternalValidationOutcome.REJECTED:
        raise ValidationAppError(reasons[0], details={"reasons": reasons})
    if outcome == ExternalValidationOutcome.NEEDS_REVIEW and not allow_needs_review:
        raise ValidationAppError(
            "External offer needs review before import",
            details={"reasons": reasons},
        )
