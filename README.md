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
| `index.html` | The whole app: layout, styles and code in one file |
| `sw.js` | Service worker: offline caching and lock-screen rest alerts |
| `icon.png` | App icon |

There's no build step. Serve the folder over HTTPS (GitHub Pages works) and open `index.html`. A copy served from a path containing `/overload-beta/` runs as the beta and shows its version as "beta".

## License

© 2026 Jake Scarpa. All rights reserved. See [LICENSE](LICENSE).
