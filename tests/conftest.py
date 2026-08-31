import os

import pytest
from fastapi.testclient import TestClient

pytest_plugins = ["tests.pilot_fixtures"]

# Use in-memory SQLite for tests (no PostgreSQL required locally)
os.environ["DATABASE_URL"] = "sqlite:///:memory:"
os.environ["SECRET_KEY"] = "test-secret-key"
os.environ["APP_ENV"] = "testing"
os.environ["RATE_LIMIT_PER_MINUTE"] = "10000"
os.environ.pop("MEDIA_PUBLIC_BASE_URL", None)

# Ensure all models are registered before create_all
from aaspas.database import Base, SessionLocal, engine, get_db  # noqa: E402
from aaspas.main import app  # noqa: E402
from aaspas.config import get_settings  # noqa: E402

get_settings.cache_clear()
from aaspas.modules.auth.models import User  # noqa: E402, F401
from aaspas.modules.category.models import Category  # noqa: E402, F401
from aaspas.modules.customer.models import CustomerProfile  # noqa: E402, F401
from aaspas.modules.favorite.models import Favorite  # noqa: E402, F401
from aaspas.modules.location.models import Location  # noqa: E402, F401
from aaspas.modules.notification.models import Notification  # noqa: E402, F401
from aaspas.modules.offer.models import Offer  # noqa: E402, F401
from aaspas.modules.shop.models import Shop  # noqa: E402, F401


@pytest.fixture(autouse=True)
def setup_database():
    from aaspas.config import get_settings

    get_settings.cache_clear()
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)
    get_settings.cache_clear()


@pytest.fixture
def db_session():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def client(db_session):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()
