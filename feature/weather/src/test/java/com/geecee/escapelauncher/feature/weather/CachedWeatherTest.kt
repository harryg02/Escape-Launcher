package com.geecee.escapelauncher.feature.weather

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedWeatherTest {
    private val fetchedAt = 1_000_000_000L
    private val minute = 60 * 1000L
    private val cached = CachedWeather(text = "12°C", useFahrenheit = false, fetchedAtMillis = fetchedAt)

    @Test
    fun `fresh straight after fetching`() {
        assertTrue(cached.isFresh(nowMillis = fetchedAt, useFahrenheit = false))
    }

    @Test
    fun `fresh until fifteen minutes`() {
        assertTrue(cached.isFresh(nowMillis = fetchedAt + 15 * minute - 1, useFahrenheit = false))
        assertFalse(cached.isFresh(nowMillis = fetchedAt + 15 * minute, useFahrenheit = false))
    }

    @Test
    fun `not fresh in the other unit`() {
        assertFalse(cached.isFresh(nowMillis = fetchedAt + minute, useFahrenheit = true))
    }

    @Test
    fun `not fresh if the clock went back before the fetch`() {
        assertFalse(cached.isFresh(nowMillis = fetchedAt - minute, useFahrenheit = false))
    }

    @Test
    fun `kept for two hours after a failed fetch`() {
        val keep = CachedWeather.KEEP_AFTER_FAILURE_MILLIS
        assertTrue(cached.isFresh(fetchedAt + 60 * minute, useFahrenheit = false, maxAgeMillis = keep))
        assertFalse(cached.isFresh(fetchedAt + 120 * minute, useFahrenheit = false, maxAgeMillis = keep))
    }
}
