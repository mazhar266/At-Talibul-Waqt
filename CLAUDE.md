# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Adhan (repository folder `TalibulWaqt`) is a Kotlin Multiplatform + Compose Multiplatform salat (prayer) app for Android, iOS and Desktop (JVM). It is the mobile companion to the owner's NoorSalat web app (https://salat.mazhar.fi) and deliberately mirrors it: same prayer-time algorithm and Finnish/ECFR high-latitude rules, same dhikr library ids, same per-prayer adhkar routines and daily recitation tracking. Package/namespace is `fi.mazhar.prayer.adhan`.

## Commands

Run from the repo root. In PowerShell use `.\gradlew.bat`; in Git Bash `./gradlew`. If `java` is not on PATH, point Gradle at Android Studio's JDK first: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (Git Bash) — the Gradle daemon itself is auto-provisioned (JDK 21, foojay).

- Android debug APK: `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/`
- Desktop run: `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`)
- Desktop installer for the current OS: `./gradlew :desktopApp:packageDistributionForCurrentOS`
- iOS: open `iosApp/` in Xcode and run there (macOS only; `iosMain` cannot be compiled or tested on Windows)

Tests live in `shared` only (`commonTest` runs under each platform's task):

- Desktop/JVM: `./gradlew :shared:jvmTest`
- Android host (local JVM): `./gradlew :shared:testAndroidHostTest`
- iOS simulator: `./gradlew :shared:iosSimulatorArm64Test` (macOS only)
- Single class/method: `./gradlew :shared:jvmTest --tests "fi.mazhar.prayer.adhan.prayer.PrayerTimeCalculatorTest"` (append `.methodName`; same flag works for `testAndroidHostTest`)
- UI screenshots without an emulator: `ADHAN_SCREENSHOT_DIR=/some/dir ./gradlew :shared:jvmTest --tests "fi.mazhar.prayer.adhan.ScreenshotRenderTest"` renders every tab plus the tasbeeh session to PNG via `ImageComposeScene` (`shared/src/jvmTest/.../ScreenshotRenderTest.kt`). It is a no-op when the env var is unset.

No ktlint/detekt is configured; Android lint is `./gradlew :androidApp:lint`.

## Toolchain

- Gradle 9.5 wrapper, AGP 9, Kotlin 2.4, Compose Multiplatform 1.12, kotlinx-datetime 0.7 (`kotlin.time.Instant`/`Clock`; `ExperimentalTime` is opted in module-wide), kotlinx-serialization, coroutines. compileSdk/targetSdk 37, minSdk 24, JVM target 11.
- All versions live in `gradle/libs.versions.toml`; add dependencies there and reference `libs.*`.
- Configuration cache and build cache are on; custom build logic must stay configuration-cache compatible.
- Material icon fonts are not a dependency (CMP 1.12's material3 no longer pulls them): icons are drawn with `Canvas` in `ui/components/Icons.kt`.

## Architecture

Three thin hosts around one shared module. `App()` in `shared/src/commonMain/.../App.kt` is the single UI entry point; `AppContent(viewModel, tab)` is the injectable variant used by screenshots/previews.

- `androidApp/` → `AdhanApplication` calls `initAdhanApp(app)` (gives shared code the Application context and tracks the foreground Activity), `MainActivity` does `setContent { App() }`.
- `desktopApp/` → `main.kt` opens a Compose `Window { App() }`.
- iOS → `iosMain/MainViewController.kt` wraps `App()`; `iosApp/.../ContentView.swift` embeds it. An Xcode build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (static framework `Shared`). `Info.plist` carries `NSLocationWhenInUseUsageDescription`.

Inside `shared/src/commonMain/kotlin/fi/mazhar/prayer/adhan/`:

- `model/` — pure data: `Prayer`, `CalculationMethod` (angles per convention), `HighLatitudeRule`, `GeoLocation` + `PresetLocations`, `PrayerTimes`; `Dhikr`/`DhikrCategory`, `PrayerSlot` (ALL + the five prayers), `RoutineEntry`/`Playlist`, `DailyRecord` with `progressFor()`; `UserSettings` (`adhanSound` plus an optional `fajrAdhanSound`, since the Fajr adhan has its own wording — always resolve a recording through `adhanSoundFor(prayer)`); `AdhanSounds` (bundled recordings + Tone + Beep, `fajr` flag on Fajr-worded recordings, credits in `composeResources/files/adhan/CREDITS.md`); `ReminderSchedule` (per-prayer `ReminderMode` ADHAN/TONE/BEEP/OFF with an optional weekend set — resolved per date by `NotificationPlanner` and again by `PrayerAlarmReceiver` when the alarm fires). `DefaultDhikrLibrary` holds the built-in adhkar (same ids as the web app) and the default per-prayer playlist.
- `prayer/` — `SolarMath` (praytimes-style declination/EoT/hour angle), `PrayerTimeCalculator` (see "Prayer-time rules" below), `DailySchedule` (yesterday/today/tomorrow timeline → current period, countdown, upcoming prayers).
- `qibla/Qibla.kt` — great-circle bearing/distance to the Kaaba with pole special-cases, 16-point compass labels, relative turn.
- `data/` — `KeyValueStore` (expect; SharedPreferences / NSUserDefaults / `~/.adhan/store.properties`) and `AppRepository`, the single source of truth exposing `StateFlow`s and persisting every mutation as JSON. A prayer without its own routine inherits `PrayerSlot.ALL`; editing it forks a copy (`updateRoutine`). Daily records are keyed by ISO date and pruned to 90 days.
- `platform/` — service interfaces plus `expect fun create…()` factories (functions, not expect classes): `LocationService`, `CompassService` (true heading; takes lat/lng for declination), `NotificationService` (with `planningHorizonDays`). `NotificationPlanner` turns settings into concrete `PrayerNotification`s and is shared with the Android receivers.
- `ui/` — `AppViewModel` (ticker, derived schedule/period flows, tasbeeh `SessionState`, reschedules notifications on location/settings changes), `theme/` (NoorSalat palette), `components/`, `screens/` (Prayers, Qibla, Routine, DhikrLibrary, Settings, `SessionOverlay`).

Platform actuals: `androidMain/platform/` (LocationManager via `LocationManagerCompat`, rotation-vector compass + `GeomagneticField`, `AlarmManager` exact alarms with `PrayerAlarmReceiver`/`BootReceiver` that re-plan 2 days ahead; runtime permissions go through the foreground activity's `activityResultRegistry` in `Permissions.kt`; manifest with permissions/receivers is `androidMain/AndroidManifest.xml`), `iosMain/platform/` (CoreLocation, `UNUserNotificationCenter` planning 7 days), `jvmMain/platform/` (unsupported stubs). iOS code has not been compiled here — verify on a Mac when touching it.

## Prayer-time rules (keep parity with the web app)

`PrayerTimeCalculator` must keep producing the same minutes as salat.mazhar.fi; `PrayerTimeCalculatorTest` pins reference values read from the site (Helsinki, Utsjoki, Longyearbyen, North Pole, Mecca on 2026-10-16). The deliberate quirks: Dhuhr and every other time are measured from solar noon + 2 min; above 50° latitude the chosen high-latitude rule is always applied (nearest-latitude 45° by default), not only when twilight is missing; during polar day/night the whole day shape is borrowed from 45° and centred on local noon; the method defaults to FINLAND (15°/15°) at ≥ 55°N and MWL elsewhere when the user has not chosen one. Umm al-Qura's 90-minute Isha is honoured everywhere except the middle-of-night and angle-based rules.
