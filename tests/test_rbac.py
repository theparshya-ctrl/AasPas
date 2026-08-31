from aaspas.common.security.rbac import Permission, UserRole, has_permission, role_at_least


def test_role_hierarchy():
    assert role_at_least(UserRole.ADMIN, UserRole.CUSTOMER)
    assert not role_at_least(UserRole.CUSTOMER, UserRole.ADMIN)


def test_permissions():
    assert has_permission(UserRole.CUSTOMER, Permission.OFFER_READ)
    assert not has_permission(UserRole.CUSTOMER, Permission.ADMIN_READ_ALL)
    assert has_permission(UserRole.SHOP_OWNER, Permission.OFFER_CREATE_OWN)
