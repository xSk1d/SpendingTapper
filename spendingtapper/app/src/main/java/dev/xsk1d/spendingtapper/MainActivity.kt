package dev.xsk1d.spendingtapper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.xsk1d.spendingtapper.ui.history.HistoryScreen
import dev.xsk1d.spendingtapper.ui.quickadd.QuickAddScreen
import dev.xsk1d.spendingtapper.ui.settings.SettingsScreen
import dev.xsk1d.spendingtapper.ui.theme.SpendingTapperTheme

private const val ROUTE_QUICK_ADD = "quick_add"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_SETTINGS = "settings"
private const val ARG_EDIT_ID = "editId"

/**
 * The only activity. Launching the app *is* the entry screen — that is the whole point:
 * a back-tap gesture (Good Lock / RegiStar) launches SpendingTapper and lands on the keypad with
 * nothing in the way.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SpendingTapperTheme {
                val navController = rememberNavController()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NavHost(navController = navController, startDestination = ROUTE_QUICK_ADD) {
                        composable(ROUTE_QUICK_ADD) {
                            QuickAddScreen(
                                editId = null,
                                onSaved = { finish() },
                                onOpenHistory = { navController.navigate(ROUTE_HISTORY) },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onDismiss = { finish() },
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate("$ROUTE_QUICK_ADD/$id") },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                            )
                        }
                        composable("$ROUTE_QUICK_ADD/{$ARG_EDIT_ID}") { entry ->
                            val id = entry.arguments?.getString(ARG_EDIT_ID)?.toLongOrNull()
                            QuickAddScreen(
                                editId = id,
                                onSaved = { navController.popBackStack() },
                                onOpenHistory = {},
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onDismiss = { navController.popBackStack() },
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
