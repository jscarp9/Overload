# Overload

**Log every set, beat last week, watch the numbers climb.**

Overload is a simple workout log built around progressive overload. Pick a plan, log your weight and reps, and the app tells you what to lift next time. This branch is the native Android version, written in Kotlin with Jetpack Compose. The original web app lives on the [`main`](../../tree/main) branch.

## Install

1. Open the [Releases](../../releases) page and download the newest `overload-0.17.x.apk`.
2. Open it on your Android phone. If asked, allow installs from your browser or file manager.
3. Later updates install over the top and keep your data.

Every push to the `kotlin` branch builds a new release automatically. The app is at version **0.17**, the same as the web app. Releases are numbered 0.17.1, 0.17.2, … When the web app moves to a new version, change `overloadVersion` in `gradle.properties` to match.

## Features

**Plans**
- Four tabs plus your profile: **Today, Crew, Profile, Stats, Plan**. New lifters get a quick 5-step app tour (replay it in Settings → Help).
- Start from a template: **Jake's split**, **Push pull legs**, **Upper lower**, **Full body**, or **Build my own**.
- Edit any day: rename it, add exercises from a library of 60+ or type your own, group them into supersets, set rest time.
- **Themed plates:** each day gets a plate from Plan → Plate, picked by category: colours, 14 special finishes (Mirror Chrome, Lava, Galaxy…), rank plates, a Legend plate for every lift, crew trophy plates, funny plates, holiday plates, plus Founder and OG plates.

**Logging**
- Weight and reps for every set, with last week's numbers shown as a guide.
- Suggestions for next time, using double progression: add reps until you hit the top of the range, then add weight.
- New PRs are flagged as you lift.
- Optional reps in reserve (RIR) box.
- Swap an exercise for just today, add or remove sets, and keep notes per exercise and per workout.
- A compact Today card: the big plate fills up as you go (tap it, it talks back), one Start button, and a ⋯ menu with Lift together, the excuse generator and session notes. Your name or motto can be engraved on the plate rim.
- A slim week strip you can swipe between weeks, and today's challenges in one row.
- Plate calculator and warm-up sets.

**Rest timer and workout clock**
- Rest starts on its own after the last exercise in a round, with +15/−15, restart and stop buttons.
- Sound and vibration when rest ends, with rest timer skins (ring, bar, spinning plate, neon), sound packs and rest one-liners.
- **Lock-screen rest alerts:** leave the app during rest and you get a notification when it's time for the next set.
- A workout clock with pause, and a summary when you finish: time, sets, volume and PRs.

**Heart rate**
- Connect Samsung Health (or any watch app) through Health Connect.
- After each workout you see average and max heart rate, plus a graph of your heart rate with your average marked.

**Progress and stats**
- Estimated one-rep max chart for every lift, with stall detection.
- A weekly view: sessions done, hard sets, volume, PRs, and sets per muscle against your plan.
- Volume for the last 8 weeks and your week streak.

**Profile, ranks, badges and seasons**
- A Profile tab with your banner, avatar, rank, motto and pinned badges. **Edit profile** and **Customize** are short lists: banner, avatar, motto, gym, walkout song, lifter type, flex stats, pinned trophies, favourite plate, profile colour and jersey number.
- **Customize:** app theme colours, backgrounds, set-complete effects, PR celebrations, rest timer skins, sound packs, name effects, post card styles and reactions. Some come only from **mystery boxes** you earn from the season pass, rank-ups and boss raids.
- A **trophy room** with spinning trophies, and badges behind one Badges row with category tabs.
- Ranks from Iron to Olympian with divisions, promotion trials and prestige stars. Tap the emblem at the top for your profile.
- 70+ badges (including hidden funny ones like Leg Day Dodger and Ghost), milestones, weekly badges, daily challenges, weekly quests and a Monday report card.
- A 30-level season pass with plate colours, frames, auras and nameplates.
- Lift ranks for your bodyweight, muscle mastery, goals with a projected finish date, and quest chains.
- A muscle map and fun stats after every workout, and a year in review.

**Crew**
- Join or start a crew (shared with friends on the web app). Post workouts, react, comment and see who's lifting live. Roast lines under workout posts (can be turned off).
- **Crew chat:** replies, reactions, @mentions, share your workout card, pinned messages. The owner can delete and mute.
- **Crew home:** animated scene banners, a crest the owner designs, crew rank from Iron to Olympian, a trophy case, crew records, a season wall, joke weekly awards, a Hall of Fame and a shareable crew card.
- **Boss raids:** a weekly boss every pound the crew lifts damages. **Lift together:** start a group at the gym, see each other's sets and rest timers live and finish with one shared card.
- **Crew profiles:** tap anyone for their full profile. Give respect once a day, vote on nicknames, owners hand out roles, and the shame plate goes to whoever skips leg day.
- The Arena (one row of tabs: Rankings, This week, Lifts, Trophies, Records, Awards): podium, Rank Power, titles, rivals, lift boards, crew challenges, crew levels and monthly awards.

