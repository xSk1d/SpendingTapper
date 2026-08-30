package dev.xsk1d.spendingtapper.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Kind { NEED, WANT }

@Entity(
    tableName = "expenses",
    indices = [Index("occurredAt")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Integer cents. Never a Double. */
    val amountCents: Long,
    val kind: Kind,
    val description: String,
    /** Date and time in one field, epoch millis. */
    val occurredAt: Long,
    /** Comma-joined names; empty when the purchase was solo. */
    val withWho: String,
    val createdAt: Long,
) {
    val people: List<String>
        get() = splitPeople(withWho)

    companion object {
        fun splitPeople(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun joinPeople(people: Collection<String>): String =
            people.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
    }
}
