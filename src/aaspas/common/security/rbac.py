from enum import StrEnum


class UserRole(StrEnum):
    CUSTOMER = "customer"
    SHOP_OWNER = "shop_owner"
    SHOP_STAFF = "shop_staff"
    ADMIN = "admin"
    SUPER_ADMIN = "super_admin"


# Role hierarchy for permission checks (higher index = more privilege)
ROLE_HIERARCHY: list[UserRole] = [
    UserRole.CUSTOMER,
    UserRole.SHOP_STAFF,
    UserRole.SHOP_OWNER,
    UserRole.ADMIN,
    UserRole.SUPER_ADMIN,
]


def role_at_least(user_role: UserRole, required: UserRole) -> bool:
    return ROLE_HIERARCHY.index(user_role) >= ROLE_HIERARCHY.index(required)


# Permission strings — avoid hard-coded checks scattered in code
class Permission(StrEnum):
    # Customer
    CUSTOMER_READ_SELF = "customer:read:self"
    CUSTOMER_UPDATE_SELF = "customer:update:self"

    # Shop
    SHOP_CREATE = "shop:create"
    SHOP_READ_OWN = "shop:read:own"
    SHOP_UPDATE_OWN = "shop:update:own"
    SHOP_MANAGE_STAFF = "shop:manage:staff"

    # Offer
    OFFER_CREATE_OWN = "offer:create:own"
    OFFER_READ = "offer:read"
    OFFER_UPDATE_OWN = "offer:update:own"
    OFFER_DELETE_OWN = "offer:delete:own"

    # Admin
    ADMIN_READ_ALL = "admin:read:all"
    ADMIN_MANAGE_USERS = "admin:manage:users"
    ADMIN_MANAGE_SHOPS = "admin:manage:shops"
    ADMIN_VIEW_ANALYTICS = "admin:view:analytics"


ROLE_PERMISSIONS: dict[UserRole, set[Permission]] = {
    UserRole.CUSTOMER: {
        Permission.CUSTOMER_READ_SELF,
        Permission.CUSTOMER_UPDATE_SELF,
        Permission.OFFER_READ,
    },
    UserRole.SHOP_STAFF: {
        Permission.CUSTOMER_READ_SELF,
        Permission.SHOP_READ_OWN,
        Permission.OFFER_READ,
        Permission.OFFER_CREATE_OWN,
        Permission.OFFER_UPDATE_OWN,
    },
    UserRole.SHOP_OWNER: {
        Permission.CUSTOMER_READ_SELF,
        Permission.CUSTOMER_UPDATE_SELF,
        Permission.SHOP_CREATE,
        Permission.SHOP_READ_OWN,
        Permission.SHOP_UPDATE_OWN,
        Permission.SHOP_MANAGE_STAFF,
        Permission.OFFER_CREATE_OWN,
        Permission.OFFER_READ,
        Permission.OFFER_UPDATE_OWN,
        Permission.OFFER_DELETE_OWN,
    },
    UserRole.ADMIN: {
        Permission.ADMIN_READ_ALL,
        Permission.ADMIN_MANAGE_USERS,
        Permission.ADMIN_MANAGE_SHOPS,
        Permission.ADMIN_VIEW_ANALYTICS,
        Permission.OFFER_READ,
    },
    UserRole.SUPER_ADMIN: set(Permission),
}


def has_permission(role: UserRole, permission: Permission) -> bool:
    return permission in ROLE_PERMISSIONS.get(role, set())
