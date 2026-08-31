package dev.xsk1d.spendingtapper.ui.quickadd

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.xsk1d.spendingtapper.appContainer
import dev.xsk1d.spendingtapper.data.Kind
import dev.xsk1d.spendingtapper.domain.Money
import dev.xsk1d.spendingtapper.ui.theme.AmountStyle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The screen a back-tap lands on. Amount first, everything else optional, one button
 * to save. On the cover screen the optional half collapses behind "details" so the
 * keypad and the Save button always fit without scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    editId: Long?,
    onSaved: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: QuickAddViewModel = viewModel(
        key = "quickadd-${editId ?: "new"}",
        factory = QuickAddViewModel.factory(context.appContainer, editId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
    ) {
        // The Flip's cover display is short and near-square. Below this height there is
        // not room for the keypad, the Save button and the optional fields at once, so
        // the optional fields collapse.
        val compact = maxHeight < 620.dp

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            TopBar(
                isEditing = state.isEditing,
                onDismiss = onDismiss,
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings,
                compact = compact,
            )

            AmountDisplay(state = state, compact = compact)

            KindToggle(
                kind = state.kind,
                onKindChange = viewModel::onKindChange,
                modifier = Modifier.fillMaxWidth(),
            )

            if (compact) {
                // On the cover screen the optional fields live in a sheet rather than
                // inline, so they never compete with the keypad for vertical space.
                DetailsSummaryButton(state = state, zone = zone, onClick = viewModel::onToggleDetails)
            } else {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailFields(
                        state = state,
                        zone = zone,
                        onDescriptionChange = viewModel::onDescriptionChange,
                        onTogglePerson = viewModel::onTogglePerson,
                        onPickDate = { showDatePicker = true },
                        onPickTime = { showTimePicker = true },
                        onNow = { viewModel.onOccurredAtChange(System.currentTimeMillis()) },
                    )
                }
            }

            Keypad(
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
                keyHeight = 60.dp,
                fillHeight = compact,
                modifier = if (compact) Modifier.weight(1f) else Modifier,
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (compact) 46.dp else 54.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isEditing) "Update" else "Save")
            }
        }

        if (compact && state.detailsExpanded) {
            ModalBottomSheet(onDismissRequest = viewModel::onToggleDetails) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailFields(
                        state = state,
                        zone = zone,
                        onDescriptionChange = viewModel::onDescriptionChange,
                        onTogglePerson = viewModel::onTogglePerson,
                        onPickDate = { showDatePicker = true },
                        onPickTime = { showTimePicker = true },
                        onNow = { viewModel.onOccurredAtChange(System.currentTimeMillis()) },
                    )
                    Button(
                        onClick = viewModel::onToggleDetails,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Done") }
                }
            }
        }
    }

    if (showDatePicker) {
        val current = LocalDateTime.ofInstant(Instant.ofEpochMilli(state.occurredAt), zone)
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate()
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utcMillis ->
                        // The Material date picker works in UTC; keep the time-of-day
                        // the entry already had and only swap the calendar date.
                        val picked = Instant.ofEpochMilli(utcMillis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.onOccurredAtChange(
                            LocalDateTime.of(picked, current.toLocalTime())
                                .atZone(zone).toInstant().toEpochMilli()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val current = LocalDateTime.ofInstant(Instant.ofEpochMilli(state.occurredAt), zone)
        val pickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = false,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onOccurredAtChange(
                        LocalDateTime.of(
                            current.toLocalDate(),
                            LocalTime.of(pickerState.hour, pickerState.minute),
                        ).atZone(zone).toInstant().toEpochMilli()
                    )
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = pickerState)
            }
        }
    }
}

/** The optional half of the form: what for, with who, and when. */
@Composable
private fun DetailFields(
    state: QuickAddUiState,
    zone: ZoneId,
    onDescriptionChange: (String) -> Unit,
    onTogglePerson: (String) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onNow: () -> Unit,
) {
    OutlinedTextField(
        value = state.description,
        onValueChange = onDescriptionChange,
        label = { Text("What for?") },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth(),
    )

    WithWhoRow(
        selected = state.people,
        recent = state.recentPeople,
        onToggle = onTogglePerson,
    )

    WhenRow(
        occurredAt = state.occurredAt,
        zone = zone,
        onPickDate = onPickDate,
        onPickTime = onPickTime,
        onNow = onNow,
    )
}

