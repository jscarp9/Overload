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
const val NEWS_ID = 3
val NEWS = listOf(
    "Profile tab" to "Your profile has its own tab now, between Crew and Stats. Edit profile and Customize are short lists: tap a row to change just that one thing.",
    "Crew chat" to "A Chat tab in Crew. Reply, react, @mention and share your last workout. The crew owner can delete, pin or mute.",
    "Crew home" to "Animated scene banners, a crest you design, crew rank from Iron to Olympian, a trophy case, crew records, a season wall and a shareable crew card.",
    "Boss raids" to "Every week a boss shows up in your crew feed. Every pound the crew lifts does damage. Beat it for crew XP, mystery boxes and the Boss Slayer plate.",
    "Lift together" to "At the gym with crewmates? Start a group from the ⋯ menu on Today. Everyone sees each other’s sets and rest timers, and you finish with one shared workout card.",
    "Crew profiles" to "Tap anyone in your crew for their full profile: banner, avatar, motto, flex stats, best lift, pinned trophies, plate collection and PR timeline. Give respect once a day and vote on nicknames.",
    "Themed plates" to "Rank plates, Legend plates for each lift, crew trophy plates, funny plates, holiday plates and 14 special finishes like Mirror Chrome, Lava and Galaxy. Plan → Plate. Engrave your name on the big plate in Edit profile.",
    "Customize" to "App themes, backgrounds, rest timer skins, sound packs, set and PR effects, name effects, post card styles, banners and new reactions. Plus mystery boxes you can only earn.",
    "Trophy room" to "Trophies on glass shelves in your profile. Tap one to spin it. Your crew can see yours.",
    "Funny stuff" to "Hidden funny badges, roast lines on crew posts, the shame plate, an excuse generator, rest timer one-liners and a plate that talks back when you tap it.",
    "Nothing gets lost" to "Deleted things go to a Trash for 30 days (Settings → Backup and restore). A full copy is saved before every update and anything missing is put back.",
    "Calmer screens" to "Glass cards with the background art showing through, a compact Today card with one Start button, challenges in one row, a slimmer crew feed and Settings as a short list.",
    "App tour" to "New to Overload? Take the quick tour in Settings → Help."
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
