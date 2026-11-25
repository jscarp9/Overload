package com.overload.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Version shown to the user (CI sets it: 0.1, 0.2, ...). */
val VERSION: String = BuildConfig.VERSION_NAME

/**
 * Bump NEWS_ID whenever you change NEWS. The sheet pops up once on the next open for anyone whose
 * seenNews doesn't match. It's separate from VERSION because every push makes a new release, and
 * the sheet shouldn't pop up for releases with nothing new to say.
 */
const val NEWS_ID = 1
val NEWS = listOf(
    "Rest alerts on your lock screen" to "Settings → Workout → Rest alerts on lock screen. Leave the app during rest and you get a notification when it's time for your next set.",
    "Slimmer rest timer" to "The rest bar at the bottom is smaller and the next exercise fits on one line.",
    "What's new" to "This screen. Find it again any time at the bottom of Settings."
)

@Composable
fun WhatsNewDialog(onClose: () -> Unit) =
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = ov.bg,
        confirmButton = { OvButton("Let's go 💪", onClose, bg = ov.ink, fg = ov.bg, wide = true) },
        title = { Title("What's new in $VERSION", 24) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Sub("Here's everything new in this update.")
                NEWS.forEach { (t, d) ->
                    Column(Modifier.padding(top = 12.dp)) {
                        OvText(t, 16, FontWeight.SemiBold)
                        OvText(d, 14, FontWeight.Medium, ov.mute)
                    }
                }
            }
        })
