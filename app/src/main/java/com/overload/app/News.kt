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
const val NEWS_ID = 2
val NEWS = listOf(
    "Crew" to "A new tab where you and your friends post workouts, react, comment and see who's lifting right now. Tap someone who's live to send them hype 🔥 Works with friends on the web app too.",
    "The Arena" to "A crew rank board with a podium, Rank Power, titles you can steal, rivals, lift boards and crew challenges.",
    "Back up and sync" to "Optional account with a recovery code. Your workouts sync across devices, including the web app, and your crew spot follows you. Settings → Data.",
    "Ranks and badges" to "Climb from Iron to Olympian with divisions, promotion trials and prestige stars. Tap the emblem at the top to see your profile and every badge.",
    "Daily challenges and weekly quests" to "3 new challenges every day on Today and 3 quests every week in Stats. Plus a weekly report card on Mondays.",
    "Seasons" to "A 30-level season pass with plate colors, frames, auras and nameplates.",
    "Lift ranks and muscle mastery" to "Every lift gets a strength rank for your bodyweight, and your body map levels up from Bronze to Legend.",
    "Goals and quest chains" to "Set a lift goal with a date and see your projected finish. Quest chains walk you to your next big plate milestone.",
    "Post-workout breakdown" to "Finish a workout to see a muscle map of what you hit and some fun stats.",
    "Four tabs" to "Today, Crew, Stats and Plan. Progress lives under Stats → Lifts and Notes under Plan → Notes. New features unlock as you go."
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
