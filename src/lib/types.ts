/** Whether the spend was something you had to do or something you chose to do.
 *  Stored uppercase because that is what the CSV format already carries. */
export type Kind = 'NEED' | 'WANT'

export type Expense = {
  id: string
  /** Integer cents. Never a float — see lib/money.ts for why. */
  amountCents: number
  kind: Kind
  description: string
  /** Epoch milliseconds. When the money was actually spent, not when it was logged. */
  occurredAt: number
  /** Who you were with. Empty when it was just you. */
  withWho: string[]
  createdAt: string
}

export type Settings = {
  /** Integer cents. 0 means no budget has been set yet. */
  monthlyBudgetCents: number
  /** Day of the month the budget rolls over, 1–31. A 31 clamps to the last day
   *  of any shorter month. Set it to your payday. */
  cycleStartDay: number
  /** Whatever marker you want in front of the numbers. Not a currency code —
   *  no conversion is ever done, this is display only. */
  currencySymbol: string
}

export type AppData = {
  expenses: Expense[]
  settings: Settings
}

/** Shape written by "Export backup". Versioned so a future import can migrate. */
export type Backup = AppData & {
  app: 'spendingtapper'
  version: number
  exportedAt: string
}
