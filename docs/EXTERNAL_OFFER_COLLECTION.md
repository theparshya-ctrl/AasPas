# External Offer Collection Workflow

Phase 1 provides the import system only. Real public offers are collected manually and imported into **Beta Neon PostgreSQL** — never into DEV `dev.db`.

## Daily workflow

1. Collect current public offers from allowed sources (shop websites, public social posts, flyers with verifiable URLs).
2. Record each offer in a JSON file using the structure below.
3. Assign a stable `external_source_key` per offer (deduplication key).
4. Set `collected_at` to the UTC timestamp when the offer was collected.
5. Validate the file without writing:

```powershell
cd H:\AasPas
$env:PYTHONPATH = "src"
python scripts/import_external_offers.py path\to\collected_offers.json --dry-run
```

6. Import into Beta only after validation:

```powershell
python scripts/import_external_offers.py path\to\collected_offers.json
```

7. Re-check customer home/search for the target area after import.
8. Remove or let expire stale offers when public sources show they ended.

## Target areas

- Pimpri
- Chinchwad
- Hadapsar
- Manjri
- Manjri Budruk
- Saswad
- Nearby relevant areas

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
| Latitude | `address.latitude` | When verified from public source |
| Longitude | `address.longitude` | When verified from public source |
| Offer title | `title` | Yes — preserve exact public wording (including "Up to …") |
| Offer description | `description` | Recommended |
| Offer type | `discount_type` | Yes — `percentage`, `up_to_percentage`, `fixed`, `up_to_fixed` |
| Discount value | `discount_value` | Yes |
| Start date/time | `starts_at` | Optional — omit when source has no start date |
| Expiry date/time | `ends_at` | Optional — **omit when source has no fixed expiry** |
| Source label | `source_name` | Yes — e.g. "Shop website", "Instagram post" |
| Source URL | `source_url` | Yes — public http(s) link |
| Collected date/time | `collected_at` | Yes — ISO 8601 UTC |
| Unique dedup key | `external_source_key` | Yes — min 8 chars, stable per offer |
| Stable shop key | `shop_external_source_key` | Optional — auto-generated if omitted |

## Collection rules

1. **Public sources only** — information visible without logging in.
2. **No login/paywall/CAPTCHA bypass** — if a source requires authentication or scraping tricks, skip it.
3. **No fabricated offers** — every field must come from a real public source.
4. **No expired offers** — skip offers explicitly expired on the source; do **not** invent an expiry date when the source shows an ongoing current offer.
5. **Exact local relevance** — shop must serve the target Pune-area locations above.
6. **Source URL required** — every record needs a verifiable public link.
7. **Collected date required** — record when you found the offer (`collected_at`).
8. **Expiry handling** — if the public source provides a fixed end date, use `ends_at`. If the source shows an ongoing/current offer with no fixed expiry, **leave `ends_at` null**. Never assign artificial 30/60/90-day expiries. Re-import updates `collected_at`; deactivate only when the source explicitly expires or disappears after repeated checks.
9. **"Up to" offers are valid** — preserve exact wording such as "Save up to 70%"; use `discount_type=up_to_percentage` when appropriate. Do not convert "up to" into a guaranteed discount.
10. **Duplicate handling** — same `external_source_key` updates the existing Beta record instead of creating a duplicate.
11. **Customer disclosure** — external offers always show **"Not confirmed by AasPas"** in the app; they are never AasPas Verified.
12. **Images** — do not copy or rehost external images unless you have clear permission; leave photos empty unless AasPas has rights.
13. **Removing stale offers** — when a source explicitly shows an offer ended, re-import with `ends_at` in the past or mark for deactivation per review workflow. Do not auto-expire solely because days passed.
14. **Daily collection workflow**:

```
Public source
    → current offer found
    → business/location validation
    → offer validity check
    → duplicate check
    → EXTERNAL / NOT_CONFIRMED
    → import or update (refresh collected_at)
    → daily refresh
```

Manual collection is acceptable initially. Do not bypass login walls, paywalls, CAPTCHA, or access restrictions.

## JSON file structure

Start from `data/external_offers/template.json` (empty `offers` array). See `docs/EXTERNAL_OFFER_COLLECTION.md` example for one illustrative record shape.

### Address object

```json
{
  "address_line1": "Shop 12, Main Road",
  "address_line2": null,
  "city": "Pimpri-Chinchwad",
  "state": "MH",
  "postal_code": "411018",
  "country": "IN",
  "latitude": 18.6298,
  "longitude": 73.7997
}
```

### Example record (illustrative — replace with real collected data)

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
        "country": "IN",
        "latitude": 18.6298,
        "longitude": 73.7997
      },
      "title": "Example offer title",
      "description": "Example description from public source",
      "discount_type": "percentage",
      "discount_value": 10,
      "starts_at": "2026-09-01T00:00:00Z",
      "ends_at": "2026-09-30T23:59:59Z",
      "source_name": "Public website",
      "source_url": "https://example.com/offers/sample",
      "collected_at": "2026-08-31T12:00:00Z"
    }
  ]
}
```

## Importer behavior

- Sets `source_type=EXTERNAL` and `is_verified=false` on every imported offer.
- Ongoing offers may have `ends_at` null — shown as active until explicitly expired or removed.
- **"Up to"** titles are preserved verbatim; use `up_to_percentage` / `up_to_fixed` discount types when helpful.
- External offers **cannot** be approved as AasPas Verified.
- Import does **not** trigger notification events.
- Duplicate `external_source_key` values update the existing record.

## Safety checks

The importer refuses:

- SQLite / `dev.db`
- `localhost` / `127.0.0.1` database URLs
- `APP_ENV=development`

Use `.env.beta` (or `AASPAS_ENV_FILE`) with the Neon `DATABASE_URL` and `APP_ENV=staging`.

DEV `dev.db`, local uploads, and Tailscale settings must remain untouched.
