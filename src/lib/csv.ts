import { format as formatDate, isValid, parse as parseDate } from 'date-fns'
import { formatCents, parseAmount } from './money'
import { dedupePeople, joinPeople, splitPeople } from './people'
import type { Expense, Kind } from './types'

/**
 * Export and import in plain RFC 4180 CSV, so the file opens in any spreadsheet
 * and a description containing a comma, a quote or a newline still survives the
 * round trip.
 */

export const HEADER = ['id', 'occurred_at', 'amount', 'kind', 'description', 'with_who'] as const

const TIMESTAMP = 'yyyy-MM-dd HH:mm:ss'

export type ImportResult = {
  /** Parsed rows, each already given a fresh id. */
  expenses: Expense[]
  /** Rows that could not be read. Counted rather than thrown, so one bad line
   *  does not cost you the whole backup. */
  skipped: number
}

function escape(value: string): string {
  return /[",\n\r]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value
}

export function exportCsv(expenses: Expense[]): string {
  const lines = [HEADER.map(escape).join(',')]
  for (const expense of expenses) {
    lines.push(
      [
        expense.id,
        formatDate(new Date(expense.occurredAt), TIMESTAMP),
        formatCents(expense.amountCents),
        expense.kind,
        expense.description,
        joinPeople(expense.withWho),
      ]
        .map(escape)
        .join(','),
    )
  }
  return `${lines.join('\n')}\n`
}

function parseTimestamp(raw: string): number | null {
  const text = raw.trim().replace('T', ' ').replace(/Z$/, '')
  if (text === '') return null
  for (const candidate of [text, `${text}:00`, `${text} 00:00:00`]) {
    const parsed = parseDate(candidate, TIMESTAMP, new Date())
    if (isValid(parsed)) return parsed.getTime()
  }
  // A bare epoch-millis column is also accepted.
  const millis = Number.parseInt(text, 10)
  return Number.isSafeInteger(millis) && String(millis) === text ? millis : null
}

function parseRow(row: string[], newId: () => string, now: string): Expense | null {
  if (row.length < 4) return null
  if (row.every((cell) => cell.trim() === '')) return null

  const amountCents = parseAmount(row[2] ?? '')
  if (amountCents === null) return null

  const occurredAt = parseTimestamp(row[1] ?? '')
  if (occurredAt === null) return null

  const rawKind = (row[3] ?? '').trim().toUpperCase()
  if (rawKind !== 'NEED' && rawKind !== 'WANT') return null

  return {
    // A fresh id on every imported row: merging a backup into a database that
    // already has entries must add to it, never overwrite what is there.
    id: newId(),
    amountCents,
    kind: rawKind as Kind,
    description: (row[4] ?? '').trim(),
    occurredAt,
    withWho: dedupePeople(splitPeople(row[5] ?? '')),
    createdAt: now,
  }
}

export function importCsv(text: string, newId: () => string, now: string): ImportResult {
  const rows = parseRows(text)
  if (rows.length === 0) return { expenses: [], skipped: 0 }

  // Tolerate a file with or without a header line.
  const first = (rows[0] ?? []).map((cell) => cell.trim().toLowerCase())
  const body = first[0] === 'id' || first[2] === 'amount' ? rows.slice(1) : rows

  const expenses: Expense[] = []
  let skipped = 0
  for (const row of body) {
    const expense = parseRow(row, newId, now)
    if (expense === null) skipped++
    else expenses.push(expense)
  }
  return { expenses, skipped }
}

/** A small RFC 4180 reader: quoted fields, doubled quotes and embedded newlines. */
export function parseRows(text: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let inQuotes = false
  let i = 0

  const endField = () => {
    row.push(field)
    field = ''
  }

  const endRow = () => {
    endField()
    if (row.length > 1 || (row[0] ?? '').trim() !== '') rows.push(row)
    row = []
  }

  while (i < text.length) {
    const c = text[i]
    if (inQuotes && c === '"' && text[i + 1] === '"') {
      field += '"'
      i++
    } else if (c === '"') {
      inQuotes = !inQuotes
    } else if (!inQuotes && c === ',') {
      endField()
    } else if (!inQuotes && (c === '\n' || c === '\r')) {
      if (c === '\r' && text[i + 1] === '\n') i++
      endRow()
    } else {
      field += c
    }
    i++
  }
  if (field !== '' || row.length > 0) endRow()
  return rows
}
