package dev.xsk1d.spendingtapper.io

import dev.xsk1d.spendingtapper.data.Expense
import dev.xsk1d.spendingtapper.data.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.time.LocalDateTime
import java.time.ZoneId

class CsvTest {

    private val zone: ZoneId = ZoneId.of("America/Toronto")
    private val now = 1_700_000_000_000L

    private fun millisAt(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun expense(
        id: Long = 1,
        amountCents: Long = 1234,
        kind: Kind = Kind.NEED,
        description: String = "coffee",
        occurredAt: Long = millisAt("2026-03-15T09:30:00"),
        withWho: String = "",
    ) = Expense(id, amountCents, kind, description, occurredAt, withWho, now)

    private fun importText(text: String) =
        Csv.import(BufferedReader(text.reader()), zone, now)

    @Test
    fun `export writes a header and one line per expense`() {
        val csv = Csv.export(listOf(expense(), expense(id = 2, description = "bus")), zone)
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        assertEquals("id,occurred_at,amount,kind,description,with_who", lines[0])
        assertEquals("1,2026-03-15 09:30:00,12.34,NEED,coffee,", lines[1])
    }

    @Test
    fun `a description containing a comma survives the round trip`() {
        val original = expense(description = "lunch, drinks and a tip")
        val result = importText(Csv.export(listOf(original), zone))
        assertEquals(1, result.expenses.size)
        assertEquals("lunch, drinks and a tip", result.expenses.first().description)
    }

    @Test
    fun `a description containing quotes survives the round trip`() {
        val original = expense(description = """the "good" bakery""")
        val exported = Csv.export(listOf(original), zone)
        assertTrue(exported.contains(""""the ""good"" bakery""""))
        assertEquals("""the "good" bakery""", importText(exported).expenses.first().description)
    }

    @Test
    fun `a description containing a newline survives the round trip`() {
        val original = expense(description = "line one\nline two")
        val result = importText(Csv.export(listOf(original), zone))
        assertEquals(1, result.expenses.size)
        assertEquals("line one\nline two", result.expenses.first().description)
    }

    @Test
    fun `every field survives a full round trip`() {
        val original = expense(
            amountCents = 987_65,
            kind = Kind.WANT,
            description = "concert",
            occurredAt = millisAt("2026-07-04T21:15:00"),
            withWho = "Sam, Alex",
        )
        val restored = importText(Csv.export(listOf(original), zone)).expenses.single()

        assertEquals(original.amountCents, restored.amountCents)
        assertEquals(original.kind, restored.kind)
        assertEquals(original.description, restored.description)
        assertEquals(original.occurredAt, restored.occurredAt)
        assertEquals(listOf("Sam", "Alex"), restored.people)
    }

    @Test
    fun `imported rows get fresh ids so a merge cannot overwrite live rows`() {
        val restored = importText(Csv.export(listOf(expense(id = 42)), zone)).expenses.single()
        assertEquals(0L, restored.id)
    }

    @Test
    fun `malformed rows are skipped and counted rather than aborting the import`() {
        val text = """
            id,occurred_at,amount,kind,description,with_who
            1,2026-03-15 09:30:00,12.34,NEED,coffee,
            2,not-a-date,5.00,NEED,broken,
            3,2026-03-15 10:00:00,not-a-number,NEED,broken,
            4,2026-03-15 11:00:00,7.50,NONSENSE,broken,
            5,2026-03-15 12:00:00,3.25,WANT,fine,
        """.trimIndent()

        val result = importText(text)
        assertEquals(2, result.expenses.size)
        assertEquals(3, result.skipped)
        assertEquals(listOf(1234L, 325L), result.expenses.map { it.amountCents })
    }

    @Test
    fun `a file without a header still imports`() {
        val text = "1,2026-03-15 09:30:00,12.34,NEED,coffee,\n"
        assertEquals(1, importText(text).expenses.size)
    }

    @Test
    fun `alternative timestamp shapes are accepted`() {
        val text = """
            id,occurred_at,amount,kind,description,with_who
            1,2026-03-15T09:30:00,1.00,NEED,iso-t,
            2,2026-03-15 09:30,2.00,NEED,no-seconds,
            3,2026-03-15,3.00,NEED,date-only,
        """.trimIndent()

        val result = importText(text)
        assertEquals(0, result.skipped)
        assertEquals(millisAt("2026-03-15T09:30:00"), result.expenses[0].occurredAt)
        assertEquals(millisAt("2026-03-15T09:30:00"), result.expenses[1].occurredAt)
        assertEquals(millisAt("2026-03-15T00:00:00"), result.expenses[2].occurredAt)
    }

    @Test
    fun `an empty file imports nothing rather than failing`() {
        assertEquals(0, importText("").expenses.size)
        assertEquals(0, importText("\n\n").expenses.size)
    }

    @Test
    fun `blank trailing lines are not counted as skipped rows`() {
        val text = "id,occurred_at,amount,kind,description,with_who\n" +
            "1,2026-03-15 09:30:00,12.34,NEED,coffee,\n\n"
        val result = importText(text)
        assertEquals(1, result.expenses.size)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `the parser handles quoted fields with embedded separators`() {
        val rows = Csv.parse("""a,"b,c","d""e"""" + "\n" + "f,g,h")
        assertEquals(listOf("a", "b,c", """d"e"""), rows[0])
        assertEquals(listOf("f", "g", "h"), rows[1])
    }

    @Test
    fun `windows line endings parse the same as unix ones`() {
        val rows = Csv.parse("a,b\r\nc,d\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }
}
