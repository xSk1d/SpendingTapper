import { formatGrouped } from '../lib/money'
import type { CycleSummary } from '../lib/summary'

type Props = {
  summary: CycleSummary
  symbol: string
}

/**
 * Legend and direct labels in one row. Two series always get a legend — colour
 * alone is never the only way to tell them apart — and since there are only two,
 * their values ride along rather than hiding in a tooltip a phone cannot show.
 *
 * The swatch carries the hue; the text stays in the muted ink token. A chart
 * colour is chosen to work as a fill, not as 12px type.
 */
export default function MeterLegend({ summary, symbol }: Props) {
  return (
    <ul className="legend">
      <li className="legend-item">
        <span className="swatch swatch-need" aria-hidden="true" />
        Needs <span className="legend-value">{formatGrouped(summary.needCents, symbol)}</span>
      </li>
      <li className="legend-item">
        <span className="swatch swatch-want" aria-hidden="true" />
        Wants <span className="legend-value">{formatGrouped(summary.wantCents, symbol)}</span>
      </li>
    </ul>
  )
}
