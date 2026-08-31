package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.ShopOwnerProfileUpdateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopOwnerPhotoMapperTest {
    @Test
    fun `photo update dto only includes photo url`() {
        val dto = ShopOwnerMapper.toPhotoUpdateDto("https://example.com/shop.jpg")
        assertEquals("https://example.com/shop.jpg", dto.photoUrl)
        assertNull(dto.name)
        assertNull(dto.category)
        assertNull(dto.address)
    }
}
