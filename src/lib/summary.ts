import { addDays, startOfDay } from 'date-fns'
import { currentCycle, cycleContains, type BudgetCycle } from './cycle'
import type { Expense, Settings } from './types'

/** One day of the cycle, split the way the app splits every entry. */
export type DayBucket = {
  date: Date
  needCents: number
  wantCents: number
  totalCents: number
}

export type CycleSummary = {
  cycle: BudgetCycle
  /** 0 when no budget has been set. `remainingCents` means nothing in that case. */
  budgetCents: number
  needCents: number
  wantCents: number
  spentCents: number
  /** Budget minus spend. Negative once the cycle is over budget. */
  remainingCents: number
  /**
   * Cycle start through today, one bucket per day — never past today. A day that
   * has not happened yet has not been spent nothing on; it is simply unknown, and
   * drawing it as a zero would read as a run of frugal days that were never lived.
   */
  days: DayBucket[]
}

/** Everything the budget visuals need, derived in one pass so they cannot disagree. */
export function summariseCycle(
  expenses: Expense[],
  settings: Settings,
  now: Date,
  /** An amount being typed but not yet saved, folded into neither total. */
  pendingCents = 0,
): CycleSummary {
  const cycle = currentCycle(now, settings.cycleStartDay)

  const buckets = new Map<number, DayBucket>()
  // The cycle is half-open, so the last bucket is the day before `end`. Today caps
  // it in the normal case; a future-dated entry cannot stretch the chart past it.
  const lastDay = Math.min(startOfDay(now).getTime(), addDays(cycle.end, -1).getTime())
  for (let day = new Date(cycle.start); day.getTime() <= lastDay; day = addDays(day, 1)) {
    buckets.set(day.getTime(), { date: new Date(day), needCents: 0, wantCents: 0, totalCents: 0 })
  }

  let needCents = 0
  let wantCents = 0
  for (const expense of expenses) {
    if (!cycleContains(cycle, expense.occurredAt)) continue
    if (expense.kind === 'NEED') needCents += expense.amountCents
    else wantCents += expense.amountCents

    // An entry logged later today, or one dated into the rest of the cycle, still
    // counts against the budget — it just has no column of its own yet.
    const bucket = buckets.get(startOfDay(new Date(expense.occurredAt)).getTime())
    if (!bucket) continue
    if (expense.kind === 'NEED') bucket.needCents += expense.amountCents
    else bucket.wantCents += expense.amountCents
    bucket.totalCents += expense.amountCents
  }

  const spentCents = needCents + wantCents + pendingCents
  return {
    cycle,
    budgetCents: settings.monthlyBudgetCents,
    needCents,
    wantCents,
    spentCents,
    remainingCents: settings.monthlyBudgetCents - spentCents,
    days: [...buckets.values()],
  }
}

/**
 * The width one cent is drawn at, as a fraction of the meter.
 *
 * The denominator is the budget until the budget is blown, and the spend after —
 * so the bar always fills exactly its track and the overspend has somewhere to go
 * rather than being clipped off the end. Callers draw a marker at `budgetFraction`
 * to show where the limit fell.
 */
export function meterScale(summary: CycleSummary): { total: number; budgetFraction: number } {
  const total = Math.max(summary.budgetCents, summary.spentCents, 1)
  return { total, budgetFraction: summary.budgetCents / total }
}

/**
 * A fraction as a CSS percentage, rounded to four places. Full float precision
 * would put a seventeen-digit number in the style attribute for a width nobody
 * can see a thousandth of.
 */
export function pct(fraction: number): string {
  return `${Number((fraction * 100).toFixed(4))}%`
}

/** A segment's share of the meter, as a percentage string ready for CSS. */
export function share(cents: number, total: number): string {
  if (total <= 0 || cents <= 0) return '0%'
  return pct(Math.min(cents, total) / total)
}

/** The busiest day in the cycle — the one column worth direct-labelling. */
export function busiestDay(days: DayBucket[]): DayBucket | null {
  let best: DayBucket | null = null
  for (const day of days) {
    if (day.totalCents > 0 && (best === null || day.totalCents > best.totalCents)) best = day
  }
  return best
}
