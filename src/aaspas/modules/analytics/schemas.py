from pydantic import BaseModel


class PlatformMetrics(BaseModel):
    total_users: int
    total_shops: int
    total_offers: int
    active_offers: int
