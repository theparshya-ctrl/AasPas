from enum import StrEnum


class ShopStatus(StrEnum):
    DRAFT = "draft"
    PENDING_APPROVAL = "pending_approval"
    REJECTED = "rejected"
    ACTIVE = "active"

    @classmethod
    def submittable(cls) -> set["ShopStatus"]:
        return {cls.DRAFT, cls.REJECTED}

    @classmethod
    def editable_by_owner(cls) -> set["ShopStatus"]:
        return {cls.DRAFT, cls.REJECTED}
