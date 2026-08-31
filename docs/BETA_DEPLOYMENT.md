# AasPas External Beta Deployment (CURSOR-041B)

## Architecture

| | Local DEV | External Beta |
|--|-----------|---------------|
| Backend | Laptop launcher, port 8000 | Cloud VM (Fly.io) or local validation port 8001 |
| Database | `dev.db` / local Postgres | **PostgreSQL** (Neon or Beta Docker Postgres) |
| Media | `uploads/` | `/data/uploads` |
| APK | AasPas DEV | AasPas BETA |
| API | Tailscale / LAN HTTP | HTTPS only |

**Never share** database, uploads, or `SECRET_KEY` between DEV and Beta.

## Free / cost check

**Verify current pricing** on provider sites before provisioning (Fly.io, Oracle, Neon). Figures below are approximate from CURSOR-041B and may have changed.

| Provider | ₹0 usage? | Credit card? | Notes |
|----------|-----------|--------------|-------|
| **Fly.io** | Pay-as-you-go only | **Yes, required** | Smallest VM + volume — confirm current rates on fly.io |
| **Oracle Always Free** | Yes (within limits) | **Yes, verification** | ARM VM; confirm current Always Free limits |
| **Neon PostgreSQL** | Free tier available | Often yes | Separate Beta DB — confirm neon.tech current limits |
| **Render free** | Sleeps when idle | Often yes | Bad for WebSocket beta |

**STOP before creating cloud resources** if credit card / billing is not approved.

## Deploy to Fly.io (recommended path)

1. Install [flyctl](https://fly.io/docs/hands-on/install-flyctl/) and `fly auth login`.
2. Copy `.env.beta.example` → `.env.beta`; set a unique `SECRET_KEY`.
3. Create app and volume:
   ```bash
   fly apps create aaspas-beta
   fly volumes create aaspas_beta_data --region bom --size 1 -a aaspas-beta
   ```
4. Set secrets (example):
   ```bash
   fly secrets set -a aaspas-beta \
     SECRET_KEY="long-random-beta-secret" \
     APP_ENV=staging \
     DATABASE_URL="postgresql://..." \
     MEDIA_ROOT="/data/uploads" \
     MEDIA_PUBLIC_BASE_URL="https://aaspas-beta.fly.dev" \
     BETA_ADMIN_EMAIL="you@example.com" \
     BETA_ADMIN_PASSWORD="your-strong-password"
   ```
5. Deploy:
   ```bash
   fly deploy --config deploy/beta/fly.toml
   ```
6. Seed admin (once, after deploy succeeds):
   ```bash
   fly ssh console -a aaspas-beta -C "PYTHONPATH=/app/src python /app/scripts/seed_beta_admin.py"
   ```
   Requires `BETA_ADMIN_EMAIL` and `BETA_ADMIN_PASSWORD` in Fly secrets (step 4).

7. Verify externally:
   ```bash
   curl https://aaspas-beta.fly.dev/health
   ```

### Disable Beta

```bash
fly apps suspend aaspas-beta
```

### Reset Beta data

```bash
fly volumes destroy aaspas_beta_data -a aaspas-beta
fly volumes create aaspas_beta_data --region bom --size 1 -a aaspas-beta
fly deploy --config deploy/beta/fly.toml
# Re-run migrations + seed admin
```

## Local Beta validation (isolated from DEV)

Does **not** replace cloud Beta; validates config on port **8001**:

```powershell
powershell -File scripts\run_beta_local.ps1 -Action start
powershell -File scripts\run_beta_local.ps1 -Action health
powershell -File scripts\run_beta_local.ps1 -Action seed-admin
powershell -File scripts\run_beta_local.ps1 -Action stop
```

Uses `beta_data/uploads/` for local media only and PostgreSQL from `.env.beta` (port 5433 via Docker Beta stack). Never touches `dev.db` or `uploads/`.

## Docker (optional)

```bash
docker compose -f docker-compose.beta.yml up --build
```

Requires `.env.beta` file.

## Android BETA APK

1. Deploy Beta backend and note HTTPS URL.
2. Copy `android/beta.properties.example` → `android/beta.properties`.
3. Set `BETA_API_BASE_URL=https://your-beta-host/`
4. Run `Build-AasPas-BETA-APK.bat`

### applicationId

Both DEV and BETA use `com.aaspas.customer`. Installing BETA **replaces** DEV on the same phone (same versionCode allows in-place update).

## Security (staging)

- JWT + RBAC unchanged
- Admin registration blocked in API (`customer` / `shop_owner` only)
- `/docs` and `/openapi.json` **hidden** when `APP_ENV=staging`
- Single Beta instance (in-memory rate limit + in-process WebSocket)
- Upload validation unchanged (JPEG/PNG/WebP, 5MB)

## WebSocket

Beta URL: `wss://<beta-host>/api/v1/ws/notifications?token=...`

Android `RealtimeNotificationClient` derives WSS from `BuildConfig.API_BASE_URL` automatically.

## Single-instance limitation

Beta runs **one** backend process. Do not scale to multiple machines until WebSocket manager and rate limiting are externalized (Redis, etc.).
