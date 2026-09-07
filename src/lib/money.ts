/**
 * Money is integer cents everywhere in this app. Nothing here is ever a float:
 * a budget that drifts by a fraction of a cent per entry is a budget you stop
 * trusting. Format only at the edge.
 */

/** Longest amount the keypad will accept: 9,999,999.99. */
const MAX_DIGITS = 9

/**
 * The keypad hands over the raw digit string typed so far ("1250" for 12.50).
 * Digits fill in from the right, which is how every calculator and card terminal
 * behaves, so it needs no explanation on the phone.
 */
export function digitsToCents(digits: string): number {
  const trimmed = digits.replace(/\D/g, '').slice(0, MAX_DIGITS)
  if (trimmed === '') return 0
  return Number.parseInt(trimmed, 10)
}

/** Appends one typed digit, dropping leading zeros and respecting the length cap. */
export function appendDigit(digits: string, digit: string): string {
  if (!/^\d$/.test(digit)) return digits
  const next = (digits + digit).replace(/^0+/, '')
  return next.length > MAX_DIGITS ? digits : next
}

export function backspace(digits: string): string {
  return digits.slice(0, -1)
}

/** 1234 -> "12.34". Always two decimal places, no grouping. */
export function formatCents(cents: number): string {
  const negative = cents < 0
  const abs = Math.abs(cents)
  const body = `${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`
  return negative ? `-${body}` : body
}

/** 1234 -> "$12.34", with the sign outside the symbol: "-$12.34". */
export function format(cents: number, symbol: string): string {
  const negative = cents < 0
  const body = symbol + formatCents(Math.abs(cents))
  return negative ? `-${body}` : body
}

/** Grouped form for the running total on the entry screen: "$1,234.56". */
export function formatGrouped(cents: number, symbol: string): string {
  const negative = cents < 0
  const abs = Math.abs(cents)
  const whole = String(Math.floor(abs / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const body = `${symbol}${whole}.${String(abs % 100).padStart(2, '0')}`
  return negative ? `-${body}` : body
}

/**
 * Parses "12.34", "12.3", "12", "$12.34" or "1,234.56" out of a CSV cell or a
 * text field. Returns null rather than NaN so callers have to handle the failure.
 */
export function parseAmount(raw: string): number | null {
  const cleaned = raw
    .trim()
    .replace(/,/g, '')
    .replace(/[^0-9.-]/g, '')
  if (cleaned === '' || cleaned === '-' || cleaned === '.') return null

  const negative = cleaned.startsWith('-')
  const unsigned = negative ? cleaned.slice(1) : cleaned
  if ((unsigned.match(/\./g) ?? []).length > 1) return null

  const [rawWhole = '', rawFraction = ''] = unsigned.split('.')
  const whole = rawWhole === '' ? '0' : rawWhole
  const fraction = rawFraction.padEnd(2, '0').slice(0, 2)
  if (!/^\d+$/.test(whole) || !/^\d{2}$/.test(fraction)) return null

  const cents = Number.parseInt(whole, 10) * 100 + Number.parseInt(fraction, 10)
  if (!Number.isSafeInteger(cents)) return null
  return negative ? -cents : cents
}
