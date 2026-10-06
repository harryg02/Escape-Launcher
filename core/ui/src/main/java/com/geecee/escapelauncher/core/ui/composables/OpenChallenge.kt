package com.geecee.escapelauncher.core.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geecee.escapelauncher.core.theme.EscapeThemePreview
import com.geecee.escapelauncher.core.ui.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private const val PAUSE_SECONDS = 5
private val sessionLengthMinutes = listOf(5, 15, 30)
private val pauseLight = Color(0xFFB2D8D8)
private val pauseDark = Color(0xFF004C4C)

/**
 * Full screen pause shown before opening an app that has the open countdown. It counts down,
 * optionally asks what the app is being opened for, and only opens the app when the user taps to
 * continue. Going back is always available, including with the system back gesture.
 *
 * @param appName Name of the app that is about to open
 * @param askIntention Whether to ask what the app is being opened for. An answer is needed to continue
 * @param askSessionLength Whether to offer a choice of how long the app will be used for. Optional
 * @param onContinue Called when the user taps to open the app, with the reason they picked if asked
 * and the minutes they planned, if any
 * @param goBack Called when the user leaves without opening the app
 */
@Composable
fun OpenChallenge(
    appName: String,
    askIntention: Boolean,
    askSessionLength: Boolean,
    haptics: HapticFeedback,
    hapticsEnabled: Boolean,
    onContinue: (intention: String?, minutes: Int?) -> Unit,
    goBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsLeft by rememberSaveable { mutableIntStateOf(PAUSE_SECONDS) }
    var intentionIndex by rememberSaveable { mutableIntStateOf(-1) }
    var sessionLengthIndex by rememberSaveable { mutableIntStateOf(-1) }
    val intentions = listOf(
        stringResource(R.string.intention_something_specific),
        stringResource(R.string.intention_replying),
        stringResource(R.string.intention_quick_check),
        stringResource(R.string.intention_not_sure)
    )
    // The last option is "No limit", which has no index in sessionLengthMinutes
    val sessionLengths = sessionLengthMinutes.map { stringResource(R.string.minutes_short, it) } +
            stringResource(R.string.no_limit)

    BackHandler { goBack() }

    LaunchedEffect(Unit) {
        if (secondsLeft == 0) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1.seconds)
            secondsLeft--
        }
        if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val answered = !askIntention || intentionIndex >= 0

    Box(
        modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(pauseLight, pauseDark),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = appName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )

            if (askIntention) {
                Spacer(Modifier.height(24.dp))
                PauseQuestion(
                    question = stringResource(R.string.intention_question),
                    options = intentions,
                    selectedIndex = intentionIndex,
                    onSelect = { intentionIndex = it }
                )
            }

            if (askSessionLength) {
                Spacer(Modifier.height(24.dp))
                PauseQuestion(
                    question = stringResource(R.string.session_length_question),
                    options = sessionLengths,
                    selectedIndex = sessionLengthIndex,
                    onSelect = { sessionLengthIndex = it },
                    compact = true
                )
            }

            Spacer(Modifier.height(32.dp))

            PauseActions(
                appName = appName,
                secondsLeft = secondsLeft,
                canContinue = secondsLeft == 0 && answered,
                onContinue = {
                    if (hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onContinue(
                        intentions.getOrNull(intentionIndex).takeIf { askIntention },
                        sessionLengthMinutes.getOrNull(sessionLengthIndex).takeIf { askSessionLength }
                    )
                },
                goBack = {
                    if (hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    goBack()
                }
            )
        }
    }
}

/**
 * A question with a short list of answers, of which one can be picked
 *
 * @param compact Lay short answers out side by side instead of one per line
 */
@Composable
private fun PauseQuestion(
    question: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = question,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (compact) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    PauseOption(
                        label = option,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
        } else {
            Column(
                Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    PauseOption(
                        label = option,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PauseOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        color = if (selected) pauseDark else Color.White,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.15f))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/**
 * Going back is the main button, opening the app is the quieter one and only works once the
 * pause is over
 */
@Composable
private fun PauseActions(
    appName: String,
    secondsLeft: Int,
    canContinue: Boolean,
    onContinue: () -> Unit,
    goBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = goBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = pauseDark
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.go_back), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onContinue,
            enabled = canContinue,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (secondsLeft > 0) {
                    stringResource(R.string.continue_in_seconds, secondsLeft)
                } else {
                    stringResource(R.string.open_app_name, appName)
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun PrevOpenChallenge() {
    EscapeThemePreview {
        OpenChallenge(
            appName = "Video app",
            askIntention = true,
            askSessionLength = true,
            haptics = LocalHapticFeedback.current,
            hapticsEnabled = false,
            onContinue = { _, _ -> },
            goBack = {}
        )
    }
}
