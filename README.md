# Overload (Kotlin / Jetpack Compose port)

Native Android port of the Overload web app (index.html). Open this folder in Android Studio
(Ladybug or newer), let Gradle sync, and run on a device or emulator (minSdk 26).

## Layout
- `Data.kt`            serializable models (plan, sessions, sets, notes, settings)
- `Plans.kt`           exercise library + the 5 plan templates
- `Logic.kt`           e1RM, PR detection, double-progression suggestions, stall check, plate calc, warm-ups, weekly stats
- `OverloadViewModel`  all state + actions (logging, rest timer, workout clock, plan edits, notes, backup/restore)
- `TodayScreen.kt`     week rack, hero card, exercise blocks, plate/warm-up/swap, exercise picker
- `OtherScreens.kt`    Progress (lift list + chart), Stats, Notes
- `PlanSettings.kt`    Plan editor, Settings, onboarding
- `MainActivity.kt`    scaffold, bottom nav, floating rest timer

Data is stored as JSON in the app's private files dir. Settings > Restore can also import a backup file
exported from the web version.

Not ported: welcome splash, share-card image, decorative background art, what's-new sheet, service worker
(not needed in a native app). Fonts are the system font (add Archivo/Inter in res/font to match the web look).
