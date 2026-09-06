# External Offer Collection Workflow

Phase 1 provides the import system. Real public offers are collected manually and imported into **Beta Neon PostgreSQL** — never into DEV `dev.db`.

CURSOR-063 adds a **repeatable refresh workflow** with structured audit reports. There is **no automatic web scraper** and **no paid scheduler** — collection remains manual; execution is manual and automation-ready.

## Target areas

- Pimpri
- Chinchwad
- Hadapsar
- Manjri
- Manjri Budruk
- Saswad
- Nearby relevant areas

## Repeatable workflow

### 1. Collect (manual)

1. Visit **public** sources only (shop websites, public social posts, flyers with verifiable URLs).
2. Copy **exact offer wording** — do not paraphrase discounts.
3. Record business-identifying address (street/venue + city). Never fabricate GPS coordinates.
4. Assign a stable `external_source_key` per offer (deduplication key).
5. Set `collected_at` to the UTC timestamp when the offer was collected.
6. Use `ends_at` only when the public source shows a fixed end date; leave **null** for ongoing/current offers.
7. Save records in a JSON file using `data/external_offers/template.json`.

### 2. Validate (dry run)

```powershell
cd H:\AasPas
$env:PYTHONPATH = "src"
python scripts/import_external_offers.py path\to\collected_offers.json --dry-run
```

### 3. Import or refresh

**Standard import** (new/updated offers only):

```powershell
python scripts/import_external_offers.py path\to\collected_offers.json --report reports\external_import.json
```

**Refresh run** (import + stale-candidate report; **does not auto-delete** missing offers):

```powershell
python scripts/import_external_offers.py path\to\collected_offers.json --refresh --report reports\external_refresh.json
```

Optional: `--allow-needs-review` for area-only addresses flagged NEEDS_REVIEW.

Environment: load `.env.beta` automatically (or set `AASPAS_ENV_FILE`). Requires Neon `DATABASE_URL` and `APP_ENV=staging`.

### 4. Verify

1. Review console summary and JSON report.
2. Check customer Home/Search for target area.
3. Review `stale_candidates` in refresh reports — investigate offers missing from the latest file before deactivating.

### 5. Audit / report

Each run produces:

| Metric | Meaning |
|--------|---------|
| `created` | New external offer |
| `updated` | Existing key with changed fields |
| `unchanged` | Existing key, identical payload (idempotent re-import) |
| `rejected` | Failed validation policy |
| `duplicate_in_file` | Same `external_source_key` twice in one JSON file |
| `stale_candidates` | Active external offers in Beta **not** present in this refresh file (report only) |

An `audit_logs` row is written per run (`module=external`, `resource_type=external_offer_import`).

Use `--report path.json` for machine-readable output. Reports must not be committed if they contain production data paths.

## Refresh behavior

| Situation | Behavior |
|-----------|----------|
| **New offer** | Inserted as EXTERNAL / `is_verified=false` |
| **Existing unchanged** | Counted as `unchanged`; no DB write |
| **Existing with updated source info** | Updated in place (title, source_url, collected_at, etc.) |
| **Duplicate in same JSON file** | Skipped with `duplicate_in_file` |
| **Missing from refresh file** | Listed as `stale_candidate` — **not** auto-expired or deleted |
| **Explicitly expired on source** | Re-import with `ends_at` in the past → rejected at import |

Never invent expiry dates. Never auto-delete because an offer was absent from one collection run.

## Expiry handling

- Ongoing offers: `ends_at = null` — remain active until explicit evidence of end.
- Fixed-end offers: set `ends_at` from the public source.
- To deactivate: re-import with past `ends_at` **only when the source explicitly ended**, or follow manual review for stale candidates across multiple refresh runs.

## Validation policy

Rejected automatically:

- Vague wording ("great deals", "visit us")
- Missing / invalid `source_name`, `source_url`, `collected_at`
- Missing identifiable discount
- Explicitly expired `ends_at`
- Invalid `external_source_key`

