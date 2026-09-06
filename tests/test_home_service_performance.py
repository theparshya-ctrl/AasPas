"""Regression tests for HomeService nearby-query performance behavior."""

from decimal import Decimal
from unittest.mock import patch

from aaspas.modules.auth.models import User
from aaspas.modules.home.schemas import HomeQueryParams
from aaspas.modules.home.service import HomeService
from aaspas.modules.location.models import Location
from aaspas.modules.shop.models import Shop
from aaspas.modules.shop.status import ShopStatus
from tests.pilot_fixtures import FAR_LAT, FAR_LON, FIXED_NOW, NEAR_LAT, NEAR_LON, pilot_data


class TestHomeServiceNearbyQuery:
    def test_nearby_query_called_once_with_coordinates(self, db_session, pilot_data):
        service = HomeService(db_session)
        params = HomeQueryParams(latitude=NEAR_LAT, longitude=NEAR_LON, radius_km=5)

        with patch.object(service, "_query_nearby_shop_rows", wraps=service._query_nearby_shop_rows) as spy:
            response = service.get_home(params, FIXED_NOW)

        assert spy.call_count == 1
        assert response.location.available is True
        assert len(response.nearby_shops) >= 1

    def test_nearby_query_not_called_without_coordinates(self, db_session, pilot_data):
        service = HomeService(db_session)
        params = HomeQueryParams()

        with patch.object(service, "_query_nearby_shop_rows", wraps=service._query_nearby_shop_rows) as spy:
            response = service.get_home(params, FIXED_NOW)

        spy.assert_not_called()
        assert response.location.available is False
        assert response.nearby_shops == []

    def test_distance_map_and_nearby_shops_share_single_query(self, db_session, pilot_data):
        service = HomeService(db_session)
        params = HomeQueryParams(latitude=NEAR_LAT, longitude=NEAR_LON, radius_km=5)
        response = service.get_home(params, FIXED_NOW)

        assert response.nearby_shops
        nearby_shop_id = response.nearby_shops[0].shop_id
        offer_with_distance = next(
            (offer for offer in response.today_offers if offer.shop_id == nearby_shop_id),
            None,
        )
        if offer_with_distance is not None:
            assert offer_with_distance.distance_km is not None
            assert round(offer_with_distance.distance_km, 2) == response.nearby_shops[0].distance_km

    def test_nearby_shop_category_from_join(self, db_session, pilot_data):
        service = HomeService(db_session)
        params = HomeQueryParams(latitude=NEAR_LAT, longitude=NEAR_LON, radius_km=5)
        response = service.get_home(params, FIXED_NOW)

        assert response.nearby_shops
        assert response.nearby_shops[0].category == pilot_data["category"].name

    def test_far_shop_excluded_after_single_query(self, db_session, pilot_data):
        far_owner = User(email="perf-far@test.com", password_hash="hash", role="shop_owner")
        db_session.add(far_owner)
        db_session.flush()
        far_shop = Shop(
            owner_id=far_owner.id,
            name="Perf Far Shop",
            slug="perf-far-shop",
            status=ShopStatus.ACTIVE.value,
        )
        db_session.add(far_shop)
        db_session.flush()
        db_session.add(
            Location(
                shop_id=far_shop.id,
                label="Primary",
                address_line1="Far",
                city="Mumbai",
                country="IN",
                latitude=Decimal(str(FAR_LAT)),
                longitude=Decimal(str(FAR_LON)),
                is_primary=True,
            )
        )
        db_session.commit()

        service = HomeService(db_session)
        params = HomeQueryParams(latitude=NEAR_LAT, longitude=NEAR_LON, radius_km=2)

        with patch.object(service, "_query_nearby_shop_rows", wraps=service._query_nearby_shop_rows) as spy:
            response = service.get_home(params, FIXED_NOW)

        assert spy.call_count == 1
        nearby_names = {shop.shop_name for shop in response.nearby_shops}
        assert "Perf Far Shop" not in nearby_names
