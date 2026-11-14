# NEXORA — a personal time operating system

NEXORA is a native Android application built from scratch: an alarm-accurate time
environment, a planner, a focus tool and an insights engine, wrapped in an original
liquid-glass design language called **NEXORA FLUID**.

```
Application ID   com.nexora.app
Version          1.0.0 (versionCode 1)
Min SDK          26 (Android 8.0)
Target/compile   35 (Android 15)
Language         Kotlin 2.0.21
UI               Jetpack Compose (BOM 2024.10.01), Material 3
Persistence      Room 2.6.1 (SQLite) + DataStore Preferences
Architecture     single-Activity Compose, MVVM with a pure-Kotlin domain layer
Primary target   Realme Narzo 20 Pro (Helio G95, Mali-G76 MC4, 6 GB, 1080×2400 @ 90 Hz)
```

NEXORA shares no branding, package identity, resources or database with any other
project in this repository. It is a new application.

---

## 1. Build from source

```bash
cd nexora
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # 79 JVM unit tests
./gradlew :app:lintDebug              # Android lint
./gradlew :app:assembleRelease        # minified release build
```

Outputs:

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
app/build/outputs/mapping/release/mapping.txt
```

### Signing

Release builds are signed with an **unsigned-dev fallback** unless signing material is
supplied through the environment:

```bash
export NEXORA_KEYSTORE=/secure/path/nexora-release.jks
export NEXORA_KEYSTORE_PASSWORD=...
export NEXORA_KEY_ALIAS=nexora
export NEXORA_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

No keystore, password or key ever lives in this repository. When the variables are absent
the release build is signed with the debug key so the artifact remains installable — it is
recorded as such in the build report rather than silently claimed as production-signed.

### Installing

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build installs alongside a release build (`com.nexora.app.debug`).

---

## 2. Architecture

```
com.nexora.app
├── design/                 NEXORA FLUID: materials, GlassSurface renderer,
│                           InterfaceProfile model, InterfaceRepository (DataStore),
│                           NexoraTheme, Motion engine
├── domain/                 Pure Kotlin. No Android imports except java.time.
│   ├── model/              TimeOfDay, WeekdayMask, Alarm, Task, Routine, FocusSession
│   ├── alarm/              AlarmMath (scheduling arithmetic), AlarmStateMachine (phases)
│   ├── insight/            InsightMath (all analytics)
│   └── ai/                 Briefing (offline, rule-based, no network)
├── data/                   Room entities, DAOs, migrations, repositories, mappers
├── alarm/                  Android alarm layer: Scheduler, Receiver, Controller,
│                           Ringer, RingingService, BootReceiver
├── notify/                 NotificationHelper, ReminderReceiver
├── platform/               DeviceProfile, CalendarBridge (read-only), PowerGuidance, widget
├── ui/                     Compose screens + one app-scoped ViewModel
└── MainActivity.kt         single Activity, intent entry points
```

Why the domain layer is Android-free: the alarm rules, analytics and briefing are the
parts most likely to be wrong, so they are the parts that run in a plain JVM test
(`app/src/test`). No Robolectric, no emulator, no device needed to prove them.

---

## 3. NEXORA FLUID

Five materials — **Crystal, Aurora, Obsidian, Frost, Holographic** — each a recipe
(light angle, gradient stops, rim light, specular colour, shadow tint). The user's sliders
modify the recipe rather than replace it, so every material stays coherent from fully
opaque to nearly clear.

The renderer (`design/GlassSurface.kt`) picks a **tier per pane**:

| Tier | When | Cost |
|---|---|---|
| `Shader` | Ultra Glass on API 31+ | one AGSL `RuntimeShader` per material |
| `Ultra` | Ultra Glass on API < 31 | cached gradients + refraction ring |
| `Balanced` | default | cached linear gradients + rim stroke |
| `Efficient` | low-RAM or user-selected | single translucent fill + hairline |

There is **no full-screen animated blur anywhere**. The ambient backdrop is a static
gradient unless motion is enabled, and the only `RenderEffect` blur is a one-shot on
sheet content, skipped entirely below API 31 (the Narzo 20 Pro on Android 10/11 takes the
gradient path, which is why the fallback is designed rather than incidental).

Customisable: material, transparency, blur, reflection, border, corner radius, shadow
depth, accent (8 ramps), backdrop (6), typography scale, density (3), motion intensity,
motion speed, haptics, sound, dark/light. Five built-in profiles plus unlimited user
profiles with save / duplicate / rename / delete / export / reset.

---

## 4. Alarm reliability

The requirement was that tapping an active alarm notification must stop audio and
vibration **immediately**, with the activity destroyed, the screen locked, or the process
having been restarted.

How that is guaranteed:

1. **Instances, not alarms, ring.** Each firing is an `AlarmInstance` identified by
   `"<alarmId>@<triggerEpochMillis>"`. Notifications are posted under that instance id, so
   two simultaneous alarms are two independent notifications.
2. **Dismiss is a broadcast, not an activity.** `AlarmReceiver` handles
   `ACTION_DISMISS` and calls `AlarmController.dismiss`, which calls `silenceNow()`
   *before* any database or coroutine work.
3. **`AlarmController` is the single writer.** Receiver, notification action, in-app stop
   and the service timeout all funnel through one mutex-guarded object.
4. **`AlarmStateMachine` is total and idempotent.** Dismissing twice returns
   `changed = false`; dismissing a terminal instance is a no-op; a stale or superseded
   instance is rejected. Those rules are unit tested (13 tests).
