package dev.xsk1d.spendingtapper.io

import dev.xsk1d.spendingtapper.data.Expense
import dev.xsk1d.spendingtapper.data.Kind
import dev.xsk1d.spendingtapper.domain.Money
import java.io.BufferedReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Export and import in plain RFC 4180 CSV, so the file opens in any spreadsheet
 * and a description containing a comma, a quote or a newline still survives the
 * round trip.
 */
object Csv {

    val HEADER = listOf("id", "occurred_at", "amount", "kind", "description", "with_who")

    private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    data class ImportResult(
        val expenses: List<Expense>,
        val skipped: Int,
    )

    fun export(expenses: List<Expense>, zone: ZoneId): String = buildString {
        appendLine(HEADER.joinToString(",") { escape(it) })
        for (e in expenses) {
            val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.occurredAt), zone)
            val row = listOf(
                e.id.toString(),
                local.format(TIMESTAMP),
                Money.formatCents(e.amountCents),
                e.kind.name,
                e.description,
                e.withWho,
            )
            appendLine(row.joinToString(",") { escape(it) })
        }
    }

    fun import(reader: BufferedReader, zone: ZoneId, now: Long): ImportResult {
        val rows = parse(reader.readText())
        if (rows.isEmpty()) return ImportResult(emptyList(), 0)

        // Tolerate a file with or without a header line.
        val first = rows.first().map { it.trim().lowercase() }
        val body = if (first.getOrNull(0) == "id" || first.getOrNull(2) == "amount") rows.drop(1) else rows

        val parsed = mutableListOf<Expense>()
        var skipped = 0
        for (row in body) {
            val expense = parseRow(row, zone, now)
            if (expense == null) skipped++ else parsed += expense
        }
        return ImportResult(parsed, skipped)
    }

    private fun parseRow(row: List<String>, zone: ZoneId, now: Long): Expense? {
        if (row.size < 4) return null
        if (row.all { it.isBlank() }) return null

        val amountCents = Money.parseAmount(row[2]) ?: return null
        val occurredAt = parseTimestamp(row[1], zone) ?: return null
        val kind = runCatching { Kind.valueOf(row[3].trim().uppercase()) }.getOrNull() ?: return null

        return Expense(
            // id 0 lets Room assign a fresh one, which is what we want on import:
            // merging a backup into a non-empty database must not overwrite live rows.
            id = 0,
            amountCents = amountCents,
            kind = kind,
            description = row.getOrNull(4).orEmpty().trim(),
            occurredAt = occurredAt,
            withWho = Expense.joinPeople(Expense.splitPeople(row.getOrNull(5).orEmpty())),
            createdAt = now,
        )
    }

    private fun parseTimestamp(raw: String, zone: ZoneId): Long? {
        val text = raw.trim().replace('T', ' ').removeSuffix("Z")
        if (text.isEmpty()) return null
        val candidates = listOf(text, "$text:00", "$text 00:00:00")
        for (candidate in candidates) {
            val parsed = runCatching { LocalDateTime.parse(candidate, TIMESTAMP) }.getOrNull()
            if (parsed != null) return parsed.atZone(zone).toInstant().toEpochMilli()
        }
        // A bare epoch-millis column is also accepted.
        return text.toLongOrNull()
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** A small RFC 4180 reader: handles quoted fields, doubled quotes and embedded newlines. */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            if (row.size > 1 || row.first().isNotBlank()) rows.add(row)
            row = mutableListOf()
        }

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    endRow()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
