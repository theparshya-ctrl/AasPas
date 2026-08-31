from sqlalchemy.orm import Session

from aaspas.modules.analytics.schemas import PlatformMetrics
from aaspas.modules.auth.models import User
from aaspas.modules.offer.models import Offer
from aaspas.modules.shop.models import Shop


class AnalyticsService:
    MODULE = "analytics"

    def __init__(self, db: Session) -> None:
        self.db = db

    def get_platform_metrics(self) -> PlatformMetrics:
        return PlatformMetrics(
            total_users=self.db.query(User).count(),
            total_shops=self.db.query(Shop).count(),
            total_offers=self.db.query(Offer).count(),
            active_offers=self.db.query(Offer).filter(Offer.status == "active").count(),
        )
