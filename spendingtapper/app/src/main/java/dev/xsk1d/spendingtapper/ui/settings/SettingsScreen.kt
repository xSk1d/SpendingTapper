package dev.xsk1d.spendingtapper.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.xsk1d.spendingtapper.appContainer
import dev.xsk1d.spendingtapper.data.Settings
import dev.xsk1d.spendingtapper.domain.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context.appContainer))
    val loadedSettings by viewModel.settings.collectAsStateWithLifecycle()
    val settings = loadedSettings ?: Settings()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHost.showSnackbar(it) }
    }

    // The system file picker means SpendingTapper needs no storage permission at all.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportTo(context.contentResolver, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFrom(context.contentResolver, it) } }

    // Seeded once, when the stored settings first arrive. Keying these off the stored
    // values instead would rewrite the field on every keystroke — typing "12.5" would
    // snap to "12.50" under the cursor before the next digit landed.
    val loaded = loadedSettings != null
    var budgetText by remember(loaded) {
        mutableStateOf(
            if (settings.monthlyBudgetCents > 0) Money.formatCents(settings.monthlyBudgetCents)
            else ""
        )
    }
    var dayText by remember(loaded) { mutableStateOf(settings.cycleStartDay.toString()) }
    var symbolText by remember(loaded) { mutableStateOf(settings.currencySymbol) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Budget", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = budgetText,
                onValueChange = { text ->
                    budgetText = text
                    val cents = Money.parseAmount(text)
                    when {
                        cents != null -> viewModel.setBudgetCents(cents)
                        text.isBlank() -> viewModel.setBudgetCents(0L)
                    }
                },
                label = { Text("Monthly budget") },
                prefix = { Text(settings.currencySymbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { text ->
                        dayText = text.filter { it.isDigit() }.take(2)
                        dayText.toIntOrNull()?.let { viewModel.setCycleStartDay(it) }
                    },
                    label = { Text("Cycle starts") },
                    supportingText = { Text(ordinalHint(dayText.toIntOrNull())) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = symbolText,
                    onValueChange = { text ->
                        symbolText = text.take(3)
                        viewModel.setCurrencySymbol(symbolText)
                    },
                    label = { Text("Symbol") },
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                )
            }

            HorizontalDivider()

            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Everything stays on this phone. Export writes a plain CSV you can open " +
                    "in any spreadsheet; import adds those rows back without touching what " +
                    "is already here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { exportLauncher.launch(defaultExportName()) },
                    modifier = Modifier.weight(1f),
                ) { Text("Export CSV") }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("Import CSV") }
            }

            HorizontalDivider()

            Text("Opening SpendingTapper fast", style = MaterialTheme.typography.titleMedium)
            Text(
                "One UI has no back-tap gesture of its own. Install Good Lock, add the " +
                    "RegiStar module, then Back-tap gesture → Double tap → SpendingTapper. " +
                    "To use SpendingTapper on the closed Flip, turn on Settings → Advanced features " +
                    "→ Labs → apps allowed on the cover screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ordinalHint(day: Int?): String = when {
    day == null || day !in 1..31 -> "1–31"
    day == 1 -> "1st of the month"
    day in 2..31 -> "${day}${ordinalSuffix(day)} of the month"
    else -> ""
}

private fun ordinalSuffix(day: Int): String = when {
    day % 100 in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

private fun defaultExportName(): String =
    "spendingtapper-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.csv"
