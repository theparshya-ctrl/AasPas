from enum import StrEnum


class OfferStatus(StrEnum):
    DRAFT = "draft"
    PENDING_APPROVAL = "pending_approval"
    REJECTED = "rejected"
    SCHEDULED = "scheduled"
    ACTIVE = "active"
    EXPIRED = "expired"

    @classmethod
    def customer_approved(cls) -> set["OfferStatus"]:
        return {cls.SCHEDULED, cls.ACTIVE}

    @classmethod
    def merchant_editable(cls) -> set["OfferStatus"]:
        return {cls.DRAFT, cls.REJECTED}

    @classmethod
    def submittable(cls) -> set["OfferStatus"]:
        return {cls.DRAFT, cls.REJECTED}
