package dev.xsk1d.spendingtapper.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.xsk1d.spendingtapper.AppContainer
import dev.xsk1d.spendingtapper.data.Expense
import dev.xsk1d.spendingtapper.data.ExpenseRepository
import dev.xsk1d.spendingtapper.data.Kind
import dev.xsk1d.spendingtapper.data.Settings
import dev.xsk1d.spendingtapper.data.SettingsStore
import dev.xsk1d.spendingtapper.domain.BudgetCycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayGroup(
    val date: LocalDate,
    val expenses: List<Expense>,
) {
    val totalCents: Long get() = expenses.sumOf { it.amountCents }
}

data class HistoryUiState(
    val groups: List<DayGroup> = emptyList(),
    val settings: Settings = Settings(),
    val cycleSpentCents: Long = 0L,
    val cycleNeedCents: Long = 0L,
    val cycleWantCents: Long = 0L,
) {
    val leftCents: Long get() = settings.monthlyBudgetCents - cycleSpentCents
}

class HistoryViewModel(
    private val repository: ExpenseRepository,
    settingsStore: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAll(),
        settingsStore.settings,
    ) { expenses, settings ->
        val cycle = BudgetCycle.current(Instant.now(), zone, settings.cycleStartDay)
        val inCycle = expenses.filter { cycle.contains(it.occurredAt) }

        HistoryUiState(
            groups = expenses
                .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }
                .map { (date, items) -> DayGroup(date, items) }
                .sortedByDescending { it.date },
            settings = settings,
            cycleSpentCents = inCycle.sumOf { it.amountCents },
            cycleNeedCents = inCycle.filter { it.kind == Kind.NEED }.sumOf { it.amountCents },
            cycleWantCents = inCycle.filter { it.kind == Kind.WANT }.sumOf { it.amountCents },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun delete(expense: Expense) {
        viewModelScope.launch { repository.delete(expense) }
    }

    fun restore(expense: Expense) {
        // Re-inserting with id 0 keeps the undo from colliding with a row that
        // may have been created in the meantime.
        viewModelScope.launch { repository.add(expense.copy(id = 0)) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(container.repository, container.settings)
            }
        }
    }
}
