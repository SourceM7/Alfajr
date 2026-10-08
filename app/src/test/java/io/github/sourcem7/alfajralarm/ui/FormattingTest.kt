package io.github.sourcem7.alfajralarm.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {
    @Test
    fun `the day period is removed wherever the locale puts it`() {
        assertEquals("h:mm", withoutDayPeriod("h:mm a"))
        assertEquals("h:mm", withoutDayPeriod("a h:mm"))
        assertEquals("h:mm", withoutDayPeriod("h:mm\u202Fa"))
        assertEquals("h:mm", withoutDayPeriod("h:mm B"))
    }

    @Test
    fun `quoted literals keep their letters`() {
        assertEquals("h 'h at' mm", withoutDayPeriod("h 'h at' mm a"))
    }

    @Test
    fun `a pattern without a day period is unchanged`() {
        assertEquals("HH:mm", withoutDayPeriod("HH:mm"))
    }
}
