import { useMemo } from 'react'
import { useNavigate } from 'react-router'
import { format as formatDate, isToday, isYesterday } from 'date-fns'
import { format as formatMoney, formatGrouped } from '../lib/money'
import { useStore } from '../lib/store'
import type { Expense } from '../lib/types'

type DayGroup = { key: string; date: Date; expenses: Expense[]; totalCents: number }
type MonthGroup = { key: string; date: Date; days: DayGroup[]; totalCents: number }

/** Newest first, grouped by month then by day, each group carrying its own total. */
export function groupByMonthAndDay(expenses: Expense[]): MonthGroup[] {
  const byDay = new Map<string, DayGroup>()
  for (const expense of [...expenses].sort((a, b) => b.occurredAt - a.occurredAt)) {
    const date = new Date(expense.occurredAt)
    const key = formatDate(date, 'yyyy-MM-dd')
    const group = byDay.get(key) ?? { key, date, expenses: [], totalCents: 0 }
    group.expenses.push(expense)
    group.totalCents += expense.amountCents
    byDay.set(key, group)
  }

  const byMonth = new Map<string, MonthGroup>()
  for (const day of byDay.values()) {
    const key = formatDate(day.date, 'yyyy-MM')
    const group = byMonth.get(key) ?? { key, date: day.date, days: [], totalCents: 0 }
    group.days.push(day)
    group.totalCents += day.totalCents
    byMonth.set(key, group)
  }
  return [...byMonth.values()]
}

function dayLabel(date: Date): string {
  if (isToday(date)) return 'Today'
  if (isYesterday(date)) return 'Yesterday'
  return formatDate(date, 'EEE d MMM')
}

export default function HistoryPage() {
  const navigate = useNavigate()
  const expenses = useStore((s) => s.expenses)
  const symbol = useStore((s) => s.settings.currencySymbol)

  const months = useMemo(() => groupByMonthAndDay(expenses), [expenses])

  return (
    <div className="screen">
      <div className="topbar">
        <button type="button" className="icon-btn" aria-label="Back" onClick={() => navigate('/')}>
          ←
        </button>
        <h1 className="topbar-title">History</h1>
        <span className="topbar-spacer" />
        <button
          type="button"
          className="icon-btn"
          aria-label="Settings"
          onClick={() => navigate('/settings')}
        >
          ⚙
        </button>
      </div>

      {months.length === 0 ? (
        <p className="empty">Nothing logged yet.</p>
      ) : (
        <div className="middle">
          <div className="list">
            {months.map((month) => (
              <section key={month.key}>
                <div className="month">
                  <span className="month-name">{formatDate(month.date, 'MMMM yyyy')}</span>
                  <span className="month-total">{formatGrouped(month.totalCents, symbol)}</span>
                </div>

                {month.days.map((day) => (
                  <div key={day.key}>
                    <div className="day">
                      <span>{dayLabel(day.date)}</span>
                      <span>{formatMoney(day.totalCents, symbol)}</span>
                    </div>
                    {day.expenses.map((expense) => (
                      <button
                        key={expense.id}
                        type="button"
                        className="entry"
                        onClick={() => navigate(`/edit/${expense.id}`)}
                      >
                        <span
                          className={expense.kind === 'NEED' ? 'dot dot-need' : 'dot dot-want'}
                          aria-hidden="true"
                        />
                        <span className="entry-body">
                          <span className="entry-desc">
                            {expense.description === '' ? 'No description' : expense.description}
                          </span>
                          <span className="entry-meta">
                            {formatDate(new Date(expense.occurredAt), 'h:mm a')}
                            {expense.withWho.length > 0 && ` · with ${expense.withWho.join(', ')}`}
                          </span>
                        </span>
                        <span className="entry-amount">
                          {formatMoney(expense.amountCents, symbol)}
                        </span>
                      </button>
                    ))}
                  </div>
                ))}
              </section>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
