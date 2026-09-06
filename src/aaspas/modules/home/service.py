import uuid
from datetime import datetime

from sqlalchemy import and_
from sqlalchemy.orm import Session

from aaspas.common.geo import bounding_box, haversine_km
from aaspas.common.storage import resolve_customer_offer_photo_url, resolve_customer_shop_photo_url
from aaspas.config import Settings, get_settings
from aaspas.modules.category.models import Category
from aaspas.modules.category.repository import CategoryRepository
from aaspas.modules.home.schemas import (
    HomeCategoryItem,
    HomeLocationInfo,
    HomeOfferItem,
    HomeQueryParams,
    HomeResponse,
    HomeShopItem,
)
from aaspas.modules.location.models import Location
from aaspas.modules.offer.customer_fields import (
    customer_offer_is_verified,
    customer_offer_source_name,
    customer_offer_source_type,
)
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.visibility import resolve_customer_visibility
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus


class HomeService:
    MODULE = "home"

    def __init__(self, db: Session, settings: Settings | None = None) -> None:
        self.db = db
        self.settings = settings or get_settings()
        self.offer_repo = OfferRepository(db)
        self.category_repo = CategoryRepository(db)

    def get_home(
        self,
        params: HomeQueryParams,
        now: datetime,
        *,
        saved_offer_ids: set[uuid.UUID] | None = None,
        saved_shop_ids: set[uuid.UUID] | None = None,
    ) -> HomeResponse:
        saved_offers = saved_offer_ids or set()
        saved_shops = saved_shop_ids or set()
        categories = self._load_categories()
        today_rows = self.offer_repo.list_customer_offers(now, coming_soon=False, limit=self.settings.home_offers_limit)
        soon_rows = self.offer_repo.list_customer_offers(now, coming_soon=True, limit=self.settings.home_offers_limit)

        has_coords = params.latitude is not None and params.longitude is not None
        location = HomeLocationInfo(
            latitude=params.latitude,
            longitude=params.longitude,
            available=has_coords,
        )

        nearby_rows: list[tuple[Shop, Location, Category | None, float]] | None = None
        if has_coords:
            assert params.latitude is not None and params.longitude is not None
            radius = params.radius_km or self.settings.nearby_radius_km
            nearby_rows = self._query_nearby_shop_rows(params.latitude, params.longitude, radius)

        distance_map = (
            {shop.id: dist for shop, _loc, _cat, dist in nearby_rows}
            if nearby_rows is not None
            else {}
        )

        return HomeResponse(
            location=location,
            categories=categories,
            today_offers=self._map_offers(today_rows, now, distance_map, saved_offers),
            coming_soon=self._map_offers(soon_rows, now, distance_map, saved_offers),
            nearby_shops=self._map_nearby_shops(nearby_rows, saved_shops) if nearby_rows is not None else [],
        )

    def _load_categories(self) -> list[HomeCategoryItem]:
        return [
            HomeCategoryItem(
                id=c.id,
                name=c.name,
                slug=c.slug,
                display_order=c.display_order,
            )
            for c in self.category_repo.list_active()
        ]

    def _map_offers(
        self,
        rows: list[tuple[Offer, Shop, Location, Category | None]],
        now: datetime,
        distance_map: dict[uuid.UUID, float],
        saved_offer_ids: set[uuid.UUID],
    ) -> list[HomeOfferItem]:
        items: list[HomeOfferItem] = []
        for offer, shop, _location, category in rows:
            visibility = resolve_customer_visibility(offer, shop, now)
            if visibility is None:
                continue
            items.append(
                HomeOfferItem(
                    offer_id=offer.id,
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
                    shop_id=shop.id,
                    shop_name=shop.name,
                    distance_km=distance_map.get(shop.id),
                    category=self._resolve_category_name(shop, category),
                    is_saved=offer.id in saved_offer_ids,
                    is_verified=customer_offer_is_verified(offer),
                    source_type=customer_offer_source_type(offer),
                    source_name=customer_offer_source_name(offer),
                )
            )
        return items

    def _map_nearby_shops(
        self,
        rows: list[tuple[Shop, Location, Category | None, float]],
        saved_shop_ids: set[uuid.UUID],
    ) -> list[HomeShopItem]:
        limit = self.settings.home_nearby_shops_limit
        items: list[HomeShopItem] = []
        for shop, location, category, dist in rows[:limit]:
            items.append(
                HomeShopItem(
                    shop_id=shop.id,
                    shop_name=shop.name,
                    photo_url=resolve_customer_shop_photo_url(
                        self.settings,
                        photo_url=shop.photo_url,
                        photo_storage_key=shop.photo_storage_key,
                    ),
                    category=self._resolve_category_name(shop, category),
                    address_area=self._format_address_area(location),
                    distance_km=round(dist, 2),
                    latitude=float(location.latitude),
                    longitude=float(location.longitude),
                    is_saved=shop.id in saved_shop_ids,
                )
            )
        return items

    def _resolve_category_name(self, shop: Shop, category: Category | None) -> str | None:
        if category is not None:
            return category.name
        return shop.category

    def _query_nearby_shop_rows(
        self, latitude: float, longitude: float, radius_km: float
    ) -> list[tuple[Shop, Location, Category | None, float]]:
        min_lat, max_lat, min_lon, max_lon = bounding_box(latitude, longitude, radius_km)
        rows = (
            self.db.query(Shop, Location, Category)
            .join(
                Location,
                and_(Location.shop_id == Shop.id, Location.is_primary.is_(True)),
            )
            .outerjoin(Category, Shop.category_id == Category.id)
            .filter(Shop.status == ShopStatus.ACTIVE.value)
            .filter(Location.latitude.isnot(None))
            .filter(Location.longitude.isnot(None))
            .filter(Location.latitude >= min_lat)
            .filter(Location.latitude <= max_lat)
            .filter(Location.longitude >= min_lon)
            .filter(Location.longitude <= max_lon)
            .all()
        )

        scored: list[tuple[Shop, Location, Category | None, float]] = []
        for shop, location, category in rows:
            dist = haversine_km(latitude, longitude, location.latitude, location.longitude)
            if dist <= radius_km:
                scored.append((shop, location, category, dist))

        scored.sort(key=lambda row: row[3])
        return scored

    @staticmethod
    def _format_address_area(location: Location) -> str:
        parts = [location.city]
        if location.state:
            parts.append(location.state)
        return ", ".join(parts)
