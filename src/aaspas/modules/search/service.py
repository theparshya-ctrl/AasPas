import math
import uuid
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from aaspas.common.exceptions import NotFoundError
from aaspas.common.geo import haversine_km
from aaspas.common.storage import resolve_customer_offer_photo_url, resolve_shop_photo_url
from aaspas.config import Settings, get_settings
from aaspas.modules.favorite.repository import FavoriteRepository
from aaspas.modules.category.models import Category
from aaspas.modules.category.repository import CategoryRepository
from aaspas.modules.category.schemas import CategoryOffersResponse
from aaspas.modules.location.models import Location
from aaspas.modules.offer.customer_fields import (
    customer_offer_is_verified,
    customer_offer_source_name,
    customer_offer_source_type,
)
from aaspas.modules.offer.models import Offer
from aaspas.modules.offer.repository import OfferRepository
from aaspas.modules.offer.visibility import CustomerOfferVisibility, resolve_customer_visibility
from aaspas.modules.search.schemas import (
    CustomerSearchOfferItem,
    CustomerSearchResults,
    SearchQuery,
)
from aaspas.modules.shop.models import Shop


class SearchService:
    """Customer offer discovery — offer-first search and category browsing."""

    MODULE = "search"

    def __init__(self, db: Session, settings: Settings | None = None) -> None:
        self.db = db
        self.settings = settings or get_settings()
        self.offer_repo = OfferRepository(db)
        self.category_repo = CategoryRepository(db)
        self.repo = FavoriteRepository(db)

    def search(self, query: SearchQuery, now: datetime, *, user_id: uuid.UUID | None = None) -> CustomerSearchResults:
        rows = self.offer_repo.search_customer_offers(
            now,
            q=query.q,
            category_id=query.category_id,
            latitude=query.latitude,
            longitude=query.longitude,
            radius_km=query.radius_km or self.settings.nearby_radius_km
            if query.latitude is not None and query.longitude is not None
            else None,
        )
        items = self._map_visible_rows(
            rows,
            now,
            latitude=query.latitude,
            longitude=query.longitude,
            radius_km=query.radius_km or self.settings.nearby_radius_km
            if query.latitude is not None and query.longitude is not None
            else None,
            saved_offer_ids=self.repo.list_saved_offer_ids(user_id) if user_id else set(),
        )
        items = self._sort_items(items, query.latitude, query.longitude)
        return self._paginate(items, query.page, query.page_size)

    def category_offers(
        self,
        category_id: uuid.UUID,
        now: datetime,
        *,
        latitude: float | None = None,
        longitude: float | None = None,
        radius_km: float | None = None,
        page: int = 1,
        page_size: int = 20,
    ) -> CategoryOffersResponse:
        category = self.category_repo.get_by_id(category_id)
        if category is None or not category.is_active:
            raise NotFoundError("Category not found")

        effective_radius = (
            radius_km or self.settings.nearby_radius_km
            if latitude is not None and longitude is not None
            else None
        )
        rows = self.offer_repo.search_customer_offers(
            now,
            category_id=category_id,
            latitude=latitude,
            longitude=longitude,
            radius_km=effective_radius,
        )
        items = self._map_visible_rows(
            rows,
            now,
            latitude=latitude,
            longitude=longitude,
            radius_km=effective_radius,
        )
        items = self._sort_items(items, latitude, longitude)

        today = [item for item in items if item.status == CustomerOfferVisibility.ACTIVE.value]
        coming_soon = [
            item for item in items if item.status == CustomerOfferVisibility.COMING_SOON.value
        ]

        total_active = len(today)
        start = (page - 1) * page_size
        paged_today = today[start : start + page_size]
        paged_soon = coming_soon[:page_size]

        return CategoryOffersResponse(
            category_id=category.id,
            category_name=category.name,
            category_slug=category.slug,
            today_offers=paged_today,
            coming_soon=paged_soon,
            total_active=total_active,
            total_coming_soon=len(coming_soon),
            page=page,
            page_size=page_size,
            total_pages=max(1, math.ceil(total_active / page_size)) if total_active else 0,
        )

    def _map_visible_rows(
        self,
        rows: list[tuple[Offer, Shop, Location, Category | None]],
        now: datetime,
        *,
        latitude: float | None,
        longitude: float | None,
        radius_km: float | None,
        saved_offer_ids: set[uuid.UUID] | None = None,
    ) -> list[CustomerSearchOfferItem]:
        saved = saved_offer_ids or set()
        items: list[CustomerSearchOfferItem] = []
        for offer, shop, location, category in rows:
            visibility = resolve_customer_visibility(offer, shop, now)
            if visibility is None:
                continue

            distance_km = self._distance_km(latitude, longitude, location)
            if radius_km is not None and distance_km is not None and distance_km > radius_km:
                continue

            items.append(
                CustomerSearchOfferItem(
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
                    status=visibility.value,
                    shop_id=shop.id,
                    shop_name=shop.name,
                    category=self._category_name(shop, category),
                    distance_km=distance_km,
                    is_saved=offer.id in saved,
                    is_verified=customer_offer_is_verified(offer),
                    source_type=customer_offer_source_type(offer),
                    source_name=customer_offer_source_name(offer),
                )
            )
        return items

    @staticmethod
    def _sort_items(
        items: list[CustomerSearchOfferItem],
        latitude: float | None,
        longitude: float | None,
    ) -> list[CustomerSearchOfferItem]:
        if latitude is not None and longitude is not None:
            return sorted(items, key=lambda item: item.distance_km if item.distance_km is not None else 9999.0)
        return sorted(
            items,
            key=lambda item: item.starts_at or datetime.min.replace(tzinfo=UTC),
            reverse=True,
        )

    @staticmethod
    def _paginate(
        items: list[CustomerSearchOfferItem], page: int, page_size: int
    ) -> CustomerSearchResults:
        total = len(items)
        start = (page - 1) * page_size
        page_items = items[start : start + page_size]
        total_pages = math.ceil(total / page_size) if total else 0
        return CustomerSearchResults(
            offers=page_items,
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages,
        )

    @staticmethod
    def _category_name(shop: Shop, category: Category | None) -> str | None:
        if category is not None:
            return category.name
        return shop.category

    @staticmethod
    def _distance_km(
        latitude: float | None,
        longitude: float | None,
        location: Location,
    ) -> float | None:
        if latitude is None or longitude is None:
            return None
        if location.latitude is None or location.longitude is None:
            return None
        return round(haversine_km(latitude, longitude, location.latitude, location.longitude), 2)