NEEDS_REVIEW (skipped unless `--allow-needs-review`):

- Area-only address without street/venue detail

Always enforced on import:

- `source_type=EXTERNAL`
- `is_verified=false`
- System owner: `external-data@aaspas.internal`
- No notification events

## Collector field guide

Use this when collecting from public sources. The importer sets `source_type=EXTERNAL` and `is_verified=false` automatically — **do not** add those fields to JSON.

| What you collect | JSON field | Required |
|------------------|------------|----------|
| Shop name | `shop_name` | Yes |
| Category | `category` | Yes — must match an active AasPas category |
| Street address | `address.address_line1` | Yes |
| Area / city | `address.city` | Yes |
| State | `address.state` | Recommended |
| Pincode | `address.postal_code` | Recommended |
| Latitude | `address.latitude` | When verified from public source only |
| Longitude | `address.longitude` | When verified from public source only |
| Offer title | `title` | Yes — preserve exact public wording |
| Offer description | `description` | Recommended |
| Offer type | `discount_type` | Yes |
| Discount value | `discount_value` | Yes |
| Start date/time | `starts_at` | Optional |
| Expiry date/time | `ends_at` | Optional — omit when no fixed expiry |
| Source label | `source_name` | Yes |
| Source URL | `source_url` | Yes — public http(s) link |
| Collected date/time | `collected_at` | Yes — ISO 8601 UTC |
| Unique dedup key | `external_source_key` | Yes — min 8 chars, stable per offer |
| Stable shop key | `shop_external_source_key` | Optional |

## Collection rules

1. **Public sources only** — no login, paywall, or CAPTCHA bypass.
2. **No fabricated offers, discounts, or coordinates.**
3. **No expired offers** unless documenting explicit end on source.
4. **Exact local relevance** — Pune-area targets above.
5. **Customer disclosure** — always "Not confirmed by AasPas".
6. **Images** — do not rehost without permission.

## JSON structure

Start from `data/external_offers/template.json` (empty `offers` array).

```json
{
  "offers": [
    {
      "external_source_key": "example-source-001",
      "shop_name": "Example Shop Name",
      "category": "Clothing / Fashion",
      "address": {
        "address_line1": "Example address line",
        "city": "Pimpri-Chinchwad",
        "state": "MH",
        "postal_code": "411018",
        "country": "IN"
      },
      "title": "Example offer title",
      "discount_type": "percentage",
      "discount_value": 10,
      "source_name": "Public website",
      "source_url": "https://example.com/offers/sample",
      "collected_at": "2026-08-31T12:00:00Z"
    }
  ]
}
```

## Safety checks

The importer refuses:

- SQLite / `dev.db`
- `localhost` / `127.0.0.1` database URLs
- `APP_ENV=development`

DEV must remain untouched.

## Automation

Render Free Beta has **no scheduled worker** in this project. Do **not** run a fake cron.

Manual execution is the supported path:

1. Collect JSON locally.
2. Run import/refresh script against Neon via `.env.beta`.
3. Review report.

Future automation can wrap the same script when infrastructure allows.

## Rollback / recovery

- **Bad import row**: fix JSON and re-run; use stable `external_source_key`.
- **Duplicate created in error**: dedup key prevents duplicates on re-run.
- **Stale offer still visible**: expected until explicit deactivation — check `stale_candidates`, verify source, then re-import with past `ends_at` if source confirms end.
- **Wrong update**: re-import corrected JSON with same `external_source_key`.
- **Audit trail**: query `audit_logs` where `resource_type='external_offer_import'`.

## Importer behavior summary

- Sets `source_type=EXTERNAL`, `is_verified=false`.
- Ongoing offers may have `ends_at` null.
- External offers cannot be AasPas Verified.
- No notification events on import.
- Duplicate `external_source_key` updates existing record.
