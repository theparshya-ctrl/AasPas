from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from aaspas.common.responses import ApiResponse
from aaspas.common.security.auth import CurrentUser, require_permission
from aaspas.common.security.rbac import Permission
from aaspas.database import get_db
from aaspas.modules.analytics.schemas import PlatformMetrics
from aaspas.modules.analytics.service import AnalyticsService

router = APIRouter(tags=["Analytics"])


@router.get("/metrics", response_model=ApiResponse[PlatformMetrics])
def platform_metrics(
    _user: Annotated[CurrentUser, Depends(require_permission(Permission.ADMIN_VIEW_ANALYTICS))],
    db: Annotated[Session, Depends(get_db)],
) -> ApiResponse[PlatformMetrics]:
    metrics = AnalyticsService(db).get_platform_metrics()
    return ApiResponse(data=metrics)
