import { describe, expect, it } from 'vitest'
import { exportCsv, importCsv, parseRows } from './csv'
import type { Expense } from './types'

let counter = 0
const newId = () => `id-${++counter}`
const NOW = '2026-03-15T09:00:00.000Z'

function expense(over: Partial<Expense> = {}): Expense {
  return {
    id: 'e1',
    amountCents: 1234,
    kind: 'NEED',
    description: 'coffee',
    occurredAt: new Date('2026-03-15T09:30:00').getTime(),
    withWho: [],
    createdAt: NOW,
    ...over,
  }
}

describe('the RFC 4180 reader', () => {
  it('reads quoted fields, doubled quotes and embedded newlines', () => {
    const rows = parseRows('a,"b,c","d""e","f\ng"\n')
    expect(rows).toEqual([['a', 'b,c', 'd"e', 'f\ng']])
  })

  it('handles CRLF line endings', () => {
    expect(parseRows('a,b\r\nc,d\r\n')).toEqual([
      ['a', 'b'],
      ['c', 'd'],
    ])
  })

  it('drops a trailing blank line rather than emitting an empty row', () => {
    expect(parseRows('a,b\n\n')).toEqual([['a', 'b']])
  })
})

describe('round trip', () => {
  it('survives commas, quotes and newlines in a description', () => {
    const original = expense({
      description: 'dinner, "drinks"\nand a taxi',
      withWho: ['Sam', 'Alex'],
      kind: 'WANT',
      amountCents: 4800,
    })

    const result = importCsv(exportCsv([original]), newId, NOW)

    expect(result.skipped).toBe(0)
    expect(result.expenses).toHaveLength(1)
    const [back] = result.expenses
    expect(back?.description).toBe(original.description)
    expect(back?.withWho).toEqual(['Sam', 'Alex'])
    expect(back?.kind).toBe('WANT')
    expect(back?.amountCents).toBe(4800)
    expect(back?.occurredAt).toBe(original.occurredAt)
  })

  it('gives imported rows fresh ids, so a backup merges instead of overwriting', () => {
    const result = importCsv(exportCsv([expense({ id: 'original' })]), newId, NOW)
    expect(result.expenses[0]?.id).not.toBe('original')
  })
})

describe('import tolerance', () => {
  it('accepts a file with no header row', () => {
    const result = importCsv('1,2026-03-15 09:30:00,12.34,NEED,coffee,\n', newId, NOW)
    expect(result.expenses).toHaveLength(1)
    expect(result.skipped).toBe(0)
  })

  it('skips unreadable rows and counts them rather than failing the whole import', () => {
    const csv = [
      'id,occurred_at,amount,kind,description,with_who',
      '1,2026-03-15 09:30:00,12.34,NEED,good,',
      '2,not-a-date,12.34,NEED,bad date,',
      '3,2026-03-15 09:30:00,not-money,NEED,bad amount,',
      '4,2026-03-15 09:30:00,12.34,MAYBE,bad kind,',
      '5,2026-03-15 10:00:00,5.00,WANT,also good,',
    ].join('\n')

    const result = importCsv(csv, newId, NOW)
    expect(result.expenses.map((e) => e.description)).toEqual(['good', 'also good'])
    expect(result.skipped).toBe(3)
  })

  it('accepts a bare epoch-millis timestamp', () => {
    const millis = new Date('2026-03-15T09:30:00').getTime()
    const result = importCsv(`1,${millis},12.34,NEED,coffee,\n`, newId, NOW)
    expect(result.expenses[0]?.occurredAt).toBe(millis)
  })

  it('returns nothing for an empty file instead of throwing', () => {
    expect(importCsv('', newId, NOW)).toEqual({ expenses: [], skipped: 0 })
  })
})
