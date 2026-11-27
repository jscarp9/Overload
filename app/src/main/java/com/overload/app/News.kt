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
const val NEWS_ID = 4
val NEWS = listOf(
    "Meet Coach ✦" to "Tap the gold ✦ Coach button at the top and just ask: swap an exercise, short on time, deload week, change your split. Coach shows the change and you tap Apply. Your old plan goes to Trash first.",
    "Build your plan with AI" to "Snap a photo of your workout notes, type a list, or use your history, and AI builds your whole week. Plan → ⋯ → Build a whole new week with AI.",
    "Coach notes and form tips" to "A short recap after each workout, a coach note in your Monday report, and ✦ Form tips in each exercise’s ⋯ menu. AI is off until you allow it (Settings → Overload AI).",
    "Clean Plan page" to "One row per exercise. Tap it to edit, drag ≡ to reorder, swipe left to delete (with Undo), and tap the 🔗 between two exercises to make a superset.",
    "Rank up, explained" to "Tap the rank emblem at the top to see exactly what your next rank and your crew’s next rank need, plus every plate ranks unlock.",
    "Medals on your shelf" to "Pinned badges are minted coins in bronze, silver, gold and up. Tap your banner or plate on your profile to change them.",
    "Holiday Drops" to "Lift on a holiday and unlock a whole matching set: an animated plate with the year, a banner, a name effect, a crew reaction, a set burst and a dated medal. 3 crew lifts unlock a crew banner.",
    "New crew crests" to "Build your crest from a shape, pattern, emblem, metal, border and ribbon. It earns stars, wings, a crown and laurels as your crew ranks up.",
    "Faster logging" to "Hold a set’s number to copy it to the next set. Swipe a set left to remove it, with Undo. “Same as last time” fills your sets from your last session (⋯ menu).",
    "Crew" to "Stats sit inside the crew banner, the Arena has 4 tabs, Invite friends in crew settings, and Block or Report on any crew profile.",
    "Privacy" to "See what’s saved and shared in Settings → Help → Privacy."
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
