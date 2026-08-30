package dev.xsk1d.spendingtapper.data

import dev.xsk1d.spendingtapper.domain.BudgetCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    fun observeInCycle(cycle: BudgetCycle): Flow<List<Expense>> =
        dao.observeBetween(cycle.startMillis, cycle.endMillis)

    fun observeSpentCents(cycle: BudgetCycle): Flow<Long> =
        dao.observeSpentCentsBetween(cycle.startMillis, cycle.endMillis)

    /** Distinct names, most recently used first, for the "with who" chips. */
    fun observeRecentPeople(limit: Int = 8): Flow<List<String>> =
        dao.observeRecentWithWho().map { rows ->
            rows.flatMap { Expense.splitPeople(it) }
                .distinctBy { it.lowercase() }
                .take(limit)
        }

    suspend fun findById(id: Long): Expense? = dao.findById(id)

    suspend fun getAllOnce(): List<Expense> = dao.getAllOnce()

    suspend fun add(expense: Expense): Long = dao.insert(expense)

    suspend fun update(expense: Expense) = dao.update(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    /** Used by CSV import; ids are reassigned by the caller when they would collide. */
    suspend fun insertAll(expenses: List<Expense>) = dao.insertAll(expenses)
}
