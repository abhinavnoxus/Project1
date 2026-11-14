# NEXORA — known limitations

Written to be read before the release is trusted. Nothing here is hidden in the UI.

## Not verified on a physical Realme Narzo 20 Pro

NEXORA was developed in an environment with no Android device and no emulator. All build,
unit-test, lint and release verification ran on GitHub Actions (`ubuntu-latest`, JDK 17,
Android SDK platforms 34–36, build-tools 36.x).

**Verified by execution**

- Kotlin compilation of the full app (debug and release).
- 79 JVM unit tests.
- Android lint (no blocking errors).
- R8-minified release build.
- APK structure: manifest, permissions, components, dex, resources (see the CI
  verification comment).

**Not verified, and therefore not claimed**

- Actual runtime behaviour on the Narzo 20 Pro (Realme UI 2.0/3.0, Android 10–12).
- Frame-time / jank measurements on Mali-G76 MC4. The performance policy is implemented and
  the tier selection is unit tested, but no on-device profile was captured, so no FPS or
  jank figures are reported anywhere.
- Alarm firing after a real device reboot, with the screen locked, after OEM process
  killing. The code paths exist and are unit tested at the state-machine and scheduling
  level; end-to-end behaviour on Realme UI is unverified.
- Foreground service start on Android 12+ under Realme's background restrictions.
- Haptics, audio focus and ringtone playback on real hardware.
- Instrumented (Espresso/Compose) UI tests were not run: no emulator or device was
  available to the build environment.
- Notification dismissal by tap was verified only through the state machine and the
  receiver wiring, not by driving a real notification shade.

To verify on the device:

```bash
adb install -r app-debug.apk
adb shell dumpsys deviceidle force-idle          # then wait for an alarm
adb reboot                                        # confirm re-arming
adb shell cmd notification post ...               # or just let one fire
```

## Deliberate functional boundaries

- **Blur on Android 10/11.** The Narzo 20 Pro shipped with Android 10 and received up to
  Android 12. `RenderEffect` blur exists only from API 31. On API 26–30 NEXORA renders its
  cached-gradient glass instead. It is designed to look deliberate, but it is not true
  backdrop blur.
- **Calendar is read-only.** There is no code path that writes to the calendar provider.
- **Intelligence is rule-based, offline.** It is not a language model. It produces a
  structured briefing from local rules and always labels suggestions.
- **Profile import** accepts a NEXORA profile JSON chosen through the system document
  picker; there is no paste-a-URL import, by design (no network).
- **Focus "extend"** updates the persisted `plannedMinutes` of the running session, but the
  ring's progress bar is drawn against the original start instant, so the visual arc
  restarts from a lower fill after an extension.
- **Timers** are persisted as absolute instants and restored after process death.
  **The stopwatch is still process-local**: it survives backgrounding but not a process
  kill, because a stopwatch has no meaningful wall-clock meaning across a restart.
- **Widget** refreshes on a single minute-boundary alarm; Realme UI may batch that wake-up,
  so the widget can lag by up to a minute under Doze.
- **Locale.** Only `en` resources are shipped.
- **Accessibility.** Content descriptions and touch targets are provided throughout, and
  typography scales with the user's setting, but no TalkBack walkthrough was performed.

## Release signing

The CI release build is signed with the debug keystore because no production keystore is
available in this environment. The APK installs and runs, but **it is not suitable for
Play Store distribution** until re-signed with a real key (see README §1). This is stated
here so the artifact is not mistaken for a production-signed build.
