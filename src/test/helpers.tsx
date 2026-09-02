import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HashRouter } from 'react-router'
import { vi } from 'vitest'
import App from '../App'
import { defaultSettings, useStore } from '../lib/store'
import type { AppData, Expense, Settings } from '../lib/types'

/** Fixed clock for every test: Sunday 15 March 2026, midday local. */
export const NOW = new Date(2026, 2, 15, 12, 0, 0)

export const SETTINGS: Settings = defaultSettings()

export function makeExpense(over: Partial<Expense> = {}): Expense {
  return {
    id: 'e1',
    amountCents: 1234,
    kind: 'NEED',
    description: 'coffee',
    occurredAt: new Date(2026, 2, 15, 9, 30).getTime(),
    withWho: [],
    createdAt: '',
    ...over,
  }
}

/** Wipes persisted state and seeds the store directly, bypassing the UI. */
export function seedStore(data: Partial<AppData> = {}) {
  localStorage.clear()
  useStore.setState({
    expenses: data.expenses ?? [],
    settings: data.settings ?? SETTINGS,
  })
}

export function storeState(): AppData {
  const { expenses, settings } = useStore.getState()
  return { expenses, settings }
}

/**
 * `delay: null` keeps user-event off the timer queue, so it stays compatible with
 * the faked system clock the date-dependent assertions rely on.
 */
export function setupUser() {
  return userEvent.setup({ delay: null })
}

export function renderApp(path = '/') {
  window.location.hash = path
  return render(
    <HashRouter>
      <App />
    </HashRouter>,
  )
}

/** window.confirm is a no-op in jsdom; every destructive path needs it stubbed. */
export function stubConfirm(answer = true) {
  return vi.spyOn(window, 'confirm').mockReturnValue(answer)
}
