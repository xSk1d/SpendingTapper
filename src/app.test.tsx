import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import {
  NOW,
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
    renderApp('/history')

    expect(screen.getByText('March 2026')).toBeInTheDocument()
    // Both entries fall on one day, so the day total and the month total agree.
    expect(screen.getAllByText('$12.50')).toHaveLength(2)

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
