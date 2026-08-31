package com.aaspas.customer.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimePickerFieldsTest {

    @Test
    fun `formats hour minute as HH colon MM`() {
        assertEquals("09:05", formatHourMinute(9, 5))
        assertEquals("21:30", formatHourMinute(21, 30))
    }

    @Test
    fun `parses hour minute from time string`() {
        assertEquals(14 to 45, parseHourMinute("14:45"))
        assertEquals(9 to 0, parseHourMinute(""))
    }

    @Test
    fun `converts iso date to display text`() {
        assertEquals("21 Aug 2026", formatDateForDisplay("2026-08-21"))
    }

    @Test
    fun `converts millis back to iso date`() {
        val millis = parseIsoDateToMillis("2026-08-21")!!
        assertEquals("2026-08-21", millisToIsoDate(millis))
    }
}
