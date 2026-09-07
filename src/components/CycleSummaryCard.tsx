import { addDays, format as formatDate } from 'date-fns'
import { format as formatMoney, formatGrouped } from '../lib/money'
import { busiestDay, pct, type CycleSummary } from '../lib/summary'
import BudgetMeter from './BudgetMeter'
import MeterLegend from './MeterLegend'

type Props = {
  summary: CycleSummary
  symbol: string
  hasBudget: boolean
}

/** Column height as a share of the busiest day, so the tallest column fills the plot. */
function columnHeight(cents: number, peakCents: number): string {
  if (cents <= 0 || peakCents <= 0) return '0%'
  return pct(cents / peakCents)
}

/**
 * The cycle at a glance: what is left, what it went on, and how it was spread
 * across the days so far.
 *
 * The daily columns have no per-column tooltip on purpose. A 31-day cycle on a
 * phone gives each column about 11px — far under a usable touch target, and a
 * hover layer does not exist on the device this is built for. So the extreme is
 * direct-labelled in text and every value stays reachable in the table below,
 * which is the accessible path anyway.
 */
export default function CycleSummaryCard({ summary, symbol, hasBudget }: Props) {
  const over = summary.remainingCents < 0
  const peak = busiestDay(summary.days)
  const peakCents = peak?.totalCents ?? 0
  const spentDays = summary.days.filter((day) => day.totalCents > 0)

  return (
    <section className="card" aria-label="This cycle">
      <div className="card-head">
        <span className="card-label">
          {hasBudget ? (over ? 'Over budget' : 'Left to spend') : 'Spent this cycle'}
        </span>
        <span className="card-range">
          {formatDate(summary.cycle.start, 'd MMM')} –{' '}
          {/* The cycle end is exclusive, so the last day inside it is the day before. */}
          {formatDate(addDays(summary.cycle.end, -1), 'd MMM')}
        </span>
      </div>

      <div className={over ? 'hero hero-over' : 'hero'}>
        {formatGrouped(
          hasBudget ? Math.abs(summary.remainingCents) : summary.spentCents,
          symbol,
        )}
      </div>

      {hasBudget && (
        <div className="card-sub">
          {formatGrouped(summary.spentCents, symbol)} of{' '}
          {formatGrouped(summary.budgetCents, symbol)} spent
        </div>
      )}

      <BudgetMeter summary={summary} symbol={symbol} size="tall" />
      <MeterLegend summary={summary} symbol={symbol} />

      {summary.days.length >= 5 && spentDays.length >= 2 && (
        <>
          <div
            className="cols"
            role="img"
            aria-label={`Daily spend from ${formatDate(summary.days[0].date, 'd MMM')} to ${formatDate(
              summary.days[summary.days.length - 1].date,
              'd MMM',
            )}, needs and wants stacked. Busiest day ${
              peak ? formatDate(peak.date, 'EEE d MMM') : 'none'
            } at ${formatMoney(peakCents, symbol)}.`}
          >
            {summary.days.map((day) => (
              <span className="col" key={day.date.getTime()}>
                {/* Only non-zero segments are drawn, and the 4px cap goes on whichever
                    one ends up on top — an empty segment would otherwise leave a
                    2px gap floating above the baseline with nothing under it. */}
                {day.wantCents > 0 && (
                  <span
                    className="col-seg col-want col-cap"
                    style={{ height: columnHeight(day.wantCents, peakCents) }}
                  />
                )}
                {day.needCents > 0 && (
                  <span
                    className={day.wantCents > 0 ? 'col-seg col-need' : 'col-seg col-need col-cap'}
                    style={{ height: columnHeight(day.needCents, peakCents) }}
                  />
                )}
              </span>
            ))}
          </div>

          <div className="cols-axis">
            <span>{formatDate(summary.days[0].date, 'd MMM')}</span>
            {peak && (
              <span className="cols-peak">
                Busiest {formatDate(peak.date, 'EEE d')} · {formatMoney(peak.totalCents, symbol)}
              </span>
            )}
            <span>{formatDate(summary.days[summary.days.length - 1].date, 'd MMM')}</span>
          </div>

          {/* The chart's twin. Every value the columns encode is a number here too,
              so nothing is gated behind reading a colour or landing on a mark. */}
          <details className="table-view">
            <summary>Show the numbers</summary>
            <table className="numbers">
              <thead>
                <tr>
                  <th scope="col">Day</th>
                  <th scope="col">Needs</th>
                  <th scope="col">Wants</th>
                  <th scope="col">Total</th>
                </tr>
              </thead>
              <tbody>
                {spentDays.map((day) => (
                  <tr key={day.date.getTime()}>
                    <th scope="row">{formatDate(day.date, 'EEE d MMM')}</th>
                    <td>{formatMoney(day.needCents, symbol)}</td>
                    <td>{formatMoney(day.wantCents, symbol)}</td>
                    <td>{formatMoney(day.totalCents, symbol)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>
        </>
      )}
    </section>
  )
}
