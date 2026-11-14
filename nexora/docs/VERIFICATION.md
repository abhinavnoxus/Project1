# NEXORA — verification record

Every number below was produced by a real CI run. Nothing here is estimated. The run ID and
the CI issue are cited so the results can be audited.

- **Environment** — GitHub Actions `ubuntu-latest`, JDK 17 (Temurin), Android SDK with
  platforms 34–36 and build-tools 36.x, Gradle 8.9 via the committed wrapper.
- **Target hardware for the product** — Realme Narzo 20 Pro (MediaTek Helio G95, Mali-G76
  MC4, 6 GB LPDDR4x, 6.5" 1080×2400 IPS @ 90 Hz, shipped Android 10 / Realme UI 2.0,
  upgradeable to Android 12).
- **Hardware available to this build** — none. No device, no emulator. See
  `LIMITATIONS.md`.

## Build results

| Stage | Result |
|---|---|
| `:app:assembleDebug` | **pass** |
| `:app:testDebugUnitTest` | **pass** — 79 tests, 0 failures, 0 errors, 0 skipped |
| `:app:lintDebug` | **pass** — no blocking errors |
| `:app:assembleRelease` (R8 minified, resources shrunk) | **pass** |

## Test breakdown

| Suite | Tests | Time |
|---|---|---|
| `domain.AlarmMathTest` | 20 | 0.012 s |
| `design.GlassSpecTest` | 15 | 0.018 s |
| `domain.AlarmStateMachineTest` | 13 | 0.004 s |
| `domain.TimeOfDayTest` | 9 | 0.009 s |
| `domain.InsightMathTest` | 8 | 0.004 s |
| `domain.ai.BriefingTest` | 6 | 0.042 s |
| `design.ProfileSetTest` | 6 | 0.001 s |
| `data.SchedulerContractTest` | 2 | 0.100 s |
| **Total** | **79** | **~0.2 s** |

## Artefacts

| File | Bytes | SHA-256 |
|---|---|---|
| `app-debug.apk` | 18,163,076 | `d5f5b5a24d42f66a6a4aa370743ff0c9d2c5554cc1bb7d82eddb3cb561fe9090` |
| `app-release.apk` | 1,487,513 | `b2fdd46473b2d32dca3db8ce3814ada7c07140315a21c5478ff7dbbb6c658ec9` |

Published release: **`nexora-v1.0.0`** with `app-debug.apk`, `app-release.apk` and
`SHA256SUMS.txt` attached.

The release APK is 1.49 MB against 18.16 MB for debug — the ratio R8 + resource shrinking
achieves on a Compose app. It is signed with the **debug** key because no production
keystore exists in this environment; `README.md` §1 explains how to supply one.

## APK inspection (aapt2, from CI)

```
package: com.nexora.app.debug  versionCode=1  versionName=1.0.0-debug
compileSdkVersion=35  minSdkVersion=26  targetSdkVersion=35
application-label:'NEXORA'
launchable-activity: com.nexora.app.MainActivity
```

Permissions actually present in the packaged manifest:

```
SCHEDULE_EXACT_ALARM          USE_EXACT_ALARM
RECEIVE_BOOT_COMPLETED        VIBRATE
WAKE_LOCK                     FOREGROUND_SERVICE
FOREGROUND_SERVICE_MEDIA_PLAYBACK
POST_NOTIFICATIONS            USE_FULL_SCREEN_INTENT
READ_CALENDAR
```

**`android.permission.INTERNET` is absent.** The privacy claim in the README is therefore
verified against the built artefact, not just the source manifest.

Components in the packaged manifest: 1 launcher activity, 1 foreground service
(`AlarmRingingService`), 3 app receivers, 2 platform-provided activities, 1
platform-provided provider, 1 appwidget receiver.

## Defects found and fixed during this work

| Defect | Impact | Fix |
|---|---|---|
| `WeekdayMask` used the 1-based `DayOfWeek.value` as a bit index | **Sunday alarms could never fire.** `EveryDay` = bits 0–6, Sunday was bit 7. Caught by a DST regression test. | 0-based `bitFor(day) = 1 shl (day.value - 1)`, plus 3 dedicated tests |
| `WeekdayMask.label()` compared hardcoded bit patterns | "Weekdays"/"Weekends" labels wrong | Compare against masks built through `of(...)` |
| DST spring-forward resolution walked from the wrong origin | An alarm could resolve to the previous day | Bounded minute-forward walk from local midnight |
| `GlassSpec.surfaceAlpha` clamped to 0.97 | A fully opaque pane never reached 1.0 | Clamp to 0.05–1.0 |
| `InsightMathTest` fixture added minutes as hours | False failure in the test, not the product | Fixture corrected with `plusMinutes` |
| CI workflow YAML: zero-indent heredoc in a block scalar | Workflow failed to parse (no jobs) | Report generation moved into `scripts/ci-report.sh` |
| Gradle wrapper could not be generated offline | No build possible | Committed a template wrapper, with a CI regeneration fallback |

## What is *not* verified

Stated plainly, because the acceptance criteria ask for honesty over optimism:

- **On-device runtime behaviour.** No Realme Narzo 20 Pro and no emulator were available.
- **Performance measurements.** No frame-time, jank, startup or battery numbers were
  captured, so none are claimed. The rendering-profile policy is implemented and unit
  tested; its real-world effect on a Helio G95 is unmeasured.
- **Alarm firing end-to-end** on Realme UI: reboot recovery, OEM process killing, exact
  alarm permission behaviour, full-screen intent on a locked screen. The state machine and
  scheduling arithmetic that underpin these are unit tested; the platform integration is not.
- **Instrumented tests.** `DatabaseMigrationTest`, `AlarmIntentTest` and `AppLaunchTest` are
  written and compile-checked by the debug build, but require a connected device
  (`connectedDebugAndroidTest`) and were **not executed**.
- **Blur on this specific device.** The Narzo 20 Pro maxes out at Android 12 (API 31), so it
  *may* use the `RuntimeShader` tier; the gradient fallback is what the app guarantees on
  API 26–30.
