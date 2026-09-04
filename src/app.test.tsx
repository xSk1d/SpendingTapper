import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import {
  NOW,
  SETTINGS,
  makeExpense,
  renderApp,
  seedStore,
  setupUser,
  storeState,
  stubConfirm,
} from './test/helpers'

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true })
  vi.setSystemTime(NOW)
  seedStore()
})

afterEach(() => {
  vi.useRealTimers()
})

/** The pad fills in from the right, so "1250" is $12.50. */
async function tapAmount(user: ReturnType<typeof setupUser>, digits: string) {
  for (const digit of digits) {
    await user.click(screen.getByRole('button', { name: digit }))
  }
}

describe('the entry screen', () => {
  it('opens straight onto the keypad, with no home screen in the way', () => {
    renderApp()
    expect(screen.getByRole('button', { name: '7' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument()
  })

  it('will not save nothing', () => {
    renderApp()
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('saves what was tapped in', async () => {
    const user = setupUser()
    renderApp()

    await tapAmount(user, '1250')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    const { expenses } = storeState()
    expect(expenses).toHaveLength(1)
    expect(expenses[0]?.amountCents).toBe(1250)
    expect(expenses[0]?.kind).toBe('NEED')
  })

  it('clears the form after saving, ready for the next entry', async () => {
    const user = setupUser()
    renderApp()

    await tapAmount(user, '500')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('records a want when the toggle is switched', async () => {
    const user = setupUser()
    renderApp()

    await user.click(screen.getByRole('button', { name: /Want/ }))
    await tapAmount(user, '900')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(storeState().expenses[0]?.kind).toBe('WANT')
  })

  it('marks the selected kind as pressed, so it is not colour alone', async () => {
    const user = setupUser()
    renderApp()

    expect(screen.getByRole('button', { name: /Need/ })).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByRole('button', { name: /Want/ }))
    expect(screen.getByRole('button', { name: /Want/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: /Need/ })).toHaveAttribute('aria-pressed', 'false')
  })

  it('backspaces the last digit', async () => {
    const user = setupUser()
    renderApp()

    await tapAmount(user, '1234')
    await user.click(screen.getByRole('button', { name: 'Delete last digit' }))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(storeState().expenses[0]?.amountCents).toBe(123)
  })
})

describe('the budget line', () => {
  it('offers a way to set the budget when there is none', async () => {
    const user = setupUser()
    renderApp()

    await user.click(screen.getByRole('button', { name: /No budget set/ }))
    expect(screen.getByLabelText('Monthly budget')).toBeInTheDocument()
  })

  it('counts down as an amount is tapped in', async () => {
    const user = setupUser()
    seedStore({ settings: { monthlyBudgetCents: 50000, cycleStartDay: 1, currencySymbol: '$' } })
    renderApp()

    expect(screen.getByText('$500.00 left this month')).toBeInTheDocument()
    await tapAmount(user, '1250')
    expect(screen.getByText('$487.50 left this month')).toBeInTheDocument()
  })

  it('says so when the entry would go over', async () => {
    const user = setupUser()
    seedStore({ settings: { monthlyBudgetCents: 1000, cycleStartDay: 1, currencySymbol: '$' } })
    renderApp()

    await tapAmount(user, '1500')
    expect(screen.getByText('$5.00 over budget')).toBeInTheDocument()
  })

  it('ignores spending from a previous cycle', () => {
    seedStore({
      settings: { monthlyBudgetCents: 50000, cycleStartDay: 1, currencySymbol: '$' },
      expenses: [makeExpense({ occurredAt: new Date(2026, 1, 10).getTime(), amountCents: 30000 })],
    })
    renderApp()

    expect(screen.getByText('$500.00 left this month')).toBeInTheDocument()
  })
})

describe('history', () => {
  it('groups by day with a running total, and can delete an entry', async () => {
    const user = setupUser()
    const confirm = stubConfirm(true)
    seedStore({
      expenses: [
        makeExpense({ id: 'a', amountCents: 1000, description: 'coffee' }),
        makeExpense({ id: 'b', amountCents: 250, description: 'bus' }),
      ],
    })
    const { container } = renderApp('/history')

    expect(screen.getByText('March 2026')).toBeInTheDocument()
    // Scoped to the list: the cycle summary above it shows the same total again.
    const list = container.querySelector('.list') as HTMLElement
    // Both entries fall on one day, so the day total and the month total agree.
    expect(within(list).getAllByText('$12.50')).toHaveLength(2)

    await user.click(screen.getByRole('button', { name: /coffee/ }))
    await user.click(screen.getByRole('button', { name: 'Delete' }))

    expect(confirm).toHaveBeenCalled()
    expect(storeState().expenses.map((e) => e.id)).toEqual(['b'])
  })

  it('says so when there is nothing logged', () => {
    renderApp('/history')
    expect(screen.getByText('Nothing logged yet.')).toBeInTheDocument()
  })
})

describe('settings', () => {
  it('stores a typed budget as integer cents', async () => {
    const user = setupUser()
    renderApp('/settings')

    await user.type(screen.getByLabelText('Monthly budget'), '750.50')
    expect(storeState().settings.monthlyBudgetCents).toBe(75050)
  })

  it('clamps the cycle start day into 1-31', async () => {
    const user = setupUser()
    renderApp('/settings')

    const field = screen.getByLabelText('Cycle start day')
    await user.clear(field)
    await user.type(field, '99')
    expect(storeState().settings.cycleStartDay).toBeLessThanOrEqual(31)
  })
})

describe('with who', () => {
  it('offers names used before and toggles them on the entry', async () => {
    const user = setupUser()
    seedStore({ expenses: [makeExpense({ withWho: ['Sam'] })] })
    renderApp()

    const chip = screen.getByRole('button', { name: /Sam/ })
    expect(chip).toHaveAttribute('aria-pressed', 'false')

    await user.click(chip)
    expect(screen.getByRole('button', { name: /Sam/ })).toHaveAttribute('aria-pressed', 'true')

    await tapAmount(user, '400')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    const saved = storeState().expenses.find((e) => e.amountCents === 400)
    expect(saved?.withWho).toEqual(['Sam'])
  })

  it('adds a new name from the text field', async () => {
    const user = setupUser()
    renderApp()

    await user.type(screen.getByLabelText('Add someone'), 'Alex{Enter}')
    // Typing a name both creates the chip and puts it on this entry.
    expect(screen.getByRole('button', { name: /Alex/ })).toHaveAttribute('aria-pressed', 'true')
  })
})

describe('the budget meter', () => {
  it('draws the split by kind and grows as an amount is tapped in', async () => {
    const user = setupUser()
    seedStore({
      settings: { ...SETTINGS, monthlyBudgetCents: 100_00 },
      expenses: [
        makeExpense({ id: 'a', amountCents: 2000, kind: 'NEED' }),
        makeExpense({ id: 'b', amountCents: 3000, kind: 'WANT' }),
      ],
    })
    const { container } = renderApp()

    const need = container.querySelector('.meter-need') as HTMLElement
    const want = container.querySelector('.meter-want') as HTMLElement
    expect(need.style.width).toBe('20%')
    expect(want.style.width).toBe('30%')

    // Nothing on the keypad yet, so there is no provisional segment to draw.
    expect(container.querySelector('.meter-pending-need')).toBeNull()

    await user.click(screen.getByRole('button', { name: '1' }))
    await user.click(screen.getByRole('button', { name: '0' }))
    await user.click(screen.getByRole('button', { name: '00' }))

    const pending = container.querySelector('.meter-pending-need') as HTMLElement
    expect(pending.style.width).toBe('10%')
    // The committed segments keep their share — the budget is still the yardstick.
    expect((container.querySelector('.meter-need') as HTMLElement).style.width).toBe('20%')
  })

  it('rescales to the spend and marks the limit once over budget', () => {
    seedStore({
      settings: { ...SETTINGS, monthlyBudgetCents: 100_00 },
      expenses: [makeExpense({ amountCents: 150_00, kind: 'NEED' })],
    })
    const { container } = renderApp()

    // The bar now measures $150, so the budget notch sits two thirds along it.
    expect((container.querySelector('.meter-need') as HTMLElement).style.width).toBe('100%')
    expect((container.querySelector('.meter-limit') as HTMLElement).style.left).toBe('66.6667%')
    expect(screen.getByText('$50.00 over budget')).toBeInTheDocument()
  })

  it('names both kinds in the legend rather than relying on colour', () => {
    seedStore({
      settings: { ...SETTINGS, monthlyBudgetCents: 100_00 },
      expenses: [
        makeExpense({ id: 'a', amountCents: 2000, kind: 'NEED' }),
        makeExpense({ id: 'b', amountCents: 3000, kind: 'WANT' }),
      ],
    })
    const { container } = renderApp()

    const legend = container.querySelector('.legend') as HTMLElement
    expect(within(legend).getByText('Needs')).toBeInTheDocument()
    expect(within(legend).getByText('$20.00')).toBeInTheDocument()
    expect(within(legend).getByText('Wants')).toBeInTheDocument()
    expect(within(legend).getByText('$30.00')).toBeInTheDocument()
  })

  it('is not drawn at all until a budget exists', () => {
    seedStore({ expenses: [makeExpense()] })
    const { container } = renderApp()

    expect(container.querySelector('.meter')).toBeNull()
    expect(screen.getByRole('button', { name: /No budget set/ })).toBeInTheDocument()
  })
})

describe('the cycle summary', () => {
  const CYCLE = [
    makeExpense({
      id: 'a',
      amountCents: 2000,
      kind: 'NEED',
      occurredAt: new Date(2026, 2, 3, 9).getTime(),
    }),
    makeExpense({
      id: 'b',
      amountCents: 3000,
      kind: 'WANT',
      occurredAt: new Date(2026, 2, 9, 19).getTime(),
    }),
    makeExpense({
      id: 'c',
      amountCents: 1000,
      kind: 'NEED',
      occurredAt: new Date(2026, 2, 9, 21).getTime(),
    }),
  ]

  it('leads with what is left and breaks it down', () => {
    seedStore({ settings: { ...SETTINGS, monthlyBudgetCents: 100_00 }, expenses: CYCLE })
    const { container } = renderApp('/history')

    expect(screen.getByText('Left to spend')).toBeInTheDocument()
    expect((container.querySelector('.hero') as HTMLElement).textContent).toBe('$40.00')
    expect(screen.getByText('$60.00 of $100.00 spent')).toBeInTheDocument()
    expect(screen.getByText('1 Mar – 31 Mar')).toBeInTheDocument()
  })

  it('draws one column per elapsed day, scaled to the busiest', () => {
    seedStore({ settings: { ...SETTINGS, monthlyBudgetCents: 100_00 }, expenses: CYCLE })
    const { container } = renderApp('/history')

    // 1-15 March inclusive; the rest of the month has not happened yet.
    expect(container.querySelectorAll('.col')).toHaveLength(15)

    const columns = [...container.querySelectorAll('.col')]
    // 9 March is the peak at $40, so its two segments fill the plot between them.
    const peak = columns[8]
    expect((peak.querySelector('.col-want') as HTMLElement).style.height).toBe('75%')
    expect((peak.querySelector('.col-need') as HTMLElement).style.height).toBe('25%')
    // 3 March is needs only, so it gets the rounded cap and no want segment.
    expect(columns[2].querySelector('.col-want')).toBeNull()
    expect(columns[2].querySelector('.col-need')?.className).toContain('col-cap')
    expect(columns[3].querySelectorAll('.col-seg')).toHaveLength(0)

    expect(screen.getByText(/Busiest Mon 9 · \$40\.00/)).toBeInTheDocument()
  })

  it('carries every value in a table as well as the chart', async () => {
    const user = setupUser()
    seedStore({ settings: { ...SETTINGS, monthlyBudgetCents: 100_00 }, expenses: CYCLE })
    const { container } = renderApp('/history')

    await user.click(screen.getByText('Show the numbers'))

    const rows = [...container.querySelectorAll('.numbers tbody tr')]
    expect(rows).toHaveLength(2)
    expect(rows[0].textContent).toBe('Tue 3 Mar$20.00$0.00$20.00')
    expect(rows[1].textContent).toBe('Mon 9 Mar$10.00$30.00$40.00')
  })

  it('shows the spend instead of a remainder when no budget is set', () => {
    seedStore({ expenses: CYCLE })
    const { container } = renderApp('/history')

    expect(screen.getByText('Spent this cycle')).toBeInTheDocument()
    expect((container.querySelector('.hero') as HTMLElement).textContent).toBe('$60.00')
    expect(container.querySelector('.card-sub')).toBeNull()
  })
})