/**
 * The cover screen's one-line stand-in for the fields above: it shows what has been
 * filled in so far, so opening the sheet is only necessary when something needs changing.
 */
@Composable
private fun DetailsSummaryButton(state: QuickAddUiState, zone: ZoneId, onClick: () -> Unit) {
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(state.occurredAt), zone)
    val summary = buildList {
        if (state.description.isNotBlank()) add(state.description)
        if (state.people.isNotEmpty()) add("with ${state.people.joinToString(", ")}")
        val isNow = ChronoUnit.MINUTES.between(local, LocalDateTime.now(zone)) in -1..1
        if (!isNow) add("${friendlyDate(local.toLocalDate(), zone)} ${local.format(TIME_FORMAT)}")
    }.joinToString("  ·  ").ifBlank { "Add details" }

    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TopBar(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    compact: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Spacer(Modifier.weight(1f))
        if (!isEditing) {
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
            }
        }
        if (!compact) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun AmountDisplay(state: QuickAddUiState, compact: Boolean) {
    val symbol = state.settings.currencySymbol
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = if (compact) 6.dp else 10.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = Money.formatCents(state.amountCents),
                style = AmountStyle,
                fontSize = if (compact) 42.sp else AmountStyle.fontSize,
                color = if (state.amountCents == 0L) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }

        // The live budget line: what is left after this entry, updating as you type.
        if (state.settings.hasBudget) {
            val color = when {
                state.overBudget -> MaterialTheme.colorScheme.error
                state.projectedLeftCents < state.settings.monthlyBudgetCents / 10 ->
                    MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = if (state.overBudget) {
                    "${Money.formatGrouped(-state.projectedLeftCents, symbol)} over budget"
                } else {
                    "${Money.formatGrouped(state.projectedLeftCents, symbol)} left this month"
                },
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(
                text = "No budget set",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindToggle(kind: Kind, onKindChange: (Kind) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        Kind.entries.forEachIndexed { index, entry ->
            val isSelected = kind == entry
            SegmentedButton(
                selected = isSelected,
                onClick = { onKindChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, Kind.entries.size),
                // The default active container is a tint of secondaryContainer, which sits
                // too close to the surface to read at a glance on a phone held at arm's
                // length. A filled primary swatch is unambiguous.
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    text = if (entry == Kind.NEED) "Need" else "Want",
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithWhoRow(
    selected: List<String>,
    recent: List<String>,
    onToggle: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    // Names already on this entry come first, then whoever was logged recently.
    val names = (selected + recent).distinctBy { it.lowercase() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "With who",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (names.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                names.forEach { name ->
                    val isSelected = selected.any { it.equals(name, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(name) },
                        label = { Text(name, maxLines = 1) },
                        // Colour alone is not enough on a chip this small, so a selected
                        // name also grows a tick.
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Add someone") },
            singleLine = true,
            trailingIcon = {
                if (newName.isNotBlank()) {
                    IconButton(onClick = {
                        onToggle(newName)
                        newName = ""
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Add person")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WhenRow(
    occurredAt: Long,
    zone: ZoneId,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onNow: () -> Unit,
) {
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAt), zone)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onPickDate) { Text(friendlyDate(local.toLocalDate(), zone)) }
        TextButton(onClick = onPickTime) {
            Text(local.format(TIME_FORMAT))
        }
        Spacer(Modifier.weight(1f))
        val isNow = ChronoUnit.MINUTES.between(
            local, LocalDateTime.now(zone)
        ).let { it in -1..1 }
        if (!isNow) {
            TextButton(onClick = onNow) { Text("Now") }
        }
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun friendlyDate(date: LocalDate, zone: ZoneId): String {
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}
