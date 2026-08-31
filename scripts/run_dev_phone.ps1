# Start AasPas API for physical phone testing (SQLite dev DB, no PostgreSQL).
# Phone reaches this laptop over Tailscale (not same Wi-Fi). Set API_BASE_URL
# in android/local.properties to http://<tailscale-ipv4>:8000/

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

$env:DATABASE_URL = "sqlite:///H:/AasPas/dev.db"
$env:PYTHONPATH = "src"
$env:SECRET_KEY = "dev-secret-phone-test"
$env:APP_ENV = "development"

if (-not (Test-Path "dev.db")) {
    Write-Host "Creating dev.db and seeding pilot data..."
    & .\.venv\Scripts\python.exe -c @"
from aaspas.common.audit import AuditLog
from aaspas.modules.auth.models import User
from aaspas.modules.category.models import Category
from aaspas.modules.customer.models import CustomerProfile
from aaspas.modules.favorite.models import Favorite
from aaspas.modules.location.models import Location
from aaspas.modules.notification.models import Notification
from aaspas.modules.offer.models import Offer
from aaspas.modules.shop.models import Shop
from aaspas.database import Base, engine
Base.metadata.create_all(bind=engine)
"@
    & .\.venv\Scripts\python.exe scripts/seed_pilot.py
} else {
    Write-Host "Syncing dev.db schema if needed..."
    & .\.venv\Scripts\python.exe scripts/sync_dev_db_schema.py
}

Write-Host "Starting API on http://0.0.0.0:8000 (Tailscale: check android/local.properties API_BASE_URL)"
& .\.venv\Scripts\python.exe -m uvicorn aaspas.main:app --host 0.0.0.0 --port 8000 --app-dir src
