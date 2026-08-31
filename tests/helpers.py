SHOP_ONBOARDING_PAYLOAD = {
    "name": "Fresh Mart",
    "category": "grocery",
    "description": "Neighborhood grocery",
    "contact_number": "+919876543210",
    "address": {
        "address_line1": "123 Main Road",
        "city": "Mumbai",
        "state": "MH",
        "postal_code": "400001",
        "country": "IN",
        "latitude": "19.076090",
        "longitude": "72.877426",
    },
    "business_hours": {"opens_at": "09:00", "closes_at": "21:00"},
}


def register_and_login(client, email: str, role: str = "shop_owner") -> dict:
    client.post(
        "/api/v1/auth/register",
        json={"email": email, "password": "password123", "role": role},
    )
    login = client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "password123"},
    )
    token = login.json()["data"]["access_token"]
    return {"Authorization": f"Bearer {token}"}


def register_customer(client, email: str) -> dict:
    return register_and_login(client, email, role="customer")


def create_admin_user(db_session, email: str = "admin@example.com"):
    from aaspas.common.security.auth import hash_password
    from aaspas.modules.auth.models import User

    user = User(
        email=email,
        password_hash=hash_password("password123"),
        role="admin",
        full_name="Platform Admin",
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


def admin_headers(client, db_session, email: str = "admin@example.com") -> dict:
    create_admin_user(db_session, email)
    token = client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": "password123"},
    ).json()["data"]["access_token"]
    return {"Authorization": f"Bearer {token}"}


def create_shop(client, headers: dict, payload: dict | None = None):
    return client.post("/api/v1/shops", json=payload or SHOP_ONBOARDING_PAYLOAD, headers=headers)


def submit_shop(client, shop_id: str, headers: dict):
    return client.post(f"/api/v1/shops/{shop_id}/submit", headers=headers)
