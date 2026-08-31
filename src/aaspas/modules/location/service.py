import uuid

from sqlalchemy.orm import Session

from aaspas.common.audit import AuditAction, record_audit
from aaspas.common.exceptions import ForbiddenError, NotFoundError
from aaspas.common.security.auth import CurrentUser
from aaspas.common.security.rbac import UserRole
from aaspas.modules.location.models import Location
from aaspas.modules.location.repository import LocationRepository
from aaspas.modules.location.schemas import LocationCreate, LocationResponse
from aaspas.modules.shop.repository import ShopRepository


class LocationService:
    MODULE = "location"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.repo = LocationRepository(db)
        self.shop_repo = ShopRepository(db)

    def add_location(self, user: CurrentUser, data: LocationCreate) -> LocationResponse:
        shop = self.shop_repo.get_by_id(data.shop_id)
        if shop is None:
            raise NotFoundError("Shop not found")
        if user.role not in {UserRole.ADMIN, UserRole.SUPER_ADMIN} and shop.owner_id != user.id:
            raise ForbiddenError("Cannot add location to another shop")

        location = Location(**data.model_dump())
        self.repo.create(location)
        record_audit(
            self.db,
            module=self.MODULE,
            action=AuditAction.CREATE,
            resource_type="location",
            resource_id=str(location.id),
            actor_id=user.id,
            actor_role=user.role.value,
        )
        self.db.commit()
        return LocationResponse.model_validate(location)

    def list_shop_locations(self, shop_id: uuid.UUID) -> list[LocationResponse]:
        locations = self.repo.list_by_shop(shop_id)
        return [LocationResponse.model_validate(loc) for loc in locations]
