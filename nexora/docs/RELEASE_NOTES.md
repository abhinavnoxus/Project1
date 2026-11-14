# NEXORA 1.0.0

A personal time operating system for Android, built from scratch.

## What is in this release

- **NEXORA FLUID** glass design system: five materials (Crystal, Aurora, Obsidian, Frost,
  Holographic), adjustable transparency, blur, reflection, borders, corner radius, shadow
  depth, eight accent ramps, six ambient backdrops, typography scale, density, motion
  intensity and speed, haptics, light and dark themes.
- **NEXORA Studio**: live-preview editor for material, layout, motion and profiles, with
  save, duplicate, delete, export and import.
- **Time**: five clock faces, world clocks, DST-correct time-zone converter, stopwatch with
  laps, multiple simultaneous timers.
- **Alarms**: repeating per-day, one-shot, labels, ringtone preview, vibration, snooze
  interval and limit, fade-in, next-3-firings preview, and an **Alarm Health Centre**.
- **Planning**: agenda with read-only calendar access and conflict reporting, tasks,
  reminders, priorities, routines with completion logs.
- **Focus**: sessions with a break cycle and an honest record of what was completed.
- **Insights**: focus totals, weekly and monthly charts, streaks, schedule density, task
  completion, routine consistency. Empty until there is data.
- **Briefing**: an offline, rule-based daily summary where every line is labelled
  Fact, Derived or Suggestion.
- **Widget**: home-screen clock and next alarm.

## Alarm reliability

Dismiss is handled by a broadcast receiver, not an activity: tapping the notification stops
audio and vibration immediately, cancels that exact notification and updates state — with
the screen locked, the app closed, or the activity destroyed. Each firing has a unique
instance id, so stale or repeated taps are ignored rather than acting on the wrong alarm.

## Verified

- Kotlin compilation, debug and R8-minified release builds: **passing**.
- 79 JVM unit tests: **passing**.
- Android lint: **no blocking errors**.
- APK structure, permissions and components inspected with `aapt2`: see the CI comment.

## Not verified — read before relying on it

- No physical Realme Narzo 20 Pro and no emulator were available, so runtime behaviour,
  alarm firing on Realme UI, reboot recovery and frame timings are **not** confirmed on
  hardware. See `docs/LIMITATIONS.md`.
- Instrumented (Compose/Espresso, migration) tests are written but were **not executed**;
  they require a device.
- This APK is signed with the Android **debug** key so it installs for testing. It is not
  production-signed and not suitable for Play Store distribution. Re-sign with your own
  keystore (`README.md` §1).

## Privacy

The manifest declares **no INTERNET permission**; there is no analytics, crash reporter or
advertising dependency. Calendar access is optional and read-only. Everything is stored
locally.

## Install

```bash
adb install -r app-debug.apk
```
