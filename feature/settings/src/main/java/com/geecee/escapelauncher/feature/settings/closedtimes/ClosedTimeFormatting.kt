package com.geecee.escapelauncher.feature.settings.closedtimes

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.geecee.escapelauncher.core.model.ClosedPeriod
import com.geecee.escapelauncher.core.ui.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

private val weekdays = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)
private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/**
 * The days of the week, starting on the first day of the week for the user's locale
 */
internal fun orderedDays(): List<DayOfWeek> {
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return (0 until 7).map { first.plus(it.toLong()) }
}

internal fun shortDayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

/**
 * Minutes after midnight as a time in the user's 12/24 hour format
 */
internal fun formatMinuteOfDay(context: Context, minuteOfDay: Int): String {
    val dateTime = LocalDate.now().atTime(minuteOfDay / 60, minuteOfDay % 60)
    val date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
    return DateFormat.getTimeFormat(context).format(date)
}

@Composable
internal fun describeDays(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeek.values().toSet() -> stringResource(R.string.every_day)
    weekdays -> stringResource(R.string.weekdays)
    weekend -> stringResource(R.string.weekends)
    else -> orderedDays().filter { it in days }.joinToString(", ") { shortDayName(it) }
}

/**
 * One line summary such as "Weekdays, 22:00 to 07:00"
 */
@Composable
internal fun describePeriod(period: ClosedPeriod): String {
    val context = LocalContext.current
    return stringResource(
        R.string.closed_time_summary,
        describeDays(period.days),
        formatMinuteOfDay(context, period.startMinute),
        formatMinuteOfDay(context, period.endMinute)
    )
}