5. **Startup reconciliation.** Anything still marked `Ringing` when the process starts is
   reconciled to `Expired`, so the UI can never show a phantom alarm.
6. **Bounded ringing.** The service enforces a hard timeout (10 min, 2 min for timers) so a
   stuck ring cannot flatten the battery.

Covered scenarios: foreground/background, destroyed activity, process death (state lives
in SQLite), locked screen, repeated taps, multiple alarms, stale intents, snooze with a
snooze budget, repeating alarms, reboot (`BootReceiver`), time-zone and clock changes
(`TIMEZONE_CHANGED`, `TIME_SET`), permission revocation
(`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`).

The **Alarm Health Centre** reports notification permission, channel state, exact-alarm
access, battery-optimisation state, next alarm, ringtone readability, last dismiss latency
and the Realme UI steps that matter on this device.

---

## 5. Features

**Time** — live clock with 5 faces, analog and digital, world clocks, DST-correct
time-zone converter, stopwatch with laps, multiple simultaneous countdown timers
(pause/resume/cancel, survives backgrounding as absolute instants).

**Alarms** — repeating with per-day masks, one-shot, custom labels, ringtone selection and
preview, vibration, snooze interval and limit, fade-in, live "next 3 firings" preview.

**Calendar** — optional read-only access, agenda timeline, conflict *reporting* (never
auto-resolution), task overlay.

**Planning** — tasks, reminders and priorities; routines with per-day schedules and
completion logs; focus sessions with a break-cycle suggestion.

**Insights** — focus totals, weekly and monthly charts, streaks, schedule density, task
completion and routine consistency. Empty until data exists; no fabricated series.

**Intelligence** — an offline daily briefing built from local rules. Every sentence is
labelled Fact, Derived or Suggestion. Nothing is created, moved or deleted automatically.

**Widget** — home-screen clock with next alarm. Refreshes on a single minute-boundary
alarm while a widget exists and cancels it when the last one is removed.

---

## 6. Privacy

- **No `INTERNET` permission is declared.** The app is structurally incapable of sending
  data anywhere. There is no analytics SDK, crash reporter or ad library in the
  dependency list.
- Calendar access is optional and **read-only** — no insert, update or delete path exists
  against the calendar provider.
- All data is local: a Room database plus one DataStore preferences file.
- Permissions requested: notifications, exact alarms, boot completed, vibrate, wake lock,
  foreground service (media playback), and calendar only when the user opts in. Each is
  explained in-app at the point it is requested.

---

## 7. Performance

Policy is encoded, not claimed:

- `DeviceProfile.detect()` reads the real device facts (cores, RAM, low-RAM flag, refresh
  rate, SDK level, Realme UI) and `recommendedQuality()` maps them to a tier. Users can
  override.
- Three rendering tiers; expensive effects are per-pane and degrade independently.
- Only *one* looping animation exists in the app (the ambient backdrop), and it stops when
  motion is disabled, the quality tier is Efficient, or a modal is open.
- Ring fill, timers and the stopwatch derive from absolute instants — accuracy does not
  depend on frame rate, and they cost one recomposition per tick, not a frame loop.
- Room indexes back every hot query (`alarm_instances.triggerAt`, `tasks.dueAt`,
  `routine_logs(routineId, dayEpoch) unique`, `focus_sessions.startedAt`).
- Startup work (channels, reconciliation, orphan cleanup) runs on an IO dispatcher; nothing
  blocks the first frame.

Measured numbers are in `docs/VERIFICATION.md`. Anything not measured on the physical
Narzo 20 Pro is stated as unverified rather than estimated.

---

## 8. Testing

```bash
./gradlew :app:testDebugUnitTest
```

79 JVM tests across:

| Suite | Covers |
|---|---|
| `AlarmMathTest` (20) | next-trigger rules, weekday masks, one-shot, DST gaps and overlaps, zone changes, month/year/leap rollover, snooze clamping, request-code stability, grace window |
| `AlarmStateMachineTest` (13) | every phase transition, idempotent dismissal, stale-notification rejection, snooze budget, multi-alarm coexistence |
| `TimeOfDayTest` (9) | formatting, parsing, midnight wrap, circular distance, weekday mask arithmetic (including the Sunday-bits regression) |
| `GlassSpecTest` (15) | material ids, opacity clamping, motion scaling, reduced motion, hostile persisted values, tier selection, profile completeness |
| `ProfileSetTest` (6) | active-profile resolution, fallbacks, preset validity |
| `InsightMathTest` (8) | focus totals, per-day grouping, streaks, density, consistency, empty-state honesty |
| `BriefingTest` (6) | offline briefing facts vs suggestions, conflict reporting |
| `SchedulerContractTest` (2) | repository/scheduler interaction |

CI runs assembleDebug → unit tests → lint → assembleRelease and publishes the result table
to a GitHub issue, including failing assertions. Android instrumented tests are not part of
the pipeline: no emulator or physical device is attached to the build environment, and the
report says so instead of implying they ran.

---

## 9. Repository layout

```
nexora/                  the application
  app/src/main/java/…    source
  app/src/test/java/…    JVM tests
  app/schemas/           exported Room schemas (migration source of truth)
  docs/                  verification, limitations, changelog
.github/workflows/       nexora-ci.yml
```

Other branches, tags and releases in this repository are untouched by NEXORA's work.
