package dev.xsk1d.spendingtapper.ui.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.xsk1d.spendingtapper.ui.theme.KeypadStyle

/**
 * A keypad of our own rather than the system IME. Three reasons, all of which matter
 * on a Flip: it is on screen the instant the app opens, it never resizes the window
 * mid-entry, and its keys stay thumb-sized on the 4.1" cover display where the
 * software keyboard is close to unusable.
 */
@Composable
fun Keypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 60.dp,
    /**
     * When true the pad divides whatever vertical space it is given between its rows
     * instead of claiming a fixed height. The cover screen is roughly 420dp tall, and a
     * fixed-height pad there would push the Save button off the bottom of the screen.
     */
    fillHeight: Boolean = false,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("00", "0", BACKSPACE),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in rows) {
            Row(
                if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (label in row) {
                    Key(
                        label = label,
                        height = if (fillHeight) MIN_KEY_HEIGHT else keyHeight,
                        fillHeight = fillHeight,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (label) {
                                BACKSPACE -> onBackspace()
                                "00" -> { onDigit('0'); onDigit('0') }
                                else -> onDigit(label.first())
                            }
                        },
                        onLongClick = if (label == BACKSPACE) onClear else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Key(
    label: String,
    height: Dp,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.heightIn(min = height))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            )
            .semantics {
                contentDescription = if (label == BACKSPACE) "Delete last digit" else label
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KeypadStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val BACKSPACE = "⌫"

/** Small enough to fit the cover screen, still comfortably larger than a fingertip. */
private val MIN_KEY_HEIGHT = 34.dp
