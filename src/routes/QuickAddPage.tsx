import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { format as formatDate, isSameDay, isValid, parse as parseDateFns } from 'date-fns'
import Keypad from '../components/Keypad'
import { currentCycle, spentInCycle } from '../lib/cycle'
import { appendDigit, backspace, digitsToCents, formatCents, formatGrouped } from '../lib/money'
import { dedupePeople } from '../lib/people'
import { hasBudget, recentPeople, useStore } from '../lib/store'
import type { Kind } from '../lib/types'
import { closeApp } from '../lib/platform'

const DATE_FMT = 'yyyy-MM-dd'
const TIME_FMT = 'HH:mm'

/** '1250' <- 1250 cents, so editing an entry drops back into the keypad's model. */
function centsToDigits(cents: number): string {
  return cents === 0 ? '' : String(Math.abs(cents))
}

export default function QuickAddPage() {
  const navigate = useNavigate()
  const { id } = useParams()

  const expenses = useStore((s) => s.expenses)
  const settings = useStore((s) => s.settings)
  const addExpense = useStore((s) => s.addExpense)
  const updateExpense = useStore((s) => s.updateExpense)
  const deleteExpense = useStore((s) => s.deleteExpense)

  const editing = useMemo(() => expenses.find((e) => e.id === id), [expenses, id])

  const [digits, setDigits] = useState('')
  const [kind, setKind] = useState<Kind>('NEED')
  const [description, setDescription] = useState('')
  const [people, setPeople] = useState<string[]>([])
  const [newName, setNewName] = useState('')
  const [occurredAt, setOccurredAt] = useState(() => Date.now())
  const [toast, setToast] = useState<string | null>(null)

  // Loading an entry to edit happens once the store has it, not on every render.
  useEffect(() => {
    if (!editing) return
    setDigits(centsToDigits(editing.amountCents))
    setKind(editing.kind)
    setDescription(editing.description)
    setPeople(editing.withWho)
    setOccurredAt(editing.occurredAt)
  }, [editing])

  useEffect(() => {
    if (toast === null) return
    const timer = window.setTimeout(() => setToast(null), 1600)
    return () => clearTimeout(timer)
  }, [toast])

  const amountCents = digitsToCents(digits)
  const canSave = amountCents > 0

  // What will be left after this purchase, counting down as you type. This is the
  // one number worth looking at while entering an amount.
  const projectedLeftCents = useMemo(() => {
    if (!hasBudget(settings)) return 0
    const cycle = currentCycle(new Date(), settings.cycleStartDay)
    const others = editing ? expenses.filter((e) => e.id !== editing.id) : expenses
    return settings.monthlyBudgetCents - spentInCycle(others, cycle) - amountCents
  }, [expenses, settings, amountCents, editing])

  const known = useMemo(
    () => dedupePeople([...people, ...recentPeople(expenses)]),
    [people, expenses],
  )

  const isSelected = (name: string) => people.some((p) => p.toLowerCase() === name.toLowerCase())

  const togglePerson = (name: string) => {
    setPeople((current) =>
      isSelected(name)
        ? current.filter((p) => p.toLowerCase() !== name.toLowerCase())
        : dedupePeople([...current, name]),
    )
  }

  const setDatePart = (value: string) => {
    const parsed = parseDateFns(value, DATE_FMT, new Date(occurredAt))
    if (isValid(parsed)) setOccurredAt(parsed.getTime())
  }

  const setTimePart = (value: string) => {
    const parsed = parseDateFns(value, TIME_FMT, new Date(occurredAt))
    if (isValid(parsed)) setOccurredAt(parsed.getTime())
  }

  const save = () => {
    if (!canSave) return
    const input = {
      amountCents,
      kind,
      description: description.trim(),
      occurredAt,
      withWho: people,
    }

    if (editing) {
      updateExpense(editing.id, input)
      navigate(-1)
      return
    }

    addExpense(input)
    // Saving closes the app so the next back-tap starts clean. On the web there is
    // no app to close, so the form resets in place instead.
    closeApp(() => {
      setDigits('')
      setDescription('')
      setPeople([])
      setOccurredAt(Date.now())
      setToast('Saved')
    })
  }

  const now = new Date()
  const occurred = new Date(occurredAt)
  const isNow = Math.abs(occurredAt - now.getTime()) < 60_000

  return (
    <div className="screen">
      <div className="topbar">
        <button
          type="button"
          className="icon-btn"
          aria-label="Close"
          onClick={() => (editing ? navigate(-1) : closeApp())}
        >
          ✕
        </button>
        <span className="topbar-spacer" />
        {!editing && (
          <button
            type="button"
            className="icon-btn"
            aria-label="History"
            onClick={() => navigate('/history')}
          >
            ☰
          </button>
        )}
        <button
          type="button"
          className="icon-btn"
          aria-label="Settings"
          onClick={() => navigate('/settings')}
        >
          ⚙
        </button>
      </div>

      <div>
        <div className="amount">
          <span className="amount-symbol">{settings.currencySymbol}</span>
          <span className={amountCents === 0 ? 'amount-value amount-zero' : 'amount-value'}>
            {formatCents(amountCents)}
          </span>
        </div>

        {hasBudget(settings) ? (
          <div
            className={
              projectedLeftCents < 0
                ? 'budget-line budget-over'
                : projectedLeftCents < settings.monthlyBudgetCents / 10
                  ? 'budget-line budget-low'
                  : 'budget-line'
            }
          >
            {projectedLeftCents < 0
              ? `${formatGrouped(-projectedLeftCents, settings.currencySymbol)} over budget`
              : `${formatGrouped(projectedLeftCents, settings.currencySymbol)} left this month`}
          </div>
        ) : (
          <button type="button" className="budget-set" onClick={() => navigate('/settings')}>
            No budget set — tap to set one
          </button>
        )}
      </div>

      <div className="segmented" role="group" aria-label="Need or want">
        {(['NEED', 'WANT'] as const).map((entry) => (
          <button
            key={entry}
            type="button"
            className={kind === entry ? 'seg seg-on' : 'seg'}
            aria-pressed={kind === entry}
            onClick={() => setKind(entry)}
          >
            {kind === entry ? '✓ ' : ''}
            {entry === 'NEED' ? 'Need' : 'Want'}
          </button>
        ))}
      </div>

      <div className="middle">
        <div>
          <label className="label" htmlFor="description">
            What for?
          </label>
          <input
            id="description"
            className="input"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div>
          <span className="label">With who</span>
          {known.length > 0 && (
            <div className="chips">
              {known.map((name) => (
                <button
                  key={name}
                  type="button"
                  className={isSelected(name) ? 'chip chip-on' : 'chip'}
                  aria-pressed={isSelected(name)}
                  onClick={() => togglePerson(name)}
                >
                  {isSelected(name) ? '✓ ' : ''}
                  {name}
                </button>
              ))}
            </div>
          )}
          <input
            className="input"
            placeholder="Add someone"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key !== 'Enter' || newName.trim() === '') return
              e.preventDefault()
              togglePerson(newName.trim())
              setNewName('')
            }}
            aria-label="Add someone"
          />
        </div>

        <div className="whenrow">
          <input
            className="input"
            type="date"
            value={formatDate(occurred, DATE_FMT)}
            onChange={(e) => setDatePart(e.target.value)}
            aria-label="Date"
            style={{ width: 'auto', flex: 1 }}
          />
          <input
            className="input"
            type="time"
            value={formatDate(occurred, TIME_FMT)}
            onChange={(e) => setTimePart(e.target.value)}
            aria-label="Time"
            style={{ width: 'auto', flex: 1 }}
          />
          {!isNow && (
            <button type="button" className="linkbtn" onClick={() => setOccurredAt(Date.now())}>
              Now
            </button>
          )}
        </div>

        {!isSameDay(occurred, now) && (
          <div className="setting-hint">Logging against {formatDate(occurred, 'EEE d MMM')}</div>
        )}
      </div>

      <Keypad
        onDigit={(digit) => setDigits((d) => appendDigit(d, digit))}
        onBackspace={() => setDigits((d) => backspace(d))}
        onClear={() => setDigits('')}
      />

      <button type="button" className="primary" disabled={!canSave} onClick={save}>
        {editing ? 'Update' : 'Save'}
      </button>

      {editing && (
        <button
          type="button"
          className="linkbtn"
          style={{ color: 'var(--danger)' }}
          onClick={() => {
            if (!window.confirm('Delete this entry?')) return
            deleteExpense(editing.id)
            navigate('/history')
          }}
        >
          Delete
        </button>
      )}

      {toast !== null && <div className="toast">{toast}</div>}
    </div>
  )
}
