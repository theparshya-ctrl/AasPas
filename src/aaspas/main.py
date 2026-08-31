import asyncio
from contextlib import asynccontextmanager
from datetime import UTC, datetime
from pathlib import Path

from fastapi import APIRouter, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from aaspas import __version__
from aaspas.common.error_handlers import register_exception_handlers
from aaspas.common.middleware import register_middleware
from aaspas.common.responses import ApiResponse
from aaspas.common.storage_factory import is_local_media_storage
from aaspas.config import get_settings
from aaspas.database import configure_logging
from aaspas.modules import get_domain_modules

settings = get_settings()
configure_logging()


def _public_api_docs_enabled() -> bool:
    """OpenAPI/Swagger is dev-only; hidden on staging/production Beta and prod hosts."""
    return get_settings().app_env in {"development", "testing"}


class HealthData(BaseModel):
    status: str
    version: str
    environment: str


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Ensure notification event handlers are registered at startup.
    from aaspas.modules.notification import service as _notification_service  # noqa: F401
    from aaspas.modules.notification.ws_manager import notification_ws_manager

    notification_ws_manager.set_loop(asyncio.get_running_loop())
    yield


def create_app() -> FastAPI:
    docs_enabled = _public_api_docs_enabled()
    app = FastAPI(
        title=settings.app_name,
        version=__version__,
        description="AasPas modular monolith API — local commerce & offers platform",
        docs_url="/docs" if docs_enabled else None,
        redoc_url="/redoc" if docs_enabled else None,
        openapi_url="/openapi.json" if docs_enabled else None,
        lifespan=lifespan,
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origin_list,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    register_middleware(app)
    register_exception_handlers(app)

    @app.get("/health", response_model=ApiResponse[HealthData], tags=["System"])
    def health_check() -> ApiResponse[HealthData]:
        return ApiResponse(
            data=HealthData(
                status="ok",
                version=__version__,
                environment=settings.app_env,
            ),
            meta={"checked_at": datetime.now(UTC).isoformat()},
        )

    api_v1 = APIRouter(prefix=settings.api_v1_prefix)
    for module in get_domain_modules():
        if module.enabled:
            api_v1.include_router(module.router, prefix=module.prefix, tags=[module.name.title()])

    app.include_router(api_v1)

    from aaspas.modules.notification.ws_router import router as notification_ws_router

    app.include_router(notification_ws_router, prefix=f"{settings.api_v1_prefix}/ws")

    if is_local_media_storage(get_settings()):
        current = get_settings()
        media_root = Path(current.media_root)
        media_root.mkdir(parents=True, exist_ok=True)
        app.mount(current.media_url_prefix, StaticFiles(directory=str(media_root)), name="media")

    return app


app = create_app()
