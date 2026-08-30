package dev.xsk1d.spendingtapper

import android.app.Application
import android.content.Context
import dev.xsk1d.spendingtapper.data.ExpenseRepository
import dev.xsk1d.spendingtapper.data.SettingsStore
import dev.xsk1d.spendingtapper.data.SpendingTapperDatabase

/**
 * Hand-rolled container instead of a DI framework: this app has three dependencies,
 * and Hilt would be more moving parts than the thing it wires up.
 */
class AppContainer(context: Context) {
    private val database by lazy { SpendingTapperDatabase.build(context) }
    val repository by lazy { ExpenseRepository(database.expenseDao()) }
    val settings by lazy { SettingsStore(context) }
}

class SpendingTapperApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SpendingTapperApp).container
