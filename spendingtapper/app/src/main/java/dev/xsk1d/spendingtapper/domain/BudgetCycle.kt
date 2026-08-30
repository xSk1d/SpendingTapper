package dev.xsk1d.spendingtapper.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A half-open window [startMillis, endMillis) covering one budget month.
 *
 * The cycle need not start on the 1st: someone paid on the 25th can set the cycle to
 * begin then. Months are not all the same length, so a start day of 31 clamps to the
 * last day of any shorter month.
 */
data class BudgetCycle(
    val startMillis: Long,
    val endMillis: Long,
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
) {
    fun contains(epochMillis: Long): Boolean = epochMillis in startMillis until endMillis

    /** Days remaining including today; at least 1 so callers can divide by it safely. */
    fun daysRemaining(now: Instant, zone: ZoneId): Long {
        val today = now.atZone(zone).toLocalDate()
        return maxOf(1L, ChronoUnit.DAYS.between(today, endDateExclusive))
    }

    companion object {

        /** The cycle containing [now]. */
        fun current(now: Instant, zone: ZoneId, cycleStartDay: Int): BudgetCycle =
            containing(now.atZone(zone).toLocalDate(), zone, cycleStartDay)

        /** The cycle containing [date]. */
        fun containing(date: LocalDate, zone: ZoneId, cycleStartDay: Int): BudgetCycle {
            val day = cycleStartDay.coerceIn(1, 31)
            // The anchor in this calendar month, clamped for short months.
            val thisMonthStart = anchor(date, day)
            val start = if (date < thisMonthStart) anchor(date.minusMonths(1), day) else thisMonthStart
            val end = anchor(start.plusMonths(1), day)
            return BudgetCycle(
                startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
                endMillis = end.atStartOfDay(zone).toInstant().toEpochMilli(),
                startDate = start,
                endDateExclusive = end,
            )
        }

        /** The [day]th of [date]'s month, or the last day if that month is shorter. */
        private fun anchor(date: LocalDate, day: Int): LocalDate =
            date.withDayOfMonth(minOf(day, date.lengthOfMonth()))
    }
}
