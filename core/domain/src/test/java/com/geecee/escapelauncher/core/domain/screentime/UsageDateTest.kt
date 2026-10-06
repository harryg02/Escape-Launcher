package com.geecee.escapelauncher.core.domain.screentime

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class UsageDateTest {

    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `days are written as ISO dates`() {
        assertEquals("2026-01-09", usageDate(LocalDate.of(2026, 1, 9)))
    }

    @Test
    fun `the device language does not change how a day is written`() {
        val day = LocalDate.of(2026, 10, 5)

        listOf("ar-EG", "fa-IR", "hi-IN-u-nu-deva", "th-TH-u-ca-buddhist", "en-US").forEach { tag ->
            Locale.setDefault(Locale.forLanguageTag(tag))
            assertEquals(tag, "2026-10-05", usageDate(day))
        }
    }
}
