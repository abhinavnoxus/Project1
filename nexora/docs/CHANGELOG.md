# NEXORA changelog

All versions follow [SemVer](https://semver.org). NEXORA's versioning is independent of
every other project in this repository.

## 1.0.0 — first release

### Application identity

- New application: `com.nexora.app`, name **NEXORA**, versionCode 1 / versionName 1.0.0.
- New adaptive icon (liquid orb mark), new palette, new typography ramp.
- No shared package, resource, database or asset with any other project in the repo.

### NEXORA FLUID design system

- Five glass materials: Crystal, Aurora, Obsidian, Frost, Holographic, each a full recipe
  (light angle, gradient stops, rim light, specular, shadow tint).
- Per-pane rendering ladder: AGSL `RuntimeShader` (Ultra, API 31+), cached-gradient Ultra
  (API < 31), Balanced, and Efficient. Blur is never applied to a live backdrop.
- Adjustable transparency, blur, reflection, border, corner radius, shadow depth,
  8 accent ramps, 6 ambient backdrops, typography scale, 3 densities, motion intensity,
  motion speed, haptics, sound, light/dark.
- Five built-in interface profiles, plus unlimited user profiles: save, duplicate, delete,
  activate, export to JSON, reset to default.

### NEXORA Studio

- Live preview rendered by the real glass renderer.
- Home section reordering, hiding and restoring; clock-face selection; navigation style and
  labels; motion profiles; haptics and sound; profile management and backup.

### Motion engine

- One easing family, all durations derived from the user's intensity/speed.
- Reduced motion collapses every transition to 0 ms.
- Press physics scaled by motion intensity; entrance choreography; No animation ever gates
  an alarm action.

### Time environment

- Five clock faces (Halo, Meridian, Sundial, Minimal, Epoch), analog and digital.
- World clocks with a DST-correct converter.
- Stopwatch with laps; multiple simultaneous countdown timers with pause/resume/cancel.
- Alarm scheduling with per-day masks, one-shot alarms, labels, ringtone selection and
  preview, vibration, snooze interval and limit, fade-in, next-3-firings preview.
- Agenda with read-only calendar access and conflict reporting.
- Tasks, reminders, priorities, routines with completion logs, focus sessions.
- Insights: focus totals, weekly/monthly charts, streaks, schedule density, task
  completion, routine consistency.
- Offline intelligence briefing labelled Fact / Derived / Suggestion.
- Home-screen widget with a single minute-boundary refresh.

### Alarm reliability

- Alarm-instance identity (`alarmId@triggerMillis`) end to end.
- Dismiss handled by a broadcast receiver; audio stops before any database work.
- One mutex-guarded controller for receiver, notification action, in-app stop and timeout.
- Idempotent, total state machine; stale and superseded instances rejected.
- Startup reconciliation of orphaned `Ringing` rows.
- Strict-mode-grade PendingIntents: explicit, immutable, unique request codes.
- Restore on boot, package replace, time-zone change and exact-alarm permission change.
- Alarm Health Centre reporting every dependency plus Realme UI-specific guidance.

### Performance

- Device capability probe drives the recommended rendering tier; user override supported.
- Only one looping animation in the app, and only when motion is enabled.
- Room indexes on every hot query; no destructive migration fallback.

### Privacy

- No `INTERNET` permission declared; no analytics, crash-reporting or ad dependency.
- Calendar access is optional and read-only.

### Fixed during development

- `WeekdayMask` used the 1-based `DayOfWeek.value` as a bit index, so Sunday landed on bit 7
  outside every preset mask — **Sunday alarms could never fire**. Caught by the DST
  regression test and fixed with a 0-based `bitFor()`, plus three dedicated tests.
- `WeekdayMask.label()` compared against hardcoded bit patterns for "Weekdays"/"Weekends"
  that were wrong for the same reason.
- DST spring-forward resolution could land on the wrong day; replaced with a bounded
  minute-forward walk from local midnight.
- `GlassSpec.surfaceAlpha` clamped to 0.97, so a fully opaque pane never reached 1.0.
- `InsightMathTest` fixture added minutes as *hours*; the production code was correct.

### Known limitations

See `docs/LIMITATIONS.md`.
