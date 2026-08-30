package dev.xsk1d.spendingtapper.ui.quickadd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.xsk1d.spendingtapper.AppContainer
import dev.xsk1d.spendingtapper.data.Expense
import dev.xsk1d.spendingtapper.data.ExpenseRepository
import dev.xsk1d.spendingtapper.data.Kind
import dev.xsk1d.spendingtapper.data.Settings
import dev.xsk1d.spendingtapper.data.SettingsStore
import dev.xsk1d.spendingtapper.domain.BudgetCycle
import dev.xsk1d.spendingtapper.domain.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Everything the entry screen draws, in one snapshot. */
data class QuickAddUiState(
    val digits: String = "",
    val kind: Kind = Kind.NEED,
    val description: String = "",
    val people: List<String> = emptyList(),
    val occurredAt: Long = System.currentTimeMillis(),
    val recentPeople: List<String> = emptyList(),
    val settings: Settings = Settings(),
    val spentCents: Long = 0L,
    val isEditing: Boolean = false,
    val detailsExpanded: Boolean = false,
) {
    val amountCents: Long get() = Money.digitsToCents(digits)

    val canSave: Boolean get() = amountCents > 0L

    /** Budget minus what is already spent this cycle. */
    val leftCents: Long get() = settings.monthlyBudgetCents - spentCents

    /**
     * The number the screen actually shows: what would be left *after* the entry being
     * typed. Watching this fall as you key in the amount is the entire reason the
     * budget is on this screen and not buried in a report.
     */
    val projectedLeftCents: Long get() = leftCents - amountCents

    val overBudget: Boolean get() = settings.hasBudget && projectedLeftCents < 0L
}

class QuickAddViewModel(
    private val repository: ExpenseRepository,
    settingsStore: SettingsStore,
    private val savedState: SavedStateHandle,
    private val editId: Long?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val form = MutableStateFlow(restoreForm())

    private val settingsFlow: StateFlow<Settings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val spentFlow: StateFlow<Long> = settingsStore.settings
        .map { it.cycleStartDay }
        .distinctUntilChanged()
        .flatMapLatest { startDay ->
            val cycle = BudgetCycle.current(Instant.now(), zone, startDay)
            repository.observeSpentCents(cycle)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val uiState: StateFlow<QuickAddUiState> = combine(
        form,
        settingsFlow,
        spentFlow,
        repository.observeRecentPeople(),
    ) { form, settings, spent, recent ->
        form.copy(
            settings = settings,
            // When editing an existing row, its own amount is already inside `spent`,
            // so subtract it back out or the preview double-counts the entry.
            spentCents = if (form.isEditing) spent - originalAmountCents else spent,
            recentPeople = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, QuickAddUiState())

    private var originalAmountCents: Long = 0L
    private var originalCreatedAt: Long = System.currentTimeMillis()

    init {
        if (editId != null) {
            viewModelScope.launch {
                repository.findById(editId)?.let { existing ->
                    originalAmountCents = existing.amountCents
                    originalCreatedAt = existing.createdAt
                    updateForm {
                        it.copy(
                            digits = existing.amountCents.toString(),
                            kind = existing.kind,
                            description = existing.description,
                            people = existing.people,
                            occurredAt = existing.occurredAt,
                            isEditing = true,
                            detailsExpanded = true,
                        )
                    }
                }
            }
        }
    }

    fun onDigit(digit: Char) = updateForm { it.copy(digits = Money.appendDigit(it.digits, digit)) }

    fun onBackspace() = updateForm { it.copy(digits = Money.backspace(it.digits)) }

    fun onClear() = updateForm { it.copy(digits = "") }

    fun onKindChange(kind: Kind) = updateForm { it.copy(kind = kind) }

    fun onDescriptionChange(text: String) = updateForm { it.copy(description = text) }

    fun onOccurredAtChange(millis: Long) = updateForm { it.copy(occurredAt = millis) }

    fun onToggleDetails() = updateForm { it.copy(detailsExpanded = !it.detailsExpanded) }

    fun onTogglePerson(name: String) = updateForm { state ->
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@updateForm state
        val present = state.people.any { it.equals(trimmed, ignoreCase = true) }
        val next = if (present) {
            state.people.filterNot { it.equals(trimmed, ignoreCase = true) }
        } else {
            state.people + trimmed
        }
        state.copy(people = next)
    }

    /** Saves, then hands the caller control so the activity can finish itself. */
    fun save(onDone: () -> Unit) {
        val state = form.value
        if (!state.canSave) return
        viewModelScope.launch {
            val expense = Expense(
                id = editId ?: 0L,
                amountCents = state.amountCents,
                kind = state.kind,
                description = state.description.trim(),
                occurredAt = state.occurredAt,
                withWho = Expense.joinPeople(state.people),
                createdAt = if (editId != null) originalCreatedAt else System.currentTimeMillis(),
            )
            if (editId != null) repository.update(expense) else repository.add(expense)
            clearSavedForm()
            onDone()
        }
    }

    private fun updateForm(transform: (QuickAddUiState) -> QuickAddUiState) {
        form.value = transform(form.value).also { persistForm(it) }
    }

    // A half-typed expense should survive the process being killed while the phone is
    // folded, so the in-progress form is mirrored into SavedStateHandle.
    private fun persistForm(state: QuickAddUiState) {
        savedState[KEY_DIGITS] = state.digits
        savedState[KEY_KIND] = state.kind.name
        savedState[KEY_DESCRIPTION] = state.description
        savedState[KEY_PEOPLE] = Expense.joinPeople(state.people)
        savedState[KEY_OCCURRED_AT] = state.occurredAt
        savedState[KEY_DETAILS] = state.detailsExpanded
    }

    private fun clearSavedForm() {
        listOf(KEY_DIGITS, KEY_KIND, KEY_DESCRIPTION, KEY_PEOPLE, KEY_OCCURRED_AT, KEY_DETAILS)
            .forEach { savedState.remove<Any>(it) }
    }

    private fun restoreForm(): QuickAddUiState = QuickAddUiState(
        digits = savedState[KEY_DIGITS] ?: "",
        kind = savedState.get<String>(KEY_KIND)
            ?.let { runCatching { Kind.valueOf(it) }.getOrNull() } ?: Kind.NEED,
        description = savedState[KEY_DESCRIPTION] ?: "",
        people = Expense.splitPeople(savedState[KEY_PEOPLE] ?: ""),
        occurredAt = savedState[KEY_OCCURRED_AT] ?: System.currentTimeMillis(),
        detailsExpanded = savedState[KEY_DETAILS] ?: false,
    )

    companion object {
        private const val KEY_DIGITS = "digits"
        private const val KEY_KIND = "kind"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_PEOPLE = "people"
        private const val KEY_OCCURRED_AT = "occurredAt"
        private const val KEY_DETAILS = "details"

        fun factory(container: AppContainer, editId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    QuickAddViewModel(
                        repository = container.repository,
                        settingsStore = container.settings,
                        savedState = createSavedStateHandle(),
                        editId = editId,
                    )
                }
            }
    }
}
