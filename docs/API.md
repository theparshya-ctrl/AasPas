# AasPas API Notes

## Search API (CURSOR-007 breaking change)

`GET /api/v1/search` was intentionally changed from a mixed shop/merchant response to an **offer-first customer discovery** response.

### Previous (pre CURSOR-007)
```json
{
  "shops": [ /* full ShopResponse[] */ ],
  "offers": [ /* full OfferResponse[] */ ],
  "total_shops": 0,
  "total_offers": 0
}
```

### Current (CURSOR-007+)
```json
{
  "offers": [ /* CustomerSearchOfferItem[] with status, shop summary */ ],
  "total": 0,
  "page": 1,
  "page_size": 20,
  "total_pages": 0
}
```

**Notes:**
- No shop directory results — AasPas remains offer-first.
- Visibility rules enforced server-side (draft/pending/expired/inactive-shop excluded).
- Query params: `q`, `category_id`, `latitude`, `longitude`, `radius_km`, `page`, `page_size`.
- Removed legacy params: `city`, string `category`.
- The Android customer app is the only known consumer; the change is intentional.

## Favorites API (CURSOR-008)

Customer-authenticated endpoints under `/api/v1/favorites`:
- `GET /` — list saved offers and shops
- `POST/DELETE /offers/{offer_id}`
- `POST/DELETE /shops/{shop_id}`

Optional `is_saved` on Home/Search offer items when `Authorization: Bearer` is provided for a customer.

## Session / tokens (CURSOR-009)

- Login returns a bearer access token with `expires_in` (default 3600 seconds / 60 minutes).
- **No refresh-token endpoint** exists yet; clients should treat expired/401 responses as logout and re-prompt sign-in.
- Android stores token + expiry in private SharedPreferences (not encrypted; acceptable for MVP — migrate to EncryptedSharedPreferences when hardening).
- Profile read uses existing `GET /api/v1/auth/me` (email, full_name, role, created_at).
