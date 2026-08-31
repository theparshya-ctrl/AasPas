# AasPas

Production-ready modular monolith platform for local commerce, shops, and offers.

## Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL 16 (or Docker)
- Git

### Setup

```bash
# Clone and enter project
cd AasPas

# Virtual environment
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # macOS/Linux

# Install dependencies
pip install -r requirements-dev.txt

# Environment
copy .env.example .env          # Windows
# cp .env.example .env          # macOS/Linux

# Start database
docker compose up postgres -d

# Run migrations
alembic upgrade head

# Start API server
uvicorn aaspas.main:app --reload --app-dir src
```

### Verify

- Health: http://localhost:8000/health
- API docs: http://localhost:8000/docs
- Run tests: `pytest`

### One-click backend (Windows)

You do not need Cursor open to run the development API.

1. Double-click `Start-AasPas-Backend.bat`
2. Wait until the window shows `AasPas Backend: RUNNING`
3. Optional: create a Desktop shortcut named **AasPas Backend** with target `H:\AasPas\Start-AasPas-Backend.bat` and start-in `H:\AasPas`

Stop with `Stop-AasPas-Backend.bat`. Restart with `Restart-AasPas-Backend.bat`.

The launcher uses `.venv\Scripts\python.exe` (no `activate`), checks Tailscale, and health-checks `http://127.0.0.1:8000/health`. It does not change Android `local.properties` or create firewall rules. Do not port-forward TCP 8000 on the router.

Runtime: `logs\aaspas-backend.pid` only. Delete a stale PID file if the process was killed outside the launcher. Terminal output is the log; no rotation is needed unless you later add log files under `logs\`.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full modular monolith design, module boundaries, security model, and evolution roadmap.

## Modules (Phase 1)

| Module | Prefix | Status |
|--------|--------|--------|
| Auth | `/api/v1/auth` | Active |
| Customer | `/api/v1/customers` | Active |
| Shop | `/api/v1/shops` | Active |
| Category | `/api/v1/categories` | Active |
| Home | `/api/v1/home` | Active |
| Location | `/api/v1/locations` | Active |
| Search | `/api/v1/search` | Active |
| Notification | `/api/v1/notifications` | Active |
| Analytics | `/api/v1/analytics` | Active |
| Admin | `/api/v1/admin` | Active |
| Subscription | `/api/v1/subscriptions` | Future |
| Loyalty | `/api/v1/loyalty` | Future |

## Project Structure

```
AasPas/
├── src/aaspas/           # Application source
│   ├── common/           # Shared infrastructure
│   ├── modules/          # Domain modules
│   ├── config.py
│   ├── database.py
│   └── main.py
├── alembic/              # Database migrations
├── tests/                # Unit & API tests
├── docs/                 # Architecture docs
├── docker-compose.yml
└── Dockerfile
```

## License

Proprietary — AasPas Platform
