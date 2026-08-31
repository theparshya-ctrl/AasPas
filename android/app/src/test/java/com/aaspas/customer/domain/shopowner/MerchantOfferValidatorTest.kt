package com.aaspas.customer.domain.shopowner

import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantOfferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantOfferValidatorTest {

    @Test
    fun `draft requires title and valid discount`() {
        val draft = MerchantOfferDraft(shopId = "shop-1")
        assertNotNull(MerchantOfferValidator.validateDraft(draft))

        val valid = draft.copy(title = "Sale", discountValue = "10")
        assertNull(MerchantOfferValidator.validateDraft(valid))
    }

    @Test
    fun `percentage must be between 0 and 100`() {
        assertNotNull(MerchantOfferValidator.validateDiscount("percentage", "0"))
        assertNotNull(MerchantOfferValidator.validateDiscount("percentage", "101"))
        assertNull(MerchantOfferValidator.validateDiscount("percentage", "25"))
    }

    @Test
    fun `fixed discount must be positive`() {
        assertNotNull(MerchantOfferValidator.validateDiscount("fixed", "0"))
        assertNull(MerchantOfferValidator.validateDiscount("fixed", "50"))
    }

    @Test
    fun `submit requires description and valid dates`() {
        val draft = MerchantOfferDraft(
            shopId = "shop-1",
            title = "Weekend Sale",
            description = "Too short",
            discountValue = "10",
        )
        assertNotNull(MerchantOfferValidator.validateForSubmit(draft))

        val complete = draft.copy(
            description = "Valid on all items this weekend.",
            startDate = "2026-08-21",
            startTime = "09:00",
            endDate = "2026-08-21",
            endTime = "21:00",
        )
        assertNull(MerchantOfferValidator.validateForSubmit(complete))
    }

    @Test
    fun `end must be after start`() {
        val draft = MerchantOfferDraft(
            shopId = "shop-1",
            title = "Weekend Sale",
            description = "Valid on all items this weekend.",
            discountValue = "10",
            startDate = "2026-08-21",
            startTime = "21:00",
            endDate = "2026-08-21",
            endTime = "09:00",
        )
        assertNotNull(MerchantOfferValidator.validateForSubmit(draft))
    }

    @Test
    fun `editable only for draft and rejected`() {
        assertTrue(MerchantOfferValidator.isEditable(MerchantOfferStatus.Draft))
        assertTrue(MerchantOfferValidator.isEditable(MerchantOfferStatus.Rejected))
        assertTrue(!MerchantOfferValidator.isEditable(MerchantOfferStatus.PendingApproval))
        assertTrue(!MerchantOfferValidator.isEditable(MerchantOfferStatus.Active))
    }

    @Test
    fun `datetime combine and split`() {
        val iso = MerchantOfferDateTime.combine("2026-08-21", "09:30")
        assertEquals("2026-08-21T09:30:00Z", iso)
        assertEquals("2026-08-21", MerchantOfferDateTime.splitDate(iso))
        assertEquals("09:30", MerchantOfferDateTime.splitTime(iso))
    }
}
