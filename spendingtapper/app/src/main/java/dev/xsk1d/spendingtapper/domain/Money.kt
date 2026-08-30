package dev.xsk1d.spendingtapper.domain

import kotlin.math.absoluteValue

/**
 * Money is integer cents everywhere in this app. Nothing here is ever a Double:
 * a budget that drifts by a fraction of a cent per entry is a budget you stop trusting.
 */
object Money {

    /** Longest amount the keypad will accept: 9,999,999.99. */
    private const val MAX_DIGITS = 9

    /**
     * The keypad hands us the raw digit string the user has typed ("1250" for 12.50).
     * Digits fill in from the right, which is how every calculator and card terminal
     * behaves, so it needs no explanation on the phone.
     */
    fun digitsToCents(digits: String): Long {
        val trimmed = digits.filter { it.isDigit() }.take(MAX_DIGITS)
        if (trimmed.isEmpty()) return 0L
        return trimmed.toLong()
    }

    /** Appends one typed digit, ignoring leading zeros and the length cap. */
    fun appendDigit(digits: String, digit: Char): String {
        if (!digit.isDigit()) return digits
        val next = (digits + digit).trimStart('0')
        return if (next.length > MAX_DIGITS) digits else next
    }

    fun backspace(digits: String): String = digits.dropLast(1)

    /** "1234" -> "12.34". Always two decimal places, no grouping. */
    fun formatCents(cents: Long): String {
        val negative = cents < 0
        val abs = cents.absoluteValue
        val body = "${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
        return if (negative) "-$body" else body
    }

    /** "1234" -> "$12.34", with the sign outside the symbol: "-$12.34". */
    fun format(cents: Long, symbol: String): String {
        val negative = cents < 0
        val body = symbol + formatCents(cents.absoluteValue)
        return if (negative) "-$body" else body
    }

    /** Grouped form for the running total on the entry screen: "$1,234.56". */
    fun formatGrouped(cents: Long, symbol: String): String {
        val negative = cents < 0
        val abs = cents.absoluteValue
        val whole = (abs / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        val body = "$symbol$whole.${(abs % 100).toString().padStart(2, '0')}"
        return if (negative) "-$body" else body
    }

    /** Parses "12.34", "12.3", "12", "$12.34", "1,234.56" from a CSV or a text field. */
    fun parseAmount(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "").filter { it.isDigit() || it == '.' || it == '-' }
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
        val negative = cleaned.startsWith("-")
        val unsigned = cleaned.removePrefix("-")
        if (unsigned.count { it == '.' } > 1) return null
        val parts = unsigned.split(".")
        val whole = parts[0].ifEmpty { "0" }
        val fraction = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
        val cents = whole.toLongOrNull()?.times(100)?.plus(fraction.toLongOrNull() ?: return null)
            ?: return null
        return if (negative) -cents else cents
    }
}
