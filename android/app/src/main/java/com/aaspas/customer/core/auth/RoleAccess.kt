package com.aaspas.customer.core.auth

object RoleAccess {
    fun canAccessShopOwner(role: String?): Boolean = role == UserRoles.SHOP_OWNER

    fun canAccessAdmin(role: String?): Boolean = role == UserRoles.ADMIN

    fun isCustomerOnly(role: String?): Boolean {
        return role.isNullOrBlank() || role == UserRoles.CUSTOMER
    }
}
