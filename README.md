# Overload

**Log every set, beat last week, watch the numbers climb.**

Overload is a simple workout log built around progressive overload. Pick a plan, log your weight and reps, and the app tells you what to lift next time. This branch is the native Android version, written in Kotlin with Jetpack Compose. The original web app lives on the [`main`](../../tree/main) branch.

## Install

1. Open the [Releases](../../releases) page and download the newest `overload-0.x.apk`.
2. Open it on your Android phone. If asked, allow installs from your browser or file manager.
3. Later updates install over the top and keep your data.

Every push to the `kotlin` branch builds a new release automatically (0.1, 0.2, 0.3, …).

## Features

**Plans**
- Start from a template: **Jake's split**, **Push pull legs**, **Upper lower**, **Full body**, or **Build my own**.
- Edit any day: rename it, add exercises from a library of 60+ or type your own, group them into supersets, set rest time.

**Logging**
- Weight and reps for every set, with last week's numbers shown as a guide.
- Suggestions for next time, using double progression: add reps until you hit the top of the range, then add weight.
- New PRs are flagged as you lift.
- Optional reps in reserve (RIR) box.
- Swap an exercise for just today, add or remove sets, and keep notes per exercise and per workout.
- Plate calculator and warm-up sets.

**Rest timer and workout clock**
- Rest starts on its own after the last exercise in a round, with +15/−15, restart and stop buttons.
- Sound and vibration when rest ends.
- **Lock-screen rest alerts:** leave the app during rest and you get a notification when it's time for the next set.
- A workout clock with pause, and a summary when you finish: time, sets, volume and PRs.

**Heart rate**
- Connect Samsung Health (or any watch app) through Health Connect.
- After each workout you see average and max heart rate, plus a graph of your heart rate with your average marked.

**Progress and stats**
- Estimated one-rep max chart for every lift, with stall detection.
- A weekly view: sessions done, hard sets, volume, PRs, and sets per muscle against your plan.
- Volume for the last 8 weeks and your week streak.

**Notes, settings and backup**
- Coloured notes you can pin.
- Light, dark or automatic theme. lb or kg.
- Save a backup file and restore it, including backups from the web version.
- A **What's new** screen after each update.

All data stays on your phone.

## Build it yourself

Open this folder in Android Studio (Ladybug or newer), let Gradle sync, and run it on a phone or emulator.

- Min Android version: 8.0 (SDK 26). Heart rate needs Android 9 or newer.
- Compiles against SDK 36, targets SDK 35.
- Android Gradle plugin 8.10.1, Gradle 8.11.1, Kotlin 2.0.21, JDK 17.

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
| `Theme.kt`, `Components.kt` | Colours, plate graphics, shared UI pieces |

Data is saved as JSON in the app's private storage.

**Updating What's new:** edit `NEWS` in `News.kt` and add 1 to `NEWS_ID`. The screen then pops up once for everyone after they update.

### Differences from the web version

Not ported yet: badges and ranks, daily challenges, weekly quests, the post-workout muscle map, share cards, copy-a-day, streak freezes, daily snapshots, the welcome splash and the background art. Fonts are the system font. Add Archivo and Inter in `res/font` to match the web look.

## License

© 2026 Jake Scarpa. All rights reserved. See [LICENSE](LICENSE).
