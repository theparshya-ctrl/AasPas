from functools import lru_cache

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Central application configuration. Override via environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "AasPas"
    app_env: str = Field(default="development", pattern="^(development|testing|staging|production)$")
    app_debug: bool = False
    api_v1_prefix: str = "/api/v1"

    host: str = "0.0.0.0"
    port: int = Field(default=8000, validation_alias=AliasChoices("PORT", "port"))

    database_url: str = "postgresql://aaspas:aaspas@localhost:5432/aaspas"

    secret_key: str = "change-me-in-production"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30

    cors_origins: str = "http://localhost:3000"
    rate_limit_per_minute: int = 120
    log_level: str = "INFO"

    nearby_radius_km: float = Field(default=10.0, gt=0, le=100)
    home_offers_limit: int = Field(default=20, ge=1, le=100)
    home_nearby_shops_limit: int = Field(default=20, ge=1, le=100)

    media_root: str = "uploads"
    media_url_prefix: str = "/media"
    media_public_base_url: str | None = None
    media_storage_backend: str = Field(default="local", pattern="^(local|s3)$")
    s3_endpoint_url: str | None = Field(default=None, validation_alias=AliasChoices("S3_ENDPOINT_URL", "AWS_ENDPOINT_URL_S3"))
    s3_region: str = Field(default="us-east-2", validation_alias=AliasChoices("S3_REGION", "AWS_REGION"))
    s3_access_key_id: str | None = Field(default=None, validation_alias=AliasChoices("S3_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID"))
    s3_secret_access_key: str | None = Field(default=None, validation_alias=AliasChoices("S3_SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY"))
    s3_bucket_name: str | None = Field(default=None, validation_alias=AliasChoices("S3_BUCKET_NAME", "AWS_S3_BUCKET"))
    shop_photo_max_bytes: int = Field(default=5 * 1024 * 1024, ge=1024, le=20 * 1024 * 1024)

    @property
    def cors_origin_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]

    @property
    def is_production(self) -> bool:
        return self.app_env == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()
