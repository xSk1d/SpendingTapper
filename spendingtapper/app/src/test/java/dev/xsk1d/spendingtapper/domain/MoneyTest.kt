package dev.xsk1d.spendingtapper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `digits fill in from the right like a card terminal`() {
        assertEquals(0L, Money.digitsToCents(""))
        assertEquals(5L, Money.digitsToCents("5"))
        assertEquals(50L, Money.digitsToCents("50"))
        assertEquals(1250L, Money.digitsToCents("1250"))
    }

    @Test
    fun `appending digits drops leading zeros and caps the length`() {
        assertEquals("5", Money.appendDigit("", '5'))
        assertEquals("5", Money.appendDigit("0", '5'))
        assertEquals("50", Money.appendDigit("5", '0'))
        // Nine digits is the cap; a tenth is ignored rather than silently wrapping.
        val nine = "123456789"
        assertEquals(nine, Money.appendDigit(nine, '9'))
    }

    @Test
    fun `backspace removes one digit`() {
        assertEquals("12", Money.backspace("123"))
        assertEquals("", Money.backspace("1"))
        assertEquals("", Money.backspace(""))
    }

    @Test
    fun `formatting always shows two decimal places`() {
        assertEquals("0.00", Money.formatCents(0))
        assertEquals("0.05", Money.formatCents(5))
        assertEquals("0.50", Money.formatCents(50))
        assertEquals("12.34", Money.formatCents(1234))
        assertEquals("-12.34", Money.formatCents(-1234))
    }

    @Test
    fun `the sign sits outside the currency symbol`() {
        assertEquals("$12.34", Money.format(1234, "$"))
        assertEquals("-$12.34", Money.format(-1234, "$"))
    }

    @Test
    fun `grouped formatting inserts thousands separators`() {
        assertEquals("$1.00", Money.formatGrouped(100, "$"))
        assertEquals("$999.99", Money.formatGrouped(99999, "$"))
        assertEquals("$1,000.00", Money.formatGrouped(100000, "$"))
        assertEquals("$1,234,567.89", Money.formatGrouped(123456789, "$"))
        assertEquals("-$1,234.56", Money.formatGrouped(-123456, "$"))
    }

    @Test
    fun `parsing accepts the shapes a CSV or a text field can produce`() {
        assertEquals(1234L, Money.parseAmount("12.34"))
        assertEquals(1230L, Money.parseAmount("12.3"))
        assertEquals(1200L, Money.parseAmount("12"))
        assertEquals(1234L, Money.parseAmount(" $12.34 "))
        assertEquals(123456L, Money.parseAmount("1,234.56"))
        assertEquals(-1234L, Money.parseAmount("-12.34"))
        // Extra decimal places are truncated, not rounded into the next cent.
        assertEquals(1234L, Money.parseAmount("12.349"))
    }

    @Test
    fun `parsing rejects what it cannot make sense of`() {
        assertNull(Money.parseAmount(""))
        assertNull(Money.parseAmount("   "))
        assertNull(Money.parseAmount("abc"))
        assertNull(Money.parseAmount("."))
        assertNull(Money.parseAmount("1.2.3"))
    }

    @Test
    fun `a typed amount survives the round trip through cents`() {
        val digits = "1250"
        assertEquals("12.50", Money.formatCents(Money.digitsToCents(digits)))
    }
}
