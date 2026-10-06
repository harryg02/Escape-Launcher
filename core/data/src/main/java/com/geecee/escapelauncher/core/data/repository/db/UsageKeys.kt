package com.geecee.escapelauncher.core.data.repository.db

import com.geecee.escapelauncher.core.domain.screentime.usageDate
import com.geecee.escapelauncher.core.model.AppUsage
import java.time.DateTimeException
import java.time.LocalDate

/*
 * Usage rows are keyed "<packageName>-<date>". Package names can contain '-' and so can the
 * date, so a key is only ever matched against a known date suffix, never split on '-'.
 */

internal fun usageKey(packageName: String, date: String): String = "$packageName-$date"

// A date at the end of a key, in any script's digits (older versions wrote the device's digits)
private val keyWithDate = Regex("""^(.+)-(\p{Nd}{4})-(\p{Nd}{2})-(\p{Nd}{2})$""")

/**
 * Rewrites a key whose date was written with the device language's digits (for example
 * Arabic-Indic or Devanagari) to use an ISO date. Keys that already do, or whose date can't be
 * read, are returned unchanged.
 */
internal fun normaliseUsageKey(key: String): String {
    // Right-to-left languages can add invisible direction marks
    val cleaned = key.filterNot { Character.getType(it) == Character.FORMAT.toInt() }
    val match = keyWithDate.matchEntire(cleaned) ?: return key
    val (packageName, year, month, day) = match.destructured

    return try {
        val date = LocalDate.of(year.toDigitsInt(), month.toDigitsInt(), day.toDigitsInt())
        usageKey(packageName, usageDate(date))
    } catch (e: DateTimeException) {
        key
    }
}

/**
 * Normalises every key with [normaliseUsageKey], adding up rows that end up with the same key
 * (the same app on the same day, written before and after a language change)
 *
 * @param rows Pairs of usage key and total time
 */
internal fun normaliseUsageKeys(rows: List<Pair<String, Long>>): Map<String, Long> =
    rows.groupingBy { (key, _) -> normaliseUsageKey(key) }
        .fold(0L) { total, (_, time) -> total + time }

private fun String.toDigitsInt(): Int = fold(0) { number, char -> number * 10 + Character.digit(char, 10) }

/**
 * Groups usage rows by which of [dates] they belong to. Every date is present in the result,
 * with an empty list if nothing was used that day. Rows for other dates are ignored.
 *
 * @param rows Pairs of usage key and total time
 */
internal fun groupUsageByDate(rows: List<Pair<String, Long>>, dates: List<String>): Map<String, List<AppUsage>> =
    dates.associateWith { date ->
        val suffix = "-$date"
        rows.filter { (key, _) -> key.endsWith(suffix) && key.length > suffix.length }
            .map { (key, time) -> AppUsage(packageName = key.removeSuffix(suffix), totalTime = time) }
            .sortedByDescending { it.totalTime }
    }

/**
 * The usage keys that do not belong to any of [keepDates]
 */
internal fun usageKeysToDelete(keys: List<String>, keepDates: List<String>): List<String> {
    val suffixes = keepDates.map { "-$it" }
    return keys.filter { key -> suffixes.none { key.endsWith(it) } }
}
