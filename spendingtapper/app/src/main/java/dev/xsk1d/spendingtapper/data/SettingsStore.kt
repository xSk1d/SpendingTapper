package dev.xsk1d.spendingtapper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val monthlyBudgetCents: Long = 0L,
    val cycleStartDay: Int = 1,
    val currencySymbol: String = "$",
) {
    val hasBudget: Boolean get() = monthlyBudgetCents > 0L
}

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            monthlyBudgetCents = prefs[KEY_BUDGET] ?: 0L,
            cycleStartDay = (prefs[KEY_CYCLE_START_DAY] ?: 1).coerceIn(1, 31),
            currencySymbol = prefs[KEY_CURRENCY] ?: "$",
        )
    }

    suspend fun setMonthlyBudgetCents(cents: Long) {
        store.edit { it[KEY_BUDGET] = cents.coerceAtLeast(0L) }
    }

    suspend fun setCycleStartDay(day: Int) {
        store.edit { it[KEY_CYCLE_START_DAY] = day.coerceIn(1, 31) }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        store.edit { it[KEY_CURRENCY] = symbol.take(3).ifBlank { "$" } }
    }

    private companion object {
        val KEY_BUDGET = longPreferencesKey("monthly_budget_cents")
        val KEY_CYCLE_START_DAY = intPreferencesKey("cycle_start_day")
        val KEY_CURRENCY = stringPreferencesKey("currency_symbol")
    }
}
