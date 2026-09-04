import { describe, expect, it } from 'vitest'
import { busiestDay, meterScale, share, summariseCycle } from './summary'
import { defaultSettings } from './store'
import type { Expense, Settings } from './types'

const NOW = new Date(2026, 2, 15, 12, 0, 0) // Sun 15 March 2026, midday

function settings(over: Partial<Settings> = {}): Settings {
  return { ...defaultSettings(), monthlyBudgetCents: 50_000, ...over }
}

function expense(over: Partial<Expense> = {}): Expense {
  return {
    id: Math.random().toString(36).slice(2),
    amountCents: 1000,
    kind: 'NEED',
    description: '',
    occurredAt: new Date(2026, 2, 10, 9, 0).getTime(),
    withWho: [],
    createdAt: '',
    ...over,
  }
}

describe('summariseCycle', () => {
  it('splits the cycle spend by kind', () => {
    const summary = summariseCycle(
      [
        expense({ amountCents: 1200, kind: 'NEED' }),
        expense({ amountCents: 800, kind: 'NEED' }),
        expense({ amountCents: 4800, kind: 'WANT' }),
      ],
      settings(),
      NOW,
    )
    expect(summary.needCents).toBe(2000)
    expect(summary.wantCents).toBe(4800)
    expect(summary.spentCents).toBe(6800)
    expect(summary.remainingCents).toBe(43_200)
  })

  it('ignores spend outside the cycle', () => {
    const summary = summariseCycle(
      [
        expense({ amountCents: 1000, occurredAt: new Date(2026, 1, 27).getTime() }),
        expense({ amountCents: 2000, occurredAt: new Date(2026, 2, 2).getTime() }),
      ],
      settings(),
      NOW,
    )
    expect(summary.spentCents).toBe(2000)
  })

  it('follows a cycle that does not start on the 1st', () => {
    // Paid on the 25th: on 15 March the live cycle opened on 25 February.
    const summary = summariseCycle(
      [expense({ amountCents: 3000, occurredAt: new Date(2026, 1, 27).getTime() })],
      settings({ cycleStartDay: 25 }),
      NOW,
    )
    expect(summary.cycle.start).toEqual(new Date(2026, 1, 25))
    expect(summary.spentCents).toBe(3000)
  })

  it('folds a pending amount into the spend without touching either kind', () => {
    const summary = summariseCycle([expense({ amountCents: 1000 })], settings(), NOW, 2500)
    expect(summary.needCents).toBe(1000)
    expect(summary.wantCents).toBe(0)
    expect(summary.spentCents).toBe(3500)
    expect(summary.remainingCents).toBe(46_500)
  })

  it('goes negative once the budget is blown', () => {
    const summary = summariseCycle(
      [expense({ amountCents: 60_000 })],
      settings({ monthlyBudgetCents: 50_000 }),
      NOW,
    )
    expect(summary.remainingCents).toBe(-10_000)
  })

  it('buckets days from the cycle start through today, never past it', () => {
    const summary = summariseCycle([], settings(), NOW)
    expect(summary.days).toHaveLength(15)
    expect(summary.days[0].date).toEqual(new Date(2026, 2, 1))
    expect(summary.days[14].date).toEqual(new Date(2026, 2, 15))
  })

  it('puts each entry in its own day, split by kind', () => {
    const summary = summariseCycle(
      [
        expense({ amountCents: 1000, kind: 'NEED', occurredAt: new Date(2026, 2, 3, 8).getTime() }),
        expense({ amountCents: 500, kind: 'WANT', occurredAt: new Date(2026, 2, 3, 20).getTime() }),
        expense({ amountCents: 700, kind: 'WANT', occurredAt: new Date(2026, 2, 9).getTime() }),
      ],
      settings(),
      NOW,
    )
    const third = summary.days.find((d) => d.date.getDate() === 3)
    expect(third).toMatchObject({ needCents: 1000, wantCents: 500, totalCents: 1500 })
    expect(summary.days.find((d) => d.date.getDate() === 9)?.wantCents).toBe(700)
    expect(summary.days.find((d) => d.date.getDate() === 4)?.totalCents).toBe(0)
  })

  it('still counts an entry dated later in the cycle, even with no column for it', () => {
    const summary = summariseCycle(
      [expense({ amountCents: 2500, occurredAt: new Date(2026, 2, 20).getTime() })],
      settings(),
      NOW,
    )
    expect(summary.spentCents).toBe(2500)
    expect(summary.days.some((d) => d.date.getDate() === 20)).toBe(false)
    expect(summary.days.reduce((sum, d) => sum + d.totalCents, 0)).toBe(0)
  })
})

describe('meterScale', () => {
  it('measures against the budget while there is budget left', () => {
    const summary = summariseCycle([expense({ amountCents: 10_000 })], settings(), NOW)
    const { total, budgetFraction } = meterScale(summary)
    expect(total).toBe(50_000)
    expect(budgetFraction).toBe(1)
  })

  it('measures against the spend once over, so the overspend has room to draw', () => {
    const summary = summariseCycle([expense({ amountCents: 75_000 })], settings(), NOW)
    const { total, budgetFraction } = meterScale(summary)
    expect(total).toBe(75_000)
    expect(budgetFraction).toBeCloseTo(2 / 3)
  })

  it('never divides by zero when nothing is set and nothing is spent', () => {
    const summary = summariseCycle([], settings({ monthlyBudgetCents: 0 }), NOW)
    expect(meterScale(summary).total).toBe(1)
  })
})

describe('share', () => {
  it('turns cents into a CSS width', () => {
    expect(share(2500, 10_000)).toBe('25%')
    expect(share(0, 10_000)).toBe('0%')
  })

  it('clamps rather than overflowing its track', () => {
    expect(share(12_000, 10_000)).toBe('100%')
  })
})

describe('busiestDay', () => {
  it('picks the highest-spending day and ignores empty ones', () => {
    const summary = summariseCycle(
      [
        expense({ amountCents: 1000, occurredAt: new Date(2026, 2, 3).getTime() }),
        expense({ amountCents: 8600, occurredAt: new Date(2026, 2, 7).getTime() }),
      ],
      settings(),
      NOW,
    )
    expect(busiestDay(summary.days)?.date.getDate()).toBe(7)
  })

  it('is null when nothing has been spent', () => {
    expect(busiestDay(summariseCycle([], settings(), NOW).days)).toBeNull()
  })
})
