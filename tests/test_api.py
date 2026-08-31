from tests.helpers import create_shop, register_and_login


def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["data"]["status"] == "ok"
    assert body["data"]["environment"] == "testing"


def test_register_and_login(client):
    reg = client.post(
        "/api/v1/auth/register",
        json={
            "email": "alice@example.com",
            "password": "password123",
            "full_name": "Alice",
            "role": "customer",
        },
    )
    assert reg.status_code == 201
    assert reg.json()["data"]["email"] == "alice@example.com"

    login = client.post(
        "/api/v1/auth/login",
        json={"email": "alice@example.com", "password": "password123"},
    )
    assert login.status_code == 200
    assert "access_token" in login.json()["data"]


def test_protected_route_requires_auth(client):
    response = client.get("/api/v1/auth/me")
    assert response.status_code == 401
    assert response.json()["success"] is False


def test_customer_profile_flow(client):
    client.post(
        "/api/v1/auth/register",
        json={"email": "bob@example.com", "password": "password123", "role": "customer"},
    )
    login = client.post(
        "/api/v1/auth/login",
        json={"email": "bob@example.com", "password": "password123"},
    )
    token = login.json()["data"]["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    create = client.post(
        "/api/v1/customers",
        json={"phone": "+919999999999", "preferred_language": "en"},
        headers=headers,
    )
    assert create.status_code == 201

    profile = client.get("/api/v1/customers/me", headers=headers)
    assert profile.status_code == 200
    assert profile.json()["data"]["phone"] == "+919999999999"


def test_shop_owner_isolation(client):
    owner_headers = register_and_login(client, "owner@example.com")
    shop = create_shop(client, owner_headers)
    assert shop.status_code == 201
    shop_id = shop.json()["data"]["id"]

    other_headers = register_and_login(client, "other@example.com")
    forbidden = client.patch(
        f"/api/v1/shops/{shop_id}",
        json={"name": "Hacked Shop"},
        headers=other_headers,
    )
    assert forbidden.status_code == 403


def test_search_public(client):
    response = client.get("/api/v1/search")
    assert response.status_code == 200
    data = response.json()["data"]
    assert "offers" in data
    assert "total" in data
