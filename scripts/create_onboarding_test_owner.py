"""Development-only: create onboarding-test@aaspas.demo shop owner without shop."""

from __future__ import annotations

import json
import sqlite3
import sys
import urllib.error
import urllib.request

BASE = "http://10.181.105.174:8000"
EMAIL = "onboarding-test@aaspas.demo"
PASSWORD = "onboarding-dev-2026-aaspas"
PILOT = "pilot-owner@aaspas.demo"


def req(method: str, path: str, body: dict | None = None, token: str | None = None) -> tuple[int, dict]:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode()
        try:
            parsed = json.loads(payload)
        except json.JSONDecodeError:
            parsed = {"raw": payload}
        return exc.code, parsed


def main() -> int:
    status, reg = req(
        "POST",
        "/api/v1/auth/register",
        {
            "email": EMAIL,
            "password": PASSWORD,
            "full_name": "Onboarding Test Owner",
            "role": "shop_owner",
        },
    )
    print(f"REGISTER_STATUS={status}")

    status, login = req("POST", "/api/v1/auth/login", {"email": EMAIL, "password": PASSWORD})
    print(f"LOGIN_STATUS={status}")
    token = login.get("data", {}).get("access_token")
    if not token:
        print(json.dumps(login, indent=2))
        return 1

    status, me = req("GET", "/api/v1/auth/me", token=token)
    print(f"ME_STATUS={status}")
    print(f"ME_ROLE={me.get('data', {}).get('role')}")

    status, dash = req("GET", "/api/v1/shops/me/dashboard", token=token)
    print(f"DASHBOARD_STATUS={status}")

    conn = sqlite3.connect("dev.db")
    cur = conn.cursor()
    cur.execute(
        "SELECT COUNT(*) FROM shops s JOIN users u ON u.id = s.owner_id WHERE u.email = ?",
        (EMAIL,),
    )
    shop_count = cur.fetchone()[0]
    cur.execute(
        "SELECT COUNT(*) FROM shops s JOIN users u ON u.id = s.owner_id WHERE u.email = ?",
        (PILOT,),
    )
    pilot_shop_count = cur.fetchone()[0]
    cur.execute("SELECT email, role FROM users WHERE email = ?", (PILOT,))
    pilot_user = cur.fetchone()
    conn.close()

    print(f"SHOP_COUNT={shop_count}")
    print(f"PILOT_SHOP_COUNT={pilot_shop_count}")
    print(f"PILOT_USER={pilot_user}")

    status, create_probe = req("POST", "/api/v1/shops", {"name": "x"}, token=token)
    print(f"CREATE_PROBE_STATUS={status}")
    error_code = create_probe.get("error", {}).get("code")
    print(f"CREATE_PROBE_ERROR_CODE={error_code}")

    if status == 403:
        print("CREATE_PERMISSION=FORBIDDEN")
        return 1
    if status in {422, 400}:
        print("CREATE_PERMISSION=ALLOWED (validation rejected incomplete body)")
    else:
        print(f"CREATE_PERMISSION=UNEXPECTED_STATUS_{status}")

    print(f"TEST_PASSWORD={PASSWORD}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
