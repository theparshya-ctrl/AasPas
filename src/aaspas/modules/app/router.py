from fastapi import APIRouter

from aaspas.common.responses import ApiResponse
from aaspas.modules.app.schemas import AppVersionInfo
from aaspas.modules.app.service import get_beta_app_version

router = APIRouter(tags=["App"])


@router.get("/version", response_model=ApiResponse[AppVersionInfo])
def get_app_version() -> ApiResponse[AppVersionInfo]:
    """Public Beta APK version metadata for in-app update checks."""
    return ApiResponse(data=get_beta_app_version())
