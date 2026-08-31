import uuid

from sqlalchemy.orm import Session

from aaspas.modules.location.models import Location


class LocationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_by_shop(self, shop_id: uuid.UUID) -> list[Location]:
        return self.db.query(Location).filter(Location.shop_id == shop_id).all()

    def get_primary_by_shop(self, shop_id: uuid.UUID) -> Location | None:
        return (
            self.db.query(Location)
            .filter(Location.shop_id == shop_id, Location.is_primary.is_(True))
            .one_or_none()
        )

    def create(self, location: Location) -> Location:
        self.db.add(location)
        self.db.flush()
        return location

    def save(self, location: Location) -> Location:
        self.db.add(location)
        self.db.flush()
        return location
