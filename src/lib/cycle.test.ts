import { describe, expect, it } from 'vitest'
import { addDays, format } from 'date-fns'
import { cycleContains, cycleContaining, daysRemaining, spentInCycle } from './cycle'

const day = (iso: string) => new Date(`${iso}T12:00:00`)
const asDate = (d: Date) => format(d, 'yyyy-MM-dd')

describe('cycleContaining', () => {
  it('runs 1st to 1st when the cycle starts on the 1st', () => {
    const cycle = cycleContaining(day('2026-03-15'), 1)
    expect(asDate(cycle.start)).toBe('2026-03-01')
    expect(asDate(cycle.end)).toBe('2026-04-01')
  })

  it('opens the cycle on payday when that is mid-month', () => {
    const cycle = cycleContaining(day('2026-03-26'), 25)
    expect(asDate(cycle.start)).toBe('2026-03-25')
    expect(asDate(cycle.end)).toBe('2026-04-25')
  })

  it('a date before this month’s anchor belongs to the cycle that opened last month', () => {
    const cycle = cycleContaining(day('2026-03-24'), 25)
    expect(asDate(cycle.start)).toBe('2026-02-25')
    expect(asDate(cycle.end)).toBe('2026-03-25')
  })

  it('the anchor day itself starts the new cycle, not the old one', () => {
    const cycle = cycleContaining(day('2026-03-25'), 25)
    expect(asDate(cycle.start)).toBe('2026-03-25')
  })

  it('clamps a 31st start to the last day of a short month', () => {
    const cycle = cycleContaining(day('2026-02-15'), 31)
    expect(asDate(cycle.start)).toBe('2026-01-31')
    expect(asDate(cycle.end)).toBe('2026-02-28')
  })

  it('rolls over the year end', () => {
    const cycle = cycleContaining(day('2026-12-30'), 25)
    expect(asDate(cycle.start)).toBe('2026-12-25')
    expect(asDate(cycle.end)).toBe('2027-01-25')
  })

  it('coerces a nonsense start day into range rather than throwing', () => {
    // 0 clamps up to the 1st; 99 clamps down to 31, which in March means the
    // live cycle is the one that opened on the last day of February.
    expect(asDate(cycleContaining(day('2026-03-15'), 0).start)).toBe('2026-03-01')
    expect(asDate(cycleContaining(day('2026-03-15'), 99).start)).toBe('2026-02-28')
  })
})

// The bug this guards against is a day that belongs to no cycle at all, which
// would silently drop spending out of every total.
describe('a full year, swept day by day', () => {
  for (const startDay of [1, 25, 31]) {
    it(`consecutive cycles abut with no gap or overlap (start day ${startDay})`, () => {
      let date = day('2026-01-01')
      const last = day('2027-01-01')
      while (date < last) {
        const cycle = cycleContaining(date, startDay)
        expect(cycleContains(cycle, date.getTime())).toBe(true)

        // The cycle starting the instant this one ends must be the very next one.
        const next = cycleContaining(cycle.end, startDay)
        expect(next.start.getTime()).toBe(cycle.end.getTime())

        date = addDays(date, 1)
      }
    })
  }
})

describe('daysRemaining', () => {
  it('counts today in, and never returns zero', () => {
    const cycle = cycleContaining(day('2026-03-15'), 1)
    expect(daysRemaining(cycle, day('2026-03-31'))).toBe(1)
    expect(daysRemaining(cycle, day('2026-03-30'))).toBe(2)
  })
})

describe('spentInCycle', () => {
  it('adds up only what falls inside the window', () => {
    const cycle = cycleContaining(day('2026-03-15'), 1)
    const spent = spentInCycle(
      [
        { amountCents: 1000, occurredAt: day('2026-03-02').getTime() },
        { amountCents: 250, occurredAt: day('2026-03-28').getTime() },
        { amountCents: 9999, occurredAt: day('2026-04-02').getTime() },
        { amountCents: 8888, occurredAt: day('2026-02-27').getTime() },
      ],
      cycle,
    )
    expect(spent).toBe(1250)
  })
})
