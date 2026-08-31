# AasPas Beta — Render Free + Neon PostgreSQL + Neon Object Storage

## Architecture

| Component | DEV (unchanged) | Beta (Render + Neon) |
|-----------|-----------------|----------------------|
| API host | Laptop / Tailscale | Render Free web service |
| Database | `dev.db` / local Postgres | **Neon PostgreSQL** (separate project) |
| Media | `LocalFileStorage` → `uploads/` | **S3 backend** → Neon Object Storage |
| Media URLs | `/media/...` via FastAPI | **Absolute HTTPS** Neon public_read URLs |
| APK | AasPas DEV | AasPas BETA |

**Never share** database, storage, or secrets between DEV and Beta.

## Provider verification (CURSOR-041D — 2026-08-27)

Verify again in provider UI before provisioning; terms change.

| Provider | Free / $0 | Credit card at signup | Notes |
|----------|-----------|------------------------|-------|
| **Render Free web service** | Yes (750 instance-hours/mo) | Not stated as required on [Render Free docs](https://render.com/docs/free); overage may **suspend** services if no payment method | Ephemeral disk; 15-min idle spin-down; use Neon S3 for media |
| **Neon Free PostgreSQL** | Yes ($0/month) | **No credit card required** ([Neon pricing](https://neon.tech/pricing)) | 0.5 GB/project; 100 CU-hours/mo; us-east-2 for Object Storage |
| **Neon Object Storage (Beta)** | Free during Beta (limits apply) | Requires **claimed** Neon project (not available via Claimable Neon API) | **us-east-2 only**; 5 GB free tier; S3-compatible |

**Payment method required to proceed:** None verified for Neon Free signup. Render may request a card for overages but Free tier can run without one (services suspend instead of billing).

**Agent provisioning blockers:** No `NEON_API_KEY` or `RENDER_API_KEY` in environment; no `neon`/`render` CLI installed; Object Storage requires human-owned Neon project.

## Neon Object Storage (documented API)

Neon Object Storage is **S3-compatible** and supports:

| Operation | Supported | AasPas usage |
|-----------|-----------|--------------|
| `PutObject` | Yes | Shop/Offer photo upload |
| `GetObject` | Yes | Public read via `public_read` bucket |
| `DeleteObject` | Yes | Photo replace/delete |
| `public_read` bucket | Yes | Anonymous HTTPS image URLs for Android |

Public object URL format (from Neon docs):

```
https://<branch-id>.storage.c-<N>.us-east-2.aws.neon.tech/<bucket>/<object-key>
```

Set `MEDIA_PUBLIC_BASE_URL` to the bucket base (without object key).

Implementation uses **boto3** with `AWS_ENDPOINT_URL_S3`, path-style addressing, and `MEDIA_STORAGE_BACKEND=s3`.

## Render Free limitations vs AasPas

| Requirement | Render Free behavior | Impact |
|-------------|---------------------|--------|
| Ephemeral filesystem | Local disk not persistent | **Mitigated** — media on Neon S3, not Render disk |
| 15-min idle spin-down | Spins down without HTTP/WS traffic | ~1 min cold start; WS messages count as activity (2026-02+) |
| WebSocket | Supported on free tier | `/api/v1/ws/notifications` unchanged |
| Single instance | One free web service | Matches in-process WS + rate limit |
| Bandwidth/hours | Monthly free instance hours cap | Monitor for 5–20 testers |

## Environment variables (Render)

| Variable | Required | Notes |
|----------|----------|-------|
| `DATABASE_URL` | Yes | Neon PostgreSQL connection string |
| `SECRET_KEY` | Yes | Unique Beta secret |
| `APP_ENV` | Yes | `staging` |
| `MEDIA_STORAGE_BACKEND` | Yes | `s3` |
| `AWS_ENDPOINT_URL_S3` | Yes | Neon branch storage endpoint |
| `AWS_REGION` | Yes | `us-east-2` |
| `AWS_ACCESS_KEY_ID` | Yes | Neon credential `token_id` |
| `AWS_SECRET_ACCESS_KEY` | Yes | Neon `s3_secret_access_key` |
| `AWS_S3_BUCKET` | Yes | `public_read` bucket name |
| `MEDIA_PUBLIC_BASE_URL` | Yes | Neon public bucket HTTPS base |
| `BETA_ADMIN_*` | For seed | Admin bootstrap only |
| `PORT` | Auto | Set by Render |

## Deploy steps (manual — after billing/signup approval)

1. Create **Neon project** (us-east-2) + PostgreSQL database for Beta.
2. Enable **Object Storage** on Beta branch; create `public_read` bucket.
3. Create storage credential; save `AWS_*` values (shown once).
4. Create **Render** web service (Docker, free plan) from repo.
5. Set env vars from `.env.beta.example`.
6. Use start command: `sh /app/deploy/beta/render-start.sh` (runs Alembic + uvicorn on `$PORT`).
7. Verify `GET https://<render-host>/health`.
8. Seed admin: run `seed_beta_admin.py` with Beta `DATABASE_URL` + `BETA_ADMIN_*`.
9. Update `android/beta.properties` → build BETA APK.

Blueprint reference: `deploy/beta/render.yaml` (`autoDeploy: false`).

## Android media URLs

With S3 backend, API returns **absolute HTTPS** Neon URLs. `MediaUrlResolver` passes through non-`/media/` URLs unchanged — no API proxy required.

## Disable / reset Beta

- **Disable:** Suspend Render service; rotate Neon credentials.
- **Reset DB:** Drop/recreate Neon branch or database.
- **Reset media:** Delete objects from Neon bucket.

## DEV unchanged

Local DEV continues using:

```
MEDIA_STORAGE_BACKEND=local   (default)
MEDIA_ROOT=uploads
```

No Render or Neon credentials required for DEV.
