package com.geecee.escapelauncher.feature.weather

/**
 * The last temperature fetched, so returning home doesn't ask for location and the network each
 * time.
 *
 * @param text The text shown on the home screen, e.g. "12°C"
 * @param useFahrenheit The unit [text] is in
 * @param fetchedAtMillis When it was fetched, from [System.currentTimeMillis]
 */
data class CachedWeather(
    val text: String,
    val useFahrenheit: Boolean,
    val fetchedAtMillis: Long
) {
    /**
     * Whether this can be shown instead of fetching again: it is in the [useFahrenheit] unit and
     * is less than [maxAgeMillis] old. A fetch time in the future (the clock was changed) is not
     * fresh.
     */
    fun isFresh(
        nowMillis: Long,
        useFahrenheit: Boolean,
        maxAgeMillis: Long = FRESH_FOR_MILLIS
    ): Boolean {
        val age = nowMillis - fetchedAtMillis
        return this.useFahrenheit == useFahrenheit && age in 0 until maxAgeMillis
    }

    companion object {
        /** Reused without fetching for this long. */
        const val FRESH_FOR_MILLIS = 15 * 60 * 1000L

        /** Kept on screen for this long if a later fetch fails, then hidden. */
        const val KEEP_AFTER_FAILURE_MILLIS = 2 * 60 * 60 * 1000L
    }
}
