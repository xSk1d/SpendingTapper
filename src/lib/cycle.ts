import {
  addMonths,
  differenceInCalendarDays,
  getDaysInMonth,
  setDate,
  startOfDay,
  subMonths,
} from 'date-fns'

/**
 * A half-open window [start, end) covering one budget month.
 *
 * The cycle need not start on the 1st: someone paid on the 25th can set the cycle
 * to begin then. Months are not all the same length, so a start day of 31 clamps
 * to the last day of any shorter month.
 */
export type BudgetCycle = {
  start: Date
  /** Exclusive. The first instant of the next cycle. */
  end: Date
}

/** The [day]th of this date's month, or the last day if that month is shorter. */
function anchor(date: Date, day: number): Date {
  return startOfDay(setDate(date, Math.min(day, getDaysInMonth(date))))
}

/** The cycle containing [date]. */
export function cycleContaining(date: Date, cycleStartDay: number): BudgetCycle {
  const day = Math.min(31, Math.max(1, Math.trunc(cycleStartDay)))
  const thisMonthStart = anchor(date, day)
  // Before this month's anchor, the live cycle is the one that opened last month.
  const start = startOfDay(date) < thisMonthStart ? anchor(subMonths(date, 1), day) : thisMonthStart
  return { start, end: anchor(addMonths(start, 1), day) }
}

export function currentCycle(now: Date, cycleStartDay: number): BudgetCycle {
  return cycleContaining(now, cycleStartDay)
}

export function cycleContains(cycle: BudgetCycle, epochMillis: number): boolean {
  return epochMillis >= cycle.start.getTime() && epochMillis < cycle.end.getTime()
}

/** Days left including today; at least 1 so callers can divide by it safely.
 *  The end is exclusive, so on the last day of a cycle this is 1, not 0. */
export function daysRemaining(cycle: BudgetCycle, now: Date): number {
  return Math.max(1, differenceInCalendarDays(cycle.end, startOfDay(now)))
}

/** Total spent inside the cycle, in cents. */
export function spentInCycle(
  expenses: { amountCents: number; occurredAt: number }[],
  cycle: BudgetCycle,
): number {
  let total = 0
  for (const expense of expenses) {
    if (cycleContains(cycle, expense.occurredAt)) total += expense.amountCents
  }
  return total
}
