from pydantic import BaseModel, Field


class AppVersionInfo(BaseModel):
    latest_version_name: str
    latest_version_code: int = Field(ge=1)
    download_url: str
    mandatory: bool = False
    release_notes: list[str] = Field(default_factory=list)