**Notes, settings and backup**
- Coloured notes you can pin.
- Light, dark or automatic theme. lb or kg.
- Save a backup file and restore it, including backups from the web version.
- **Nothing gets lost:** deleted exercises, notes, goals, sets and plans wait in a **Trash** for 30 days. A full copy is saved before every update, and anything missing afterwards is put back. **Export everything** writes one file with all of it.
- Glass cards with the background art always showing through (Dim art option), and Settings as a short list of pages.
- Optional cloud backup and sync (recovery code), shared with the web app.
- A **What's new** screen after each update.

All data stays on your phone.

## Build it yourself

Open this folder in Android Studio (Ladybug or newer), let Gradle sync, and run it on a phone or emulator.

- Min Android version: 8.0 (SDK 26). Heart rate needs Android 9 or newer.
- Compiles against SDK 36, targets SDK 35.
- Android Gradle plugin 8.10.1, Gradle 8.11.1, Kotlin 2.0.21, JDK 17.

Tests: `gradle testDebugUnitTest` checks the engine gives the same daily challenges and quests as the web app.

From the command line: `gradle assembleDebug`. The APK lands in `app/build/outputs/apk/debug/`.

Debug builds are signed with the `debug.keystore` in the repo root. That way every build has the same signature and updates install over the old app. It's a throwaway key, not suitable for the Play Store.

## Code layout

All source is in `app/src/main/java/com/overload/app/`.

| File | What it does |
|---|---|
| `Data.kt` | Saved data models: plan, sessions, sets, notes, settings, heart rate |
| `Plans.kt` | Exercise library and the plan templates |
| `Logic.kt` | e1RM, PR detection, progression suggestions, stall check, plate calc, warm-ups, weekly stats |
| `OverloadViewModel.kt` | All state and actions: logging, rest timer, workout clock, plan edits, notes, backup and restore |
| `MainActivity.kt` | Main layout, bottom nav, floating rest timer |
| `TodayScreen.kt` | Week strip, day card, exercise blocks, plate/warm-up/swap, exercise picker |
| `OtherScreens.kt` | Progress, Stats and Notes tabs |
| `PlanSettings.kt` | Plan editor, Settings, onboarding |
| `RestAlerts.kt` | Lock-screen rest alerts (exact alarm and notification while the app is in the background) |
| `Health.kt` | Heart rate through Health Connect: reading, the post-workout card and graph, the permission explanation screen |
| `News.kt` | What's new screen |
| `Engine.kt` | Ranks, badges, challenges, quests, seasons, lift ranks, mastery, goals (ported from the web app, same results) |
| `ProfileScreen.kt` | Profile, badge sheets, frames and auras, season pass, rank-up moment, year in review |
| `TodayExtras.kt` | Smart card, post-workout breakdown, challenges, goals, quest chains, weekly quests |
| `Art.kt` | Muscle map, rank emblem, frames and auras, confetti |
| `Crew.kt`, `CrewModels.kt`, `CrewScreen.kt` | Crew server calls, live socket, Arena logic and the Crew tab |
| `Sync.kt`, `AccountUi.kt` | Cloud backup and sync with the web app's server |
| `Sheets.kt`, `UiBits.kt` | Bottom sheets and shared UI pieces |
| `Theme.kt`, `Components.kt` | Colours, background art, plate graphics, shared UI pieces |
| `Locker.kt` | Plate finishes and themed plates, the Customize locker, mystery boxes, trophies, flex stats, funny lines, the tour |
| `PlateArt.kt`, `Art2.kt`, `Svg.kt` | Plate designs, the founder plate, scene banners, crest, bosses and trophies (drawn from the web app's SVG paths) |
| `Profile16.kt` | Profile tab, crew profile page, Edit profile, Customize, badges, trophy room, plate picker, Trash and other sheets |
| `Chat.kt` | Crew chat |
| `CrewHome.kt`, `CrewExtras.kt` | Crew header, Arena tabs, trophy case, records, boss raids, lift together, Hall of Fame, crest, owner tools |
| `Trash.kt` | Trash, update safety net, Export everything |
| `Fx.kt` | Sound packs, set and PR effects |

Data is saved as JSON in the app's private storage.

**Updating What's new:** edit `NEWS` in `News.kt` and add 1 to `NEWS_ID`. The screen then pops up once for everyone after they update.

### Differences from the web version

Not ported yet: workout share cards (image export; the crew card is there), passkey sign-in (use the recovery code), crew pings (lock-screen alerts for crew posts), copy-a-day, notes history, on-phone daily snapshots and the welcome splash. The Customize options for welcome screen, app icon and share card theme only change the web app for now. Sharing your profile shares text instead of an image. Fonts are the system font. Add Archivo and Inter in `res/font` to match the web look.

## License

© 2026 Jake Scarpa. All rights reserved. See [LICENSE](LICENSE).
