import uuid
from datetime import datetime

from sqlalchemy.orm import Session

from aaspas.common.exceptions import ForbiddenError, NotFoundError, ValidationAppError
from aaspas.common.geo import haversine_km
from aaspas.common.storage import resolve_customer_offer_photo_url, resolve_customer_shop_photo_url
from aaspas.config import get_settings
from aaspas.common.security.auth import CurrentUser
from aaspas.common.security.rbac import UserRole
from aaspas.modules.category.models import Category
from aaspas.modules.favorite.models import Favorite
from aaspas.modules.favorite.repository import FavoriteRepository
from aaspas.modules.favorite.schemas import (
    FavoriteActionResponse,
    FavoriteOfferItem,
    FavoriteShopItem,
    FavoritesResponse,
)
from aaspas.modules.location.models import Location
from aaspas.modules.location.repository import LocationRepository
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.customer_fields import (
    customer_offer_is_verified,
    customer_offer_source_name,
    customer_offer_source_type,
)
from aaspas.modules.offer.visibility import resolve_customer_visibility
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.repository import ShopRepository
from aaspas.modules.shop.status import ShopStatus


class FavoriteService:
    MODULE = "favorite"

    def __init__(self, db: Session) -> None:
        self.db = db
        self.settings = get_settings()
        self.repo = FavoriteRepository(db)
        self.offer_repo = OfferRepository(db)
        self.shop_repo = ShopRepository(db)
        self.location_repo = LocationRepository(db)

    def save_offer(
        self, user: CurrentUser, offer_id: uuid.UUID, now: datetime
    ) -> FavoriteActionResponse:
        self._require_customer(user)
        self._assert_offer_saveable(offer_id, now)
        existing = self.repo.get_by_user_offer(user.id, offer_id)
        if existing is not None:
            return FavoriteActionResponse(
                favorite_id=existing.id, offer_id=offer_id, saved=True
            )

        favorite = Favorite(user_id=user.id, offer_id=offer_id)
        self.repo.create(favorite)
        self.db.commit()
        return FavoriteActionResponse(favorite_id=favorite.id, offer_id=offer_id, saved=True)

    def unsave_offer(self, user: CurrentUser, offer_id: uuid.UUID) -> FavoriteActionResponse:
        self._require_customer(user)
        favorite = self.repo.get_by_user_offer(user.id, offer_id)
        if favorite is None:
            raise NotFoundError("Favorite not found")
        self.repo.delete(favorite)
        self.db.commit()
        return FavoriteActionResponse(favorite_id=favorite.id, offer_id=offer_id, saved=False)

    def save_shop(self, user: CurrentUser, shop_id: uuid.UUID) -> FavoriteActionResponse:
        self._require_customer(user)
        self._assert_shop_saveable(shop_id)
        existing = self.repo.get_by_user_shop(user.id, shop_id)
        if existing is not None:
            return FavoriteActionResponse(favorite_id=existing.id, shop_id=shop_id, saved=True)

        favorite = Favorite(user_id=user.id, shop_id=shop_id)
        self.repo.create(favorite)
        self.db.commit()
        return FavoriteActionResponse(favorite_id=favorite.id, shop_id=shop_id, saved=True)

    def unsave_shop(self, user: CurrentUser, shop_id: uuid.UUID) -> FavoriteActionResponse:
        self._require_customer(user)
        favorite = self.repo.get_by_user_shop(user.id, shop_id)
        if favorite is None:
            raise NotFoundError("Favorite not found")
        self.repo.delete(favorite)
        self.db.commit()
        return FavoriteActionResponse(favorite_id=favorite.id, shop_id=shop_id, saved=False)

    def list_favorites(
        self,
        user: CurrentUser,
        now: datetime,
        *,
        latitude: float | None = None,
        longitude: float | None = None,
    ) -> FavoritesResponse:
        self._require_customer(user)
        favorites = self.repo.list_by_user(user.id)
        offer_items: list[FavoriteOfferItem] = []
        shop_items: list[FavoriteShopItem] = []

        for favorite in favorites:
            if favorite.offer_id is not None:
                item = self._map_offer_favorite(favorite, now, latitude, longitude)
                if item is not None:
                    offer_items.append(item)
            elif favorite.shop_id is not None:
                item = self._map_shop_favorite(favorite, latitude, longitude)
                if item is not None:
                    shop_items.append(item)

        return FavoritesResponse(offers=offer_items, shops=shop_items)

    def get_saved_offer_ids(self, user_id: uuid.UUID) -> set[uuid.UUID]:
        return self.repo.list_saved_offer_ids(user_id)

    def get_saved_shop_ids(self, user_id: uuid.UUID) -> set[uuid.UUID]:
        return self.repo.list_saved_shop_ids(user_id)

    def _assert_offer_saveable(self, offer_id: uuid.UUID, now: datetime) -> None:
        row = self.offer_repo.get_customer_offer_context(offer_id)
        if row is None:
            raise NotFoundError("Offer not found")
        offer, shop, _, _ = row
        if resolve_customer_visibility(offer, shop, now) is None:
            raise ValidationAppError("Offer is not available to save")

    def _assert_shop_saveable(self, shop_id: uuid.UUID) -> None:
        shop = self.shop_repo.get_by_id(shop_id)
        if shop is None or shop.status != ShopStatus.ACTIVE.value:
            raise ValidationAppError("Shop is not available to save")

    def _map_offer_favorite(
        self,
        favorite: Favorite,
        now: datetime,
        latitude: float | None,
        longitude: float | None,
    ) -> FavoriteOfferItem | None:
        assert favorite.offer_id is not None
        row = self.offer_repo.get_customer_offer_context(favorite.offer_id)
        if row is None:
            offer = self.offer_repo.get_by_id(favorite.offer_id)
            if offer is None:
                return None
            shop = self.shop_repo.get_by_id(offer.shop_id)
            if shop is None:
                return None
            visibility = resolve_customer_visibility(offer, shop, now)
            location = self.location_repo.get_primary_by_shop(shop.id)
            category = (
                self.db.get(Category, shop.category_id) if shop.category_id else None
            )
        else:
            offer, shop, location, category = row
            visibility = resolve_customer_visibility(offer, shop, now)

        distance_km = self._distance_km(latitude, longitude, location)
        return FavoriteOfferItem(
            favorite_id=favorite.id,
            offer_id=favorite.offer_id,
            title=offer.title,
            description=offer.description,
            photo_url=resolve_customer_offer_photo_url(
                self.settings,
                offer_photo_url=offer.photo_url,
                offer_photo_storage_key=offer.photo_storage_key,
                shop_photo_url=shop.photo_url,
                shop_photo_storage_key=shop.photo_storage_key,
            ),
            discount_type=offer.discount_type,
            discount_value=offer.discount_value,
            starts_at=offer.starts_at,
            ends_at=offer.ends_at,
            status=visibility.value if visibility is not None else None,
            shop_id=shop.id,
            shop_name=shop.name,
            category=category.name if category else shop.category,
            distance_km=distance_km,
            is_active=visibility is not None,
            is_verified=customer_offer_is_verified(offer),
            source_type=customer_offer_source_type(offer),
            source_name=customer_offer_source_name(offer),
            saved_at=favorite.created_at,
        )

    def _map_shop_favorite(
        self,
        favorite: Favorite,
        latitude: float | None,
        longitude: float | None,
    ) -> FavoriteShopItem | None:
        assert favorite.shop_id is not None
        row = self.shop_repo.get_active_with_context(favorite.shop_id)
        shop: Shop | None
        location: Location | None
        category: Category | None
        is_active: bool

        if row is not None:
            shop, location, category = row
            is_active = True
        else:
            shop = self.shop_repo.get_by_id(favorite.shop_id)
            if shop is None:
                return None
            location = self.location_repo.get_primary_by_shop(shop.id)
            category = self.db.get(Category, shop.category_id) if shop.category_id else None
            is_active = shop.status == ShopStatus.ACTIVE.value

        distance_km = self._distance_km(latitude, longitude, location) if location else None
        return FavoriteShopItem(
            favorite_id=favorite.id,
            shop_id=favorite.shop_id,
            shop_name=shop.name,
            photo_url=resolve_customer_shop_photo_url(
                self.settings,
                photo_url=shop.photo_url,
                photo_storage_key=shop.photo_storage_key,
            ),
            category=category.name if category else shop.category,
            address_area=self._format_address_area(location) if location else None,
            latitude=float(location.latitude) if location and location.latitude is not None else None,
            longitude=float(location.longitude) if location and location.longitude is not None else None,
            distance_km=distance_km,
            is_active=is_active,
            saved_at=favorite.created_at,
        )

    @staticmethod
    def _require_customer(user: CurrentUser) -> None:
        if user.role != UserRole.CUSTOMER:
            raise ForbiddenError("Only customers can manage favorites")

    @staticmethod
    def _format_address_area(location: Location) -> str:
        parts = [location.city]
        if location.state:
            parts.append(location.state)
        return ", ".join(parts)

    @staticmethod
    def _distance_km(
        latitude: float | None,
        longitude: float | None,
        location: Location | None,
    ) -> float | None:
        if latitude is None or longitude is None or location is None:
            return None
        if location.latitude is None or location.longitude is None:
            return None
        return round(haversine_km(latitude, longitude, location.latitude, location.longitude), 2)
