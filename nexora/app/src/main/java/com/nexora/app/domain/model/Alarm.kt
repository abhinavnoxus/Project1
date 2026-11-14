package com.nexora.app.domain.model

import kotlinx.serialization.Serializable

/** How an alarm behaves after it fires. */
@Serializable
enum class AlarmKind { OneShot, Repeating, Timer, Reminder }

@Serializable
data class Alarm(
    val id: Long = 0L,
    val label: String = "Alarm",
    val hour: Int = 7,
    val minute: Int = 0,
    val repeat: WeekdayMask = WeekdayMask.None,
    val enabled: Boolean = true,
    /** Uri string of the ringtone; null means the app default. */
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val maxSnoozes: Int = 3,
    /** 0 = use the system's stream volume. */
    val customVolume: Int = 0,
    val fadeInSeconds: Int = 8,
    val kind: AlarmKind = AlarmKind.Repeating,
    /** For one-shot alarms: the exact epoch millis of the next fire. */
    val oneShotAt: Long? = null,
    val skipNext: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val time: TimeOfDay get() = TimeOfDay.of(hour, minute)

    /** True when this alarm can fire again without user action. */
    val isSelfRescheduling: Boolean get() = kind == AlarmKind.Repeating && !repeat.isEmpty
}
