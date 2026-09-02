import { describe, expect, it } from 'vitest'
import {
  appendDigit,
  backspace,
  digitsToCents,
  format,
  formatCents,
  formatGrouped,
  parseAmount,
} from './money'

describe('keypad digits', () => {
  it('fills in from the right, like a card terminal', () => {
    let digits = ''
    for (const d of ['1', '2', '5', '0']) digits = appendDigit(digits, d)
    expect(digits).toBe('1250')
    expect(digitsToCents(digits)).toBe(1250)
  })

  it('treats an empty pad as zero rather than NaN', () => {
    expect(digitsToCents('')).toBe(0)
  })

  it('drops leading zeros so "007" is 7 cents, not a longer string', () => {
    let digits = ''
    for (const d of ['0', '0', '7']) digits = appendDigit(digits, d)
    expect(digits).toBe('7')
    expect(digitsToCents(digits)).toBe(7)
  })

  it('refuses a tenth digit instead of silently truncating', () => {
    const nine = '123456789'
    expect(appendDigit(nine, '9')).toBe(nine)
  })

  it('ignores non-digits', () => {
    expect(appendDigit('12', 'x')).toBe('12')
  })

  it('backspaces one digit at a time and stops at empty', () => {
    expect(backspace('125')).toBe('12')
    expect(backspace('')).toBe('')
  })
})

describe('formatting', () => {
  it('always shows two decimal places', () => {
    expect(formatCents(0)).toBe('0.00')
    expect(formatCents(5)).toBe('0.05')
    expect(formatCents(1250)).toBe('12.50')
  })

  it('puts the sign outside the symbol', () => {
    expect(format(1234, '$')).toBe('$12.34')
    expect(format(-1234, '$')).toBe('-$12.34')
  })

  it('groups thousands', () => {
    expect(formatGrouped(123456, '$')).toBe('$1,234.56')
    expect(formatGrouped(100000000, '$')).toBe('$1,000,000.00')
    expect(formatGrouped(-123456, '$')).toBe('-$1,234.56')
  })
})

describe('parseAmount', () => {
  it('reads the shapes a CSV or a text field actually contains', () => {
    expect(parseAmount('12.34')).toBe(1234)
    expect(parseAmount('12.3')).toBe(1230)
    expect(parseAmount('12')).toBe(1200)
    expect(parseAmount('$12.34')).toBe(1234)
    expect(parseAmount('1,234.56')).toBe(123456)
    expect(parseAmount('-12.34')).toBe(-1234)
  })

  it('returns null rather than NaN for junk', () => {
    expect(parseAmount('')).toBeNull()
    expect(parseAmount('abc')).toBeNull()
    expect(parseAmount('.')).toBeNull()
    expect(parseAmount('-')).toBeNull()
    expect(parseAmount('1.2.3')).toBeNull()
  })

  it('truncates beyond two decimal places instead of rounding into the next cent', () => {
    expect(parseAmount('12.349')).toBe(1234)
  })
})
