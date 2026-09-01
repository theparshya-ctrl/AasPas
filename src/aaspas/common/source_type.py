"""Offer/shop data source classification."""

from enum import StrEnum


class SourceType(StrEnum):
    AASPAS = "AASPAS"
    EXTERNAL = "EXTERNAL"
