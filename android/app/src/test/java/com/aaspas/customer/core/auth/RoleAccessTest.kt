package com.aaspas.customer.core.auth

import com.aaspas.customer.core.auth.UserRoles.ADMIN
import com.aaspas.customer.core.auth.UserRoles.CUSTOMER
import com.aaspas.customer.core.auth.UserRoles.SHOP_OWNER
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAccessTest {

    @Test
    fun `customer cannot access shop owner or admin`() {
        assertTrue(RoleAccess.isCustomerOnly(CUSTOMER))
        assertFalse(RoleAccess.canAccessShopOwner(CUSTOMER))
        assertFalse(RoleAccess.canAccessAdmin(CUSTOMER))
    }

    @Test
    fun `shop owner can access dashboard but not admin`() {
        assertFalse(RoleAccess.isCustomerOnly(SHOP_OWNER))
        assertTrue(RoleAccess.canAccessShopOwner(SHOP_OWNER))
        assertFalse(RoleAccess.canAccessAdmin(SHOP_OWNER))
    }

    @Test
    fun `admin can access admin console only`() {
        assertFalse(RoleAccess.isCustomerOnly(ADMIN))
        assertFalse(RoleAccess.canAccessShopOwner(ADMIN))
        assertTrue(RoleAccess.canAccessAdmin(ADMIN))
    }
}
