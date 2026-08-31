# AasPas Architecture

## Overview

AasPas is built as a **Modular Monolith** — one deployable application with strict domain boundaries. This gives operational simplicity today while keeping the door open to extract high-traffic modules into independent services later.

```
┌─────────────────────────────────────────────────────────────┐
│                     AasPas API (FastAPI)                     │
├──────────┬──────────┬──────────┬──────────┬─────────────────┤
│   Auth   │ Customer │   Shop   │  Offer   │   Location      │
├──────────┼──────────┼──────────┼──────────┼─────────────────┤
│  Search  │ Notif.   │ Analytics│  Admin   │ Future modules  │
├──────────┴──────────┴──────────┴──────────┴─────────────────┤
│  Common: config, security, audit, events, error handling      │
├─────────────────────────────────────────────────────────────┤
│                    PostgreSQL (primary DB)                   │
└─────────────────────────────────────────────────────────────┘
```

## Module Boundaries

Each module lives under `src/aaspas/modules/<name>/` with:

| Layer | File | Responsibility |
|-------|------|----------------|
| API | `router.py` | HTTP endpoints, input validation |
| Service | `service.py` | Business logic, orchestration |
| Repository | `repository.py` | Data access only |
| Models | `models.py` | SQLAlchemy entities (module-owned tables) |
| Schemas | `schemas.py` | Pydantic request/response DTOs |
| Interface | `interfaces.py` | Cross-module contracts (where needed) |

### Rules

1. **No cross-module direct DB access** — use repository interfaces or domain events.
2. **Each module owns its tables** — foreign keys reference other modules' IDs, not their internals.
3. **Business rules live in services** — not in routers or hard-coded in multiple places.
4. **Future extraction** — replace `InProcessEventBus` or repository interfaces with HTTP/message-broker adapters.

## API Versioning

- Current version: `/api/v1`
- Breaking changes require a new version prefix (`/api/v2`)
- Non-breaking additions stay in the current version

## Security Model

- JWT bearer authentication
- Role-based access control (RBAC) with permission strings
- Shop owners scoped to their own `shop_id`
- Admin roles separated from customer/merchant roles
- Rate limiting on API endpoints

## Data Integrity

- PostgreSQL with foreign keys, indexes, and constraints
- Alembic migrations with reversible `downgrade()` where practical
- Audit log for important actions (`audit_logs` table)

## Observability

- Structured JSON logging via `structlog`
- Request logging middleware (method, path, status, duration)
- Audit trail for security-sensitive operations
- Health endpoint: `GET /health`

## Deployment Pipeline

```
Development → Testing → Staging → Production
```

Each environment uses separate config via environment variables (see `.env.example`).

## Evolution Roadmap

| Phase | Description |
|-------|-------------|
| **Phase 1** | Modular Monolith (current) |
| **Phase 2** | Scale individual modules (read replicas, caching, background workers) |
| **Phase 3** | Extract high-load modules (Search, Notifications, Analytics) into services |

## Adding a New Module

1. Create `src/aaspas/modules/<name>/` with router, service, repository, models, schemas
2. Register in `src/aaspas/modules/__init__.py`
3. Add Alembic migration for new tables
4. Add tests under `tests/`
5. Document API in OpenAPI (auto-generated from FastAPI)

## Customer Home API

`GET /api/v1/home` — public, no auth required.

Optional query params: `latitude`, `longitude`, `radius_km`.

### Offer customer visibility

Persisted merchant statuses: `draft`, `pending_approval`, `scheduled`, `active`, `expired`.

Customer-visible states are derived at query time from persisted status, shop `active` status, and `starts_at` / `ends_at`:

| Condition | Customer section |
|-----------|------------------|
| Approved + `starts_at > now` + valid end | Coming Soon |
| Approved + `starts_at <= now < ends_at` | Today's Offers |
| Draft, pending, expired, or past `ends_at` | Hidden |

Location is optional. Without coordinates, `nearby_shops` is empty but the response succeeds.

## Running Locally

```bash
# 1. Create virtual environment
python -m venv .venv
.venv\Scripts\activate        # Windows
pip install -r requirements-dev.txt

# 2. Start PostgreSQL
docker compose up postgres -d

# 3. Copy env and run migrations
copy .env.example .env
alembic upgrade head

# 4. Start API
uvicorn aaspas.main:app --reload --app-dir src
```

API docs: http://localhost:8000/docs
