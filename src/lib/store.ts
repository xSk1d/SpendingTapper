import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { dedupePeople } from './people'
import type { AppData, Expense, Settings } from './types'

export const SCHEMA_VERSION = 1
const STORAGE_KEY = 'spendingtapper'

export function uid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

export function nowISO(): string {
  return new Date().toISOString()
}

export function defaultSettings(): Settings {
  return { monthlyBudgetCents: 0, cycleStartDay: 1, currencySymbol: '$' }
}

/**
 * Nothing to migrate from yet — v1 is the first web schema. It exists so the first
 * real bump has somewhere to hook in, and so a persisted blob from a future version
 * is passed through rather than silently reshaped.
 */
export function migrateState(persisted: unknown, version: number): AppData {
  if (version >= SCHEMA_VERSION) return persisted as AppData
  const state = (persisted ?? {}) as { expenses?: Expense[]; settings?: Partial<Settings> }
  return {
    expenses: state.expenses ?? [],
    settings: { ...defaultSettings(), ...state.settings },
  }
}

export type ExpenseInput = Omit<Expense, 'id' | 'createdAt'>

type Store = AppData & {
  addExpense: (input: ExpenseInput) => string
  updateExpense: (id: string, patch: Partial<ExpenseInput>) => void
  deleteExpense: (id: string) => void
  /** Import merges rather than replaces, so a backup can be folded into live data. */
  addImported: (expenses: Expense[]) => void
  updateSettings: (patch: Partial<Settings>) => void
  replaceAll: (data: AppData) => void
}

export const useStore = create<Store>()(
  persist(
    (set) => ({
      expenses: [],
      settings: defaultSettings(),

      addExpense: (input) => {
        const expense: Expense = {
          ...input,
          withWho: dedupePeople(input.withWho),
          id: uid(),
          createdAt: nowISO(),
        }
        set((s) => ({ expenses: [...s.expenses, expense] }))
        return expense.id
      },

      updateExpense: (id, patch) =>
        set((s) => ({
          expenses: s.expenses.map((expense) =>
            expense.id === id
              ? {
                  ...expense,
                  ...patch,
                  withWho: patch.withWho ? dedupePeople(patch.withWho) : expense.withWho,
                }
              : expense,
          ),
        })),

      deleteExpense: (id) =>
        set((s) => ({ expenses: s.expenses.filter((expense) => expense.id !== id) })),

      addImported: (expenses) => set((s) => ({ expenses: [...s.expenses, ...expenses] })),

      updateSettings: (patch) => set((s) => ({ settings: { ...s.settings, ...patch } })),

      replaceAll: (data) =>
        set({
          expenses: data.expenses,
          settings: { ...defaultSettings(), ...data.settings },
        }),
    }),
    {
      name: STORAGE_KEY,
      version: SCHEMA_VERSION,
      partialize: (s) => ({ expenses: s.expenses, settings: s.settings }),
      migrate: migrateState,
    },
  ),
)

export function hasBudget(settings: Settings): boolean {
  return settings.monthlyBudgetCents > 0
}

/** Names used recently, most recent first — what the "with who" chips offer. */
export function recentPeople(expenses: Expense[], limit = 8): string[] {
  const byRecency = [...expenses].sort((a, b) => b.occurredAt - a.occurredAt)
  return dedupePeople(byRecency.flatMap((expense) => expense.withWho)).slice(0, limit)
}
