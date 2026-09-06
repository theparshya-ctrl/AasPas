"""Module registry — central place to wire domain modules into the API."""

from dataclasses import dataclass

from fastapi import APIRouter


@dataclass(frozen=True)
class ModuleInfo:
    name: str
    prefix: str
    router: APIRouter
    enabled: bool = True
    description: str = ""


def get_domain_modules() -> list[ModuleInfo]:
    """Import and return all domain module routers.

    Disabled/future modules remain registered but can be toggled off via config later.
    """
    from aaspas.modules.admin.router import router as admin_router
    from aaspas.modules.analytics.router import router as analytics_router
    from aaspas.modules.app.router import router as app_router
    from aaspas.modules.auth.router import router as auth_router
    from aaspas.modules.category.router import router as category_router
    from aaspas.modules.customer.router import router as customer_router
    from aaspas.modules.favorite.router import router as favorite_router
    from aaspas.modules.home.router import router as home_router
    from aaspas.modules.location.router import router as location_router
    from aaspas.modules.loyalty.router import router as loyalty_router
    from aaspas.modules.notification.router import router as notification_router
    from aaspas.modules.offer.router import router as offer_router
    from aaspas.modules.search.router import router as search_router
    from aaspas.modules.shop.router import router as shop_router
    from aaspas.modules.subscription.router import router as subscription_router

    return [
        ModuleInfo("auth", "/auth", auth_router, description="Authentication & user management"),
        ModuleInfo("app", "/app", app_router, description="App metadata & Beta updates"),
        ModuleInfo("customer", "/customers", customer_router, description="Customer profiles"),
        ModuleInfo("favorite", "/favorites", favorite_router, description="Customer favorites"),
        ModuleInfo("shop", "/shops", shop_router, description="Shop/merchant management"),
        ModuleInfo("offer", "/offers", offer_router, description="Offers & promotions"),
        ModuleInfo("category", "/categories", category_router, description="Offer/shop categories"),
        ModuleInfo("home", "/home", home_router, description="Customer home feed"),
        ModuleInfo("location", "/locations", location_router, description="Locations & geo"),
        ModuleInfo("search", "/search", search_router, description="Search & discovery"),
        ModuleInfo(
            "notification", "/notifications", notification_router, description="Notifications"
        ),
        ModuleInfo("analytics", "/analytics", analytics_router, description="Analytics & metrics"),
        ModuleInfo("admin", "/admin", admin_router, description="Admin operations"),
        ModuleInfo(
            "subscription",
            "/subscriptions",
            subscription_router,
            enabled=False,
            description="Subscriptions & payments (future)",
        ),
        ModuleInfo(
            "loyalty",
            "/loyalty",
            loyalty_router,
            enabled=False,
            description="Loyalty & rewards (future)",
        ),
    ]
