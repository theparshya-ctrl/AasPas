# External Offer JSON — Field Guide

Copy `template.json` to a new file (e.g. `pimpri_2026-09-01.json`) and add records under `offers`.

The importer automatically sets `source_type=EXTERNAL` and `is_verified=false`. Do **not** include those in JSON.

| Collector label | JSON field |
|-----------------|------------|
| Shop name | `shop_name` |
| Category | `category` |
| Address line | `address.address_line1` |
| City | `address.city` |
| Latitude | `address.latitude` |
| Longitude | `address.longitude` |
| Offer title | `title` |
| Offer description | `description` |
| Offer type | `discount_type` |
| Discount value | `discount_value` |
| Start date | `starts_at` |
| Expiry date | `ends_at` |
| Source name | `source_name` |
| Source URL | `source_url` |
| Collected date | `collected_at` |
| Unique key | `external_source_key` |

Full workflow: `docs/EXTERNAL_OFFER_COLLECTION.md`
