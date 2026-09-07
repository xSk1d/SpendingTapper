import { formatGrouped } from '../lib/money'
import { meterScale, pct, share, type CycleSummary } from '../lib/summary'
import type { Kind } from '../lib/types'

type Props = {
  summary: CycleSummary
  symbol: string
  /** The amount being typed right now. Drawn as its own lighter segment so you can
   *  watch this purchase eat into what is left before committing to it. */
  pendingCents?: number
  pendingKind?: Kind
  /** `thin` on the entry screen, where the keypad owns the vertical space. */
  size?: 'thin' | 'tall'
}

/**
 * A meter: one ratio against one limit. The fill is stacked by kind, so the same
 * mark answers "how much is left" and "how much of it went on wants" at once.
 *
 * Segments are separated by a 2px gap in the surface colour rather than a stroke —
 * a border would add ink that is not data. Once the cycle goes over, the scale
 * switches to the spend so the overspend has somewhere to be drawn, and a notch
 * marks where the budget fell.
 */
export default function BudgetMeter({
  summary,
  symbol,
  pendingCents = 0,
  pendingKind = 'NEED',
  size = 'thin',
}: Props) {
  const { total, budgetFraction } = meterScale(summary)
  const over = summary.remainingCents < 0

  const label = [
    `${formatGrouped(summary.needCents, symbol)} on needs`,
    `${formatGrouped(summary.wantCents, symbol)} on wants`,
    ...(pendingCents > 0
      ? [`${formatGrouped(pendingCents, symbol)} pending on this entry`]
      : []),
    over
      ? `${formatGrouped(-summary.remainingCents, symbol)} over a ${formatGrouped(summary.budgetCents, symbol)} budget`
      : `${formatGrouped(summary.remainingCents, symbol)} left of ${formatGrouped(summary.budgetCents, symbol)}`,
  ].join(', ')

  return (
    <div className={size === 'tall' ? 'meter meter-tall' : 'meter'} role="img" aria-label={label}>
      {summary.needCents > 0 && (
        <span className="meter-seg meter-need" style={{ width: share(summary.needCents, total) }} />
      )}
      {summary.wantCents > 0 && (
        <span className="meter-seg meter-want" style={{ width: share(summary.wantCents, total) }} />
      )}
      {pendingCents > 0 && (
        <span
          className={pendingKind === 'NEED' ? 'meter-seg meter-pending-need' : 'meter-seg meter-pending-want'}
          style={{ width: share(pendingCents, total) }}
        />
      )}
      {over && (
        <span className="meter-limit" style={{ left: pct(budgetFraction) }} aria-hidden="true" />
      )}
    </div>
  )
}
