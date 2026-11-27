# Overload

**Log every set, beat last week, watch the numbers climb.**

Overload is a workout log built around progressive overload. Pick a plan, log your weight and reps, and the app tells you what to lift next time. It runs in your phone's browser, works offline, and installs to your home screen like a normal app.

There's also a native Android app. It lives on the [`kotlin`](../../tree/kotlin) branch, and APKs are on the [Releases](../../releases) page.

## Install

Open the app's link on your phone, then add it to your home screen:

- **iPhone:** open it in **Safari** → Share → **Add to Home Screen**, then open Overload from that icon. Other iPhone browsers can't install it.
- **Android:** open it in Chrome → menu → **Install app** or **Add to Home screen**.

Use the home-screen icon from then on. Workouts logged in a normal browser tab are saved separately from the home-screen app.

## Features

**Plans and logging**
- Start from **Push pull legs**, **Upper lower**, **Full body**, or **Build my own**, then change any day or exercise.
- Exercise library or your own exercises, supersets, per-day rest times, and copying a day onto other days.
- Weight and reps for every set, with suggestions for next time and PRs flagged as you lift.
- Optional reps in reserve (RIR), a plate calculator, warm-up sets, today-only swaps, and notes with history per exercise.

**Rest timer and workouts**
- Rest timer that starts on its own, with sound, vibration and lock-screen alerts when rest ends.
- Workout clock and a post-workout breakdown: a muscle map of what you hit, sets per muscle and fun stats.

**Ranks, goals and challenges**
- Ranks from Iron to Olympian, with divisions, promotion trials, prestige stars and a rank-up moment.
- A strength rank for every lift based on your bodyweight, and muscle mastery from Bronze to Legend.
- Daily challenges, weekly quests and report cards, lift goals with a projected finish date, and quest chains.
- A 30-level season pass with plate colours, frames, auras and nameplates.
- Streak freezes, so one missed week doesn't break your streak.

**Crew**
- Post workouts, react, comment, and see who's lifting right now. Send hype to friends who are live.
- **The Arena:** a crew rank board with a podium, Rank Power, titles you can steal, rivals, lift boards and crew challenges. Stats are checked by the server.
- Monday recaps, crew levels and banners, monthly awards, Gym Buddies, and copying a friend's plan.

**Your data**
- Everything is saved on your phone first. A second copy is kept, and you can restore from the last 7 days.
- Backup files you can save and restore.
- Optional account (passkey or recovery code) to sync across devices.
- Light, dark or automatic theme. lb or kg.

## Files

| File | What it is |
|---|---|
| `index.html` | Page markup, and the list of scripts to load |
| `styles.css` | All styles |
| `js/01-core.js` | Constants, workout templates, app state, storage, helpers |
| `js/02-rendering.js` | Tab panes, `render()`, sheets, haptics, popups |
| `js/03-plates.js` | Plate drawing, share card, plate finishes, background art |
| `js/04-today.js` | Today screen, exercise picker, rest timer, logging |
| `js/05-plan.js` | Plan page and editor |
| `js/06-stats.js` | Progress, stats, notes, muscle map, lift ranks, goals |
| `js/07-rewards.js` | Badges, ranks, challenges, unlocks, locker, trophies, medals, holidays |
| `js/08-profile.js` | Profile, avatars, banners, crests, looks and effects |
| `js/09-crew.js` | Crew feed, chat, live crew, arena, raids, awards |
| `js/10-coach.js` | Coach (Overload AI) |
| `js/11-data.js` | Backups, trash, cloud sync, export, push alerts |
| `js/12-settings.js` | Settings, onboarding, welcome tour, privacy |
| `js/13-extensions.js` | Later versions' extensions to earlier functions, then all event wiring (order matters) |
| `js/14-startup.js` | Runs last and starts the app |
| `sw.js` | Service worker: offline caching and lock-screen rest alerts |
| `icon.png` | App icon |

The scripts are plain scripts loaded in number order and share their top-level names, so a function in one file can call one in any other. Code that runs while the page is loading can only use files loaded before it. When adding a new file, add it to `index.html` and to the `CODE` list in `sw.js` (so it works offline), and bump the cache version in `sw.js`.

When uploading through GitHub, upload the whole folder so `js/` and `styles.css` stay in step with `index.html`.

There's no build step. Serve the folder over HTTPS (GitHub Pages works) and open `index.html`. A copy served from a path containing `/overload-beta/` runs as the beta and shows its version as "beta".

## License

© 2026 Jake Scarpa. All rights reserved. See [LICENSE](LICENSE).
