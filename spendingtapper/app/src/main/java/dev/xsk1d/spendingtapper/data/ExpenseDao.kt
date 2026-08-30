package dev.xsk1d.spendingtapper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query(
        "SELECT * FROM expenses WHERE occurredAt >= :startMillis AND occurredAt < :endMillis " +
            "ORDER BY occurredAt DESC, id DESC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<Expense>>

    /**
     * A SUM, so the "left this month" figure never loads a single row into memory —
     * it stays instant no matter how many years of entries pile up.
     */
    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM expenses " +
            "WHERE occurredAt >= :startMillis AND occurredAt < :endMillis"
    )
    fun observeSpentCentsBetween(startMillis: Long, endMillis: Long): Flow<Long>

    /** Feeds the "with who" chips: whoever you have logged most recently, first. */
    @Query("SELECT withWho FROM expenses WHERE withWho != '' ORDER BY occurredAt DESC LIMIT 60")
    fun observeRecentWithWho(): Flow<List<String>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: Long): Expense?

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC")
    suspend fun getAllOnce(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
