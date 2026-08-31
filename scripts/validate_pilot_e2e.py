"""Live API validation for pilot customer journey (run against running FastAPI).

Usage:
    python scripts/validate_pilot_e2e.py [--base-url http://192.168.1.6:8000]
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

PIMPRI_LAT = 18.6298
PIMPRI_LON = 73.7997
CHINCHWAD_LAT = 18.6278
CHINCHWAD_LON = 73.7911


def get(base: str, path: str, token: str | None = None) -> tuple[int, dict]:
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{base.rstrip('/')}{path}", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        body = exc.read().decode()
        try:
            payload = json.loads(body)
        except json.JSONDecodeError:
            payload = {"raw": body}
        return exc.code, payload


def post(base: str, path: str, payload: dict) -> tuple[int, dict]:
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        f"{base.rstrip('/')}{path}",
        data=data,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        body = exc.read().decode()
        try:
            parsed = json.loads(body)
        except json.JSONDecodeError:
            parsed = {"raw": body}
        return exc.code, parsed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8000")
    args = parser.parse_args()
    base = args.base_url
    results: list[str] = []
    failures = 0

    def check(name: str, ok: bool, detail: str = "") -> None:
        nonlocal failures
        status = "PASS" if ok else "FAIL"
        if not ok:
            failures += 1
        line = f"[{status}] {name}" + (f" — {detail}" if detail else "")
        results.append(line)
        print(line)

    code, health = get(base, "/health")
    check("Health", code == 200 and health.get("success") is True, f"status={code}")

    code, home_pimpri = get(base, f"/api/v1/home?latitude={PIMPRI_LAT}&longitude={PIMPRI_LON}")
    data = home_pimpri.get("data", {}) if code == 200 else {}
    today = data.get("today_offers") or []
    soon = data.get("coming_soon") or []
    shops = data.get("nearby_shops") or []
    check("Home Pimpri", code == 200, f"today={len(today)} soon={len(soon)} shops={len(shops)}")

    code, home_chinchwad = get(
        base, f"/api/v1/home?latitude={CHINCHWAD_LAT}&longitude={CHINCHWAD_LON}"
    )
    ch_data = home_chinchwad.get("data", {}) if code == 200 else {}
    check(
        "Home Chinchwad",
        code == 200,
        f"shops={len(ch_data.get('nearby_shops') or [])}",
    )

    code, cats = get(base, "/api/v1/categories")
    categories = cats.get("data") or [] if code == 200 else []
    check("Categories", code == 200 and len(categories) >= 1, f"count={len(categories)}")

    if categories:
        cat_id = categories[0]["id"]
        code, cat_offers = get(
            base,
            f"/api/v1/categories/{cat_id}/offers?latitude={PIMPRI_LAT}&longitude={PIMPRI_LON}",
        )
        offers = cat_offers.get("data") or [] if code == 200 else []
        check("Category offers", code == 200, f"count={len(offers)}")

    code, search = get(
        base,
        f"/api/v1/search?q=fashion&latitude={PIMPRI_LAT}&longitude={PIMPRI_LON}",
    )
    search_offers = search.get("data", {}).get("offers") or [] if code == 200 else []
    hidden = [o for o in search_offers if o.get("title", "").lower().find("draft") >= 0]
    check(
        "Search excludes draft",
        code == 200 and not hidden,
        f"offers={len(search_offers)}",
    )

    if today:
        offer_id = today[0].get("offer_id") or today[0].get("id")
        code, offer_detail = get(base, f"/api/v1/offers/{offer_id}")
        check("Offer details active", code == 200, str(offer_id))
    else:
        check("Offer details active", False, "no today offers to test")

    if shops:
        shop_id = shops[0].get("shop_id") or shops[0].get("id")
        code, shop_detail = get(base, f"/api/v1/shops/{shop_id}")
        check("Shop details", code == 200, str(shop_id))
    else:
        check("Shop details", False, "no nearby shops to test")

    email = f"e2e-{int(PIMPRI_LAT * 1000)}@example.com"
    code, reg = post(
        base,
        "/api/v1/auth/register",
        {"email": email, "password": "password123", "full_name": "E2E User"},
    )
    token = None
    if code in {200, 201}:
        code, login = post(base, "/api/v1/auth/login", {"email": email, "password": "password123"})
        token = login.get("data", {}).get("access_token") if code == 200 else None
    check("Auth register/login", token is not None)

    code, fav_guest = get(base, "/api/v1/favorites")
    check("Favorites guest blocked", code in {401, 403}, f"status={code}")

    if token and today:
        offer_id = today[0].get("offer_id") or today[0].get("id")
        save_req = urllib.request.Request(
            f"{base.rstrip('/')}/api/v1/favorites/offers/{offer_id}",
            headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(save_req, timeout=15) as resp:
                save_code = resp.status
        except urllib.error.HTTPError as exc:
            save_code = exc.code
        check("Favorites save authed", save_code in {200, 201, 204}, f"status={save_code}")

        code, fav_list = get(base, "/api/v1/favorites", token=token)
        fav_offers = fav_list.get("data", {}).get("offers") or [] if code == 200 else []
        check("Favorites list authed", code == 200, f"count={len(fav_offers)}")

    print("\nSummary:")
    print("\n".join(results))
    print(f"\nTotal failures: {failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
