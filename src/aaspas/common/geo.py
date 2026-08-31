"""Geo utilities — haversine distance without PostGIS."""

import math
from decimal import Decimal


def haversine_km(
    lat1: float | Decimal,
    lon1: float | Decimal,
    lat2: float | Decimal,
    lon2: float | Decimal,
) -> float:
    """Great-circle distance in kilometers between two WGS84 coordinates."""
    r = 6371.0
    phi1, phi2 = math.radians(float(lat1)), math.radians(float(lat2))
    d_phi = math.radians(float(lat2) - float(lat1))
    d_lambda = math.radians(float(lon2) - float(lon1))
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return r * 2 * math.asin(math.sqrt(min(a, 1.0)))


def bounding_box(lat: float, lon: float, radius_km: float) -> tuple[float, float, float, float]:
    """Return min_lat, max_lat, min_lon, max_lon for a rough pre-filter."""
    lat_delta = radius_km / 111.0
    lon_delta = radius_km / (111.0 * max(math.cos(math.radians(lat)), 0.01))
    return lat - lat_delta, lat + lat_delta, lon - lon_delta, lon + lon_delta
