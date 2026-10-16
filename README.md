# TalibulWaqt

Salat timer, Qibla compass and dhikr companion for Android, iOS and Desktop, built with Kotlin
Multiplatform and Compose Multiplatform. It is the mobile counterpart of
[NoorSalat](https://salat.mazhar.fi) and shows the same prayer times.

## Features

- **Prayer times that work in Finland and at the North Pole.** Praytimes-style solar maths with the
  ECFR / Islamic Cultural Centre of Finland high-latitude rules (nearest latitude 45°, one-seventh,
  middle of the night, angle-based) and polar day/night fallbacks, Islamic midnight and Qiyam.
  Calculation standards: Finland, ECFR, MWL, Umm al-Qura, ISNA, Egypt, Karachi; Standard or Hanafi Asr.
- **Live countdown** to the next prayer with the elapsed-period bar, plus GPS or preset locations
  (Helsinki → Utsjoki → Longyearbyen → North Pole, and world cities).
- **Adhan notifications** at every prayer: exact alarms on Android (reboot/time-zone aware), local
  notifications on iOS planned a week ahead.
- **Qibla compass** with true-north heading, Kaaba marker, turn-left/right guidance and the great-circle
  distance to Mecca; a static dial on desktop.
- **Dhikr & dua library** with Arabic, transliteration, meaning, virtue and hadith reference, searchable
  by category, with custom entries.
- **Personalized post-prayer routines** per prayer (shared list plus Fajr/Dhuhr/Asr/Maghrib/Isha
  overrides): add, reorder, set repetitions, launch as a tap-anywhere tasbeeh session.
- **Daily tracking** with progress bars for each prayer session and each dhikr, a daily goal and streak.

## Project layout

- [`/shared`](./shared/src) — all UI and logic. `commonMain` holds the models, prayer engine, Qibla maths,
  repository and Compose screens; `androidMain`, `iosMain` and `jvmMain` provide location, compass,
  notifications and storage for each platform.
- [`/androidApp`](./androidApp), [`/desktopApp`](./desktopApp), [`/iosApp`](./iosApp) — thin launchers.

## Running

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`)
- iOS: open [`/iosApp`](./iosApp) in Xcode and run (macOS only).

## Tests

- `./gradlew :shared:jvmTest` — engine, Qibla, repository and schedule tests on the JVM
- `./gradlew :shared:testAndroidHostTest` — the same suite on the Android host JVM
- `./gradlew :shared:iosSimulatorArm64Test` — on macOS
- `TALIBULWAQT_SCREENSHOT_DIR=out ./gradlew :shared:jvmTest --tests "*ScreenshotRenderTest*"` renders every
  screen to PNG without an emulator.
