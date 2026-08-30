package dev.xsk1d.spendingtapper.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.xsk1d.spendingtapper.AppContainer
import dev.xsk1d.spendingtapper.data.ExpenseRepository
import dev.xsk1d.spendingtapper.data.Settings
import dev.xsk1d.spendingtapper.data.SettingsStore
import dev.xsk1d.spendingtapper.io.Csv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val settingsStore: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /**
     * Null until DataStore has actually read from disk. The screen needs to tell
     * "not loaded yet" apart from "loaded, and the budget is zero", or its text fields
     * seed themselves from a placeholder and then fight the user's typing.
     */
    val settings: StateFlow<Settings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun setBudgetCents(cents: Long) {
        viewModelScope.launch { settingsStore.setMonthlyBudgetCents(cents) }
    }

    fun setCycleStartDay(day: Int) {
        viewModelScope.launch { settingsStore.setCycleStartDay(day) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { settingsStore.setCurrencySymbol(symbol) }
    }

    fun exportTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val expenses = repository.getAllOnce()
                val csv = Csv.export(expenses, zone)
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) }
                        ?: error("could not open the file for writing")
                }
                expenses.size
            }
            _messages.emit(
                result.fold(
                    onSuccess = { "Exported $it ${if (it == 1) "entry" else "entries"}" },
                    onFailure = { "Export failed: ${it.message}" },
                )
            )
        }
    }

    fun importFrom(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val imported = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        Csv.import(reader, zone, System.currentTimeMillis())
                    } ?: error("could not open the file for reading")
                }
                repository.insertAll(imported.expenses)
                imported
            }
            _messages.emit(
                result.fold(
                    onSuccess = { r ->
                        buildString {
                            append("Imported ${r.expenses.size}")
                            if (r.skipped > 0) append(", skipped ${r.skipped} bad ${
                                if (r.skipped == 1) "row" else "rows"
                            }")
                        }
                    },
                    onFailure = { "Import failed: ${it.message}" },
                )
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.repository, container.settings) }
        }
    }
}
