package com.geecee.escapelauncher.core.ui.composables

import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geecee.escapelauncher.core.theme.EscapeThemePreview
import com.geecee.escapelauncher.core.ui.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/**
 * Shown instead of the pause when an app with the countdown is opened during one of the user's
 * closed times. Says when it opens again. Going back is the main action; opening anyway leads on
 * to the normal pause.
 *
 * @param appName Name of the app the user tried to open
 * @param reopensAt When the closed time ends, or null if that is more than a week away
 * @param onOpenAnyway Called when the user chooses to open the app regardless
 * @param goBack Called when the user leaves without opening the app
 */
@Composable
fun ClosedNotice(
    appName: String,
    reopensAt: LocalDateTime?,
    onOpenAnyway: () -> Unit,
    goBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler { goBack() }

    val message = if (reopensAt != null) {
        stringResource(R.string.closed_until, formatReopening(context, reopensAt))
    } else {
        stringResource(R.string.closed_for_now)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(brush = pauseBackground)
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

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
                onClick = onOpenAnyway,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_anyway), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * The time apps open again in the user's 12/24 hour format, with the day name if it isn't today
 */
private fun formatReopening(context: Context, reopensAt: LocalDateTime): String {
    val date = Date.from(reopensAt.atZone(ZoneId.systemDefault()).toInstant())
    val time = DateFormat.getTimeFormat(context).format(date)
    return if (reopensAt.toLocalDate() == LocalDate.now()) {
        time
    } else {
        reopensAt.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + time
    }
}

@Preview
@Composable
fun PrevClosedNotice() {
    EscapeThemePreview {
        ClosedNotice(
            appName = "Video app",
            reopensAt = LocalDateTime.now().plusHours(2),
            onOpenAnyway = {},
            goBack = {}
        )
    }
}
