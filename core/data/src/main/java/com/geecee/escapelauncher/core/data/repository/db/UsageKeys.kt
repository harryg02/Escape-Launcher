package com.geecee.escapelauncher.core.data.repository.db

import com.geecee.escapelauncher.core.model.AppUsage

/*
 * Usage rows are keyed "<packageName>-<date>". Package names can contain '-' and so can the
 * date, so a key is only ever matched against a known date suffix, never split on '-'.
 */

internal fun usageKey(packageName: String, date: String): String = "$packageName-$date"

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
