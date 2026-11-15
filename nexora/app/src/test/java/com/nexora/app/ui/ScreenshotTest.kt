package com.nexora.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexora.app.design.AccentRamp
import com.nexora.app.design.AmbientBackdrop
import com.nexora.app.design.ClockFace
import com.nexora.app.design.GlassMaterial
import com.nexora.app.design.GlassQuality
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.HomeSection
import com.nexora.app.design.NexoraTheme
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import com.nexora.app.domain.model.WeekdayMask
import com.nexora.app.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * JVM screenshot tests.
 *
 * These exist because this project is built with no Android device and no emulator.
 * Robolectric's native graphics mode renders the real Compose tree through the real Skia
 * pipeline on the JVM, so what comes out of CI is the actual UI rather than an opinion
 * about it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val spec = GlassSpec(
        material = GlassMaterial.Crystal,
        transparency = 0.42f,
        accent = AccentRamp.Azure,
        ambient = AmbientBackdrop.AuroraVeil,
    )

    private val now: ZonedDateTime =
        ZonedDateTime.of(2026, 11, 14, 21, 42, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Test
    fun homeScreen() {
        compose.setContent {
            NexoraTheme(spec = spec, quality = GlassQuality.Balanced) {
                HomeScreen(
                    now = now,
                    spec = spec,
                    sectionOrder = HomeSection.entries,
                    nextAlarm = sampleAlarm(),
                    todayEvents = sampleEvents(),
                    priorities = sampleTasks(),
                    reminders = sampleReminders(),
                    routines = emptyList(),
                    completedRoutineIds = emptySet(),
                    focusActive = false,
                    focusLabel = "Focus",
                    focusElapsedMinutes = 0L,
                    todayFocusMinutes = 48L,
                    calendarGranted = true,
                    clockFace = ClockFace.Halo,
                    onOpenAlarms = {},
                    onOpenAlarmDetail = {},
                    onOpenCalendar = {},
                    onOpenTimer = {},
                    onStartFocus = {},
                    onOpenFocus = {},
                    onCompleteRoutine = { _ -> },
                    onToggleTask = { _, _ -> },
                    onGrantCalendar = {},
                    onOpenInsights = {},
                    onAddTask = {},
                    onOpenRoutines = {},
                    onOpenReminders = {},
                    onOpenBriefing = {},
                    onOpenPlanner = {},
                )
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/home.png")
        publishToRepo("build/outputs/roborazzi/home.png")
    }

    /**
     * Copies the rendered PNG into `nexora/docs/screenshots/` so it lands in Git and can
     * actually be looked at by a human. CI commits it; locally it is harmless.
     */
    private fun publishToRepo(path: String) {
        runCatching {
            val moduleDir = java.io.File(System.getProperty("user.dir") ?: ".")
            val src = java.io.File(moduleDir, path)
            if (!src.exists()) {
                println("SCREENSHOT MISSING at $src")
                return@runCatching
            }
            val configured = System.getProperty("nexora.screenshotDir")
            val dest = if (configured != null) java.io.File(configured) else java.io.File(moduleDir, "../../docs/screenshots")
            dest.mkdirs()
            val out = java.io.File(dest, src.name)
            src.copyTo(out, overwrite = true)
            println("SCREENSHOT PUBLISHED to ${out.absolutePath} (${out.length()} bytes, exists=${out.exists()})")
        }.onFailure { println("SCREENSHOT PUBLISH FAILED: ${it.message}") }
    }

    private fun sampleAlarm(): Alarm = Alarm(
        id = 1L,
        label = "Morning run",
        hour = 6,
        minute = 30,
        repeat = WeekdayMask.EveryDay,
        enabled = true,
        ringtoneUri = null,
        vibrate = true,
        snoozeMinutes = 10,
        maxSnoozes = 3,
        fadeInSeconds = 10,
        kind = AlarmKind.Repeating,
    )

    private fun sampleEvents(): List<ScheduleEvent> = listOf(
        ScheduleEvent(
            id = 1L,
            title = "Design review",
            startMillis = now.withHour(10).withMinute(0).toInstant().toEpochMilli(),
            endMillis = now.withHour(11).withMinute(0).toInstant().toEpochMilli(),
            allDay = false,
            location = "Studio 2",
            calendarName = "Work",
            calendarColor = null,
        ),
        ScheduleEvent(
            id = 2L,
            title = "Dentist",
            startMillis = now.withHour(16).withMinute(30).toInstant().toEpochMilli(),
            endMillis = now.withHour(17).withMinute(15).toInstant().toEpochMilli(),
            allDay = false,
            location = "Civil Lines",
            calendarName = "Personal",
            calendarColor = null,
        ),
    )

    private fun sampleTasks(): List<Task> = listOf(
        Task(id = 1L, title = "Finish the alarm engine write-up", kind = TaskKind.Priority, priority = 1),
        Task(id = 2L, title = "Reply to Priya", kind = TaskKind.Priority, priority = 2),
    )

    private fun sampleReminders(): List<Task> = listOf(
        Task(id = 3L, title = "Take the tablets", kind = TaskKind.Reminder),
    )
}
