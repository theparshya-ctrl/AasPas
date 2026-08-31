import uuid

from sqlalchemy.orm import Session

from aaspas.modules.favorite.models import Favorite


class FavoriteRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_user_offer(self, user_id: uuid.UUID, offer_id: uuid.UUID) -> Favorite | None:
        return (
            self.db.query(Favorite)
            .filter(Favorite.user_id == user_id, Favorite.offer_id == offer_id)
            .one_or_none()
        )

    def get_by_user_shop(self, user_id: uuid.UUID, shop_id: uuid.UUID) -> Favorite | None:
        return (
            self.db.query(Favorite)
            .filter(Favorite.user_id == user_id, Favorite.shop_id == shop_id)
            .one_or_none()
        )

    def list_by_user(self, user_id: uuid.UUID) -> list[Favorite]:
        return (
            self.db.query(Favorite)
            .filter(Favorite.user_id == user_id)
            .order_by(Favorite.created_at.desc())
            .all()
        )

    def list_saved_offer_ids(self, user_id: uuid.UUID) -> set[uuid.UUID]:
        rows = (
            self.db.query(Favorite.offer_id)
            .filter(Favorite.user_id == user_id, Favorite.offer_id.isnot(None))
            .all()
        )
        return {row[0] for row in rows if row[0] is not None}

    def list_saved_shop_ids(self, user_id: uuid.UUID) -> set[uuid.UUID]:
        rows = (
            self.db.query(Favorite.shop_id)
            .filter(Favorite.user_id == user_id, Favorite.shop_id.isnot(None))
            .all()
        )
        return {row[0] for row in rows if row[0] is not None}

    def list_user_ids_for_shop(self, shop_id: uuid.UUID) -> set[uuid.UUID]:
        rows = (
            self.db.query(Favorite.user_id)
            .filter(Favorite.shop_id == shop_id)
            .all()
        )
        return {row[0] for row in rows}

    def list_user_ids_for_offer(self, offer_id: uuid.UUID) -> set[uuid.UUID]:
        rows = (
            self.db.query(Favorite.user_id)
            .filter(Favorite.offer_id == offer_id)
            .all()
        )
        return {row[0] for row in rows}

    def create(self, favorite: Favorite) -> Favorite:
        self.db.add(favorite)
        self.db.flush()
        return favorite

    def delete(self, favorite: Favorite) -> None:
        self.db.delete(favorite)
        self.db.flush()
