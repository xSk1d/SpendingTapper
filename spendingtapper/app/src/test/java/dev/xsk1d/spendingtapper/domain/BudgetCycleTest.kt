package dev.xsk1d.spendingtapper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BudgetCycleTest {

    private val zone: ZoneId = ZoneId.of("America/Toronto")

    private fun cycleOn(date: String, startDay: Int) =
        BudgetCycle.containing(LocalDate.parse(date), zone, startDay)

    private fun millisAt(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `a cycle starting on the 1st is the calendar month`() {
        val cycle = cycleOn("2026-03-15", startDay = 1)
        assertEquals(LocalDate.parse("2026-03-01"), cycle.startDate)
        assertEquals(LocalDate.parse("2026-04-01"), cycle.endDateExclusive)
    }

    @Test
    fun `the first and last instants of the month fall inside the cycle`() {
        val cycle = cycleOn("2026-03-15", startDay = 1)
        assertTrue(cycle.contains(millisAt("2026-03-01T00:00:00")))
        assertTrue(cycle.contains(millisAt("2026-03-31T23:59:59")))
        // The window is half-open, so midnight on the 1st belongs to the next cycle.
        assertFalse(cycle.contains(millisAt("2026-04-01T00:00:00")))
        assertFalse(cycle.contains(millisAt("2026-02-28T23:59:59")))
    }

    @Test
    fun `a mid-month start day straddles two calendar months`() {
        // Paid on the 25th: the cycle for the 3rd of March began on 25 February.
        val cycle = cycleOn("2026-03-03", startDay = 25)
        assertEquals(LocalDate.parse("2026-02-25"), cycle.startDate)
        assertEquals(LocalDate.parse("2026-03-25"), cycle.endDateExclusive)
        assertTrue(cycle.contains(millisAt("2026-03-03T12:00:00")))
    }

    @Test
    fun `on the start day itself the new cycle has already begun`() {
        val cycle = cycleOn("2026-03-25", startDay = 25)
        assertEquals(LocalDate.parse("2026-03-25"), cycle.startDate)
        assertEquals(LocalDate.parse("2026-04-25"), cycle.endDateExclusive)
    }

    @Test
    fun `a start day of 31 clamps to the last day of a short month`() {
        // February has no 31st, so the cycle anchors on the 28th.
        val february = cycleOn("2026-02-10", startDay = 31)
        assertEquals(LocalDate.parse("2026-01-31"), february.startDate)
        assertEquals(LocalDate.parse("2026-02-28"), february.endDateExclusive)

        val afterClamp = cycleOn("2026-03-01", startDay = 31)
        assertEquals(LocalDate.parse("2026-02-28"), afterClamp.startDate)
        assertEquals(LocalDate.parse("2026-03-31"), afterClamp.endDateExclusive)
    }

    @Test
    fun `a leap February clamps to the 29th`() {
        val cycle = cycleOn("2028-02-15", startDay = 31)
        assertEquals(LocalDate.parse("2028-01-31"), cycle.startDate)
        assertEquals(LocalDate.parse("2028-02-29"), cycle.endDateExclusive)
    }

    @Test
    fun `consecutive cycles meet exactly with no gap and no overlap`() {
        // Every day of a year must belong to exactly one cycle, or spending goes missing.
        for (startDay in listOf(1, 15, 28, 31)) {
            var date = LocalDate.parse("2026-01-01")
            var cycle = BudgetCycle.containing(date, zone, startDay)
            while (date < LocalDate.parse("2027-01-01")) {
                val current = BudgetCycle.containing(date, zone, startDay)
                if (current.startDate != cycle.startDate) {
                    assertEquals(
                        "cycles must abut for start day $startDay at $date",
                        cycle.endDateExclusive,
                        current.startDate,
                    )
                    cycle = current
                }
                assertTrue(
                    "$date must fall inside its own cycle (start day $startDay)",
                    current.contains(millisAt("${date}T12:00:00")),
                )
                date = date.plusDays(1)
            }
        }
    }

    @Test
    fun `the cycle rolls over across a year boundary`() {
        val cycle = cycleOn("2026-01-05", startDay = 20)
        assertEquals(LocalDate.parse("2025-12-20"), cycle.startDate)
        assertEquals(LocalDate.parse("2026-01-20"), cycle.endDateExclusive)
    }

    @Test
    fun `days remaining counts to the end of the cycle and never returns zero`() {
        val cycle = cycleOn("2026-03-15", startDay = 1)
        val now = LocalDateTime.parse("2026-03-15T09:00:00").atZone(zone).toInstant()
        assertEquals(17L, cycle.daysRemaining(now, zone))

        val lastDay = LocalDateTime.parse("2026-03-31T23:00:00").atZone(zone).toInstant()
        assertEquals(1L, cycle.daysRemaining(lastDay, zone))
    }

    @Test
    fun `an out of range start day is clamped rather than throwing`() {
        assertEquals(LocalDate.parse("2026-03-01"), cycleOn("2026-03-15", startDay = 0).startDate)
        assertEquals(LocalDate.parse("2026-02-28"), cycleOn("2026-03-15", startDay = 99).startDate)
    }
}
