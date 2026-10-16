# Notification sounds for iOS

These are the opening 30 seconds of each bundled adhan (plus the synthesised tone), encoded as
mono 22.05 kHz IMA4 CAF, which is one of the formats `UNNotificationSound` accepts. iOS refuses
notification sounds longer than 30 s and only looks for them in the main bundle, so the full
recordings under the shared module's `composeResources/files/adhan` cannot be used directly.

`IosNotificationService` picks the file by name from `AdhanSound.iosNotificationClip`
(`UNNotificationSound.soundNamed("adhan_makkah_haram.caf")` and so on); "Beep" keeps the default
sound.

**These files are not yet in the Xcode project.** On a Mac, add the `Sounds` folder to the iosApp
target (File → Add Files to "iosApp"…, tick "Copy items if needed" off and "Create groups" on, and
make sure the target's *Copy Bundle Resources* phase lists every `.caf`). Until then the
notification falls back to the default sound.

Provenance and licences for each recording are in
`shared/src/commonMain/composeResources/files/adhan/CREDITS.md`.
