package com.geecee.escapelauncher.core.data.repository.db

import com.geecee.escapelauncher.core.model.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageKeysTest {

    private val rows = listOf(
        "com.example.chat-2026-10-05" to 120_000L,
        "com.example.video-player-2026-10-05" to 600_000L,
        "com.example.chat-2026-10-04" to 30_000L,
        "com.example.old-2026-09-01" to 5_000L
    )

    @Test
    fun `groups rows by date and keeps dashes in package names`() {
        val grouped = groupUsageByDate(rows, listOf("2026-10-04", "2026-10-05"))

        assertEquals(
            listOf(
                AppUsage("com.example.video-player", 600_000L),
                AppUsage("com.example.chat", 120_000L)
            ),
            grouped["2026-10-05"]
        )
        assertEquals(listOf(AppUsage("com.example.chat", 30_000L)), grouped["2026-10-04"])
    }

    @Test
    fun `dates without usage are present and empty`() {
        val grouped = groupUsageByDate(rows, listOf("2026-10-03"))

        assertEquals(mapOf("2026-10-03" to emptyList<AppUsage>()), grouped)
    }

    @Test
    fun `keys outside the kept dates are deleted`() {
        val toDelete = usageKeysToDelete(rows.map { it.first }, listOf("2026-10-04", "2026-10-05"))

        assertEquals(listOf("com.example.old-2026-09-01"), toDelete)
    }

    @Test
    fun `usage key joins package and date`() {
        assertEquals("com.example.chat-2026-10-05", usageKey("com.example.chat", "2026-10-05"))
    }

    @Test
    fun `keys written with other digits are rewritten with an ISO date`() {
        // Arabic-Indic, Extended Arabic-Indic (Persian) and Devanagari digits for 2026-10-05
        assertEquals("com.example.chat-2026-10-05", normaliseUsageKey("com.example.chat-٢٠٢٦-١٠-٠٥"))
        assertEquals("com.example.chat-2026-10-05", normaliseUsageKey("com.example.chat-۲۰۲۶-۱۰-۰۵"))
        assertEquals("com.example.chat-2026-10-05", normaliseUsageKey("com.example.chat-२०२६-१०-०५"))
    }

    @Test
    fun `direction marks around the date are dropped`() {
        assertEquals(
            "com.example.chat-2026-10-05",
            normaliseUsageKey("com.example.chat-\u200F٢٠٢٦-\u200F١٠-\u200F٠٥")
        )
    }

    @Test
    fun `ISO keys and dashes in package names are left alone`() {
        assertEquals("com.example.video-player-2026-10-05", normaliseUsageKey("com.example.video-player-2026-10-05"))
    }

    @Test
    fun `keys without a readable date are left alone`() {
        assertEquals("com.example.chat-2026-13-40", normaliseUsageKey("com.example.chat-2026-13-40"))
        assertEquals("com.example.chat", normaliseUsageKey("com.example.chat"))
        assertEquals("com.example.chat-05/10/2026", normaliseUsageKey("com.example.chat-05/10/2026"))
    }

    @Test
    fun `rows for the same app and day are added together`() {
        val normalised = normaliseUsageKeys(
            listOf(
                "com.example.chat-٢٠٢٦-١٠-٠٥" to 60_000L,
                "com.example.chat-2026-10-05" to 30_000L,
                "com.example.chat-٢٠٢٦-١٠-٠٤" to 10_000L,
                "com.example.video-2026-10-05" to 5_000L
            )
        )

        assertEquals(
            mapOf(
                "com.example.chat-2026-10-05" to 90_000L,
                "com.example.chat-2026-10-04" to 10_000L,
                "com.example.video-2026-10-05" to 5_000L
            ),
            normalised
        )
    }
}
