import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { format as formatDate } from 'date-fns'
import { exportCsv, importCsv } from '../lib/csv'
import { readTextFile, saveTextFile } from '../lib/files'
import { formatCents, parseAmount } from '../lib/money'
import { nowISO, uid, useStore } from '../lib/store'

export default function SettingsPage() {
  const navigate = useNavigate()
  const expenses = useStore((s) => s.expenses)
  const settings = useStore((s) => s.settings)
  const updateSettings = useStore((s) => s.updateSettings)
  const addImported = useStore((s) => s.addImported)

  // The budget is edited as text so a half-typed "12." does not get rewritten
  // underneath the cursor. It is committed to the store on every parseable change.
  const [budgetText, setBudgetText] = useState(() =>
    settings.monthlyBudgetCents > 0 ? formatCents(settings.monthlyBudgetCents) : '',
  )
  const [toast, setToast] = useState<string | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (toast === null) return
    const timer = window.setTimeout(() => setToast(null), 2600)
    return () => clearTimeout(timer)
  }, [toast])

  const onBudgetChange = (text: string) => {
    setBudgetText(text)
    if (text.trim() === '') {
      updateSettings({ monthlyBudgetCents: 0 })
      return
    }
    const cents = parseAmount(text)
    if (cents !== null && cents >= 0) updateSettings({ monthlyBudgetCents: cents })
  }

  const onExport = async () => {
    const filename = `spendingtapper-${formatDate(new Date(), 'yyyy-MM-dd')}.csv`
    try {
      await saveTextFile(filename, exportCsv(expenses))
    } catch {
      setToast('Export cancelled')
    }
  }

  const onImport = async (file: File) => {
    const text = await readTextFile(file)
    const result = importCsv(text, uid, nowISO())
    addImported(result.expenses)
    setToast(
      result.skipped > 0
        ? `Imported ${result.expenses.length}, skipped ${result.skipped}`
        : `Imported ${result.expenses.length}`,
    )
  }

  return (
    <div className="screen">
      <div className="topbar">
        <button type="button" className="icon-btn" aria-label="Back" onClick={() => navigate(-1)}>
          ←
        </button>
        <h1 className="topbar-title">Settings</h1>
      </div>

      <div className="middle">
        <div className="setting">
          <div className="setting-title">Monthly budget</div>
          <input
            className="input"
            inputMode="decimal"
            placeholder="0.00"
            value={budgetText}
            onChange={(e) => onBudgetChange(e.target.value)}
            aria-label="Monthly budget"
          />
          <div className="setting-hint">
            The one number everything counts down from. Leave it empty for no budget.
          </div>
        </div>

        <div className="setting">
          <div className="setting-title">Cycle starts</div>
          <input
            className="input"
            type="number"
            min={1}
            max={31}
            value={settings.cycleStartDay}
            onChange={(e) => {
              const day = Number.parseInt(e.target.value, 10)
              if (Number.isFinite(day)) {
                updateSettings({ cycleStartDay: Math.min(31, Math.max(1, day)) })
              }
            }}
            aria-label="Cycle start day"
          />
          <div className="setting-hint">
            The day the month rolls over. Set it to 25 if you are paid on the 25th — short
            months clamp, so a 31 lands on the 28th in February.
          </div>
        </div>

        <div className="setting">
          <div className="setting-title">Symbol</div>
          <input
            className="input"
            maxLength={3}
            value={settings.currencySymbol}
            onChange={(e) => updateSettings({ currencySymbol: e.target.value })}
            aria-label="Currency symbol"
          />
          <div className="setting-hint">
            Display only. Nothing is ever converted between currencies.
          </div>
        </div>

        <div className="actions">
          <button type="button" className="secondary" onClick={() => void onExport()}>
            Export CSV
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => fileInput.current?.click()}
          >
            Import CSV
          </button>
          <input
            ref={fileInput}
            type="file"
            accept=".csv,text/csv"
            hidden
            aria-label="Import CSV file"
            onChange={(e) => {
              const file = e.target.files?.[0]
              // Reset first, so picking the same file twice still fires a change.
              e.target.value = ''
              if (file) void onImport(file)
            }}
          />
          <div className="setting-hint">
            Imported rows always get fresh ids, so importing a backup into a database that
            already has entries merges rather than overwrites. Rows that cannot be read are
            skipped and counted.
          </div>
        </div>
      </div>

      {toast !== null && <div className="toast">{toast}</div>}
    </div>
  )
}
