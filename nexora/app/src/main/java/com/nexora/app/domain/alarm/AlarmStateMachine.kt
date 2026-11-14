package com.nexora.app.domain.alarm

/**
 * The alarm lifecycle, modelled explicitly.
 *
 * This exists because the original problem was state drift: a notification that outlives
 * its alarm, or a dismissal that stops sound but leaves the row `RINGING`. Every
 * transition below is idempotent and total, which is what makes repeated taps and stale
 * intents harmless.
 */
enum class AlarmPhase {
    /** Scheduled with the OS, not ringing. */
    Scheduled,

    /** Fired, ringing, notification posted. */
    Ringing,

    /** Snoozed; will ring again at [AlarmInstance.snoozeUntil]. */
    Snoozed,

    /** Dismissed by the user; sound stopped and notification removed. */
    Dismissed,

    /** Fired and finished without user input (timeout, or auto-dismissed). */
    Expired,

    /** Replaced by a newer instance; nothing to dismiss. */
    Superseded,
}

/** One firing of an alarm. Instances, not alarms, are what ring. */
data class AlarmInstance(
    val instanceId: String,
    val alarmId: Long,
    val triggerAt: Long,
    val phase: AlarmPhase,
    val firedLatencyMillis: Long = 0L,
    val snoozeUntil: Long? = null,
    val snoozeCount: Int = 0,
) {
    val isRingable: Boolean get() = phase == AlarmPhase.Ringing

    /** True when no further work is needed for this instance. */
    val isTerminal: Boolean
        get() = phase == AlarmPhase.Dismissed || phase == AlarmPhase.Expired || phase == AlarmPhase.Superseded
}

/** Result of asking the machine to apply an event. */
sealed interface AlarmTransition {
    data class Applied(val instance: AlarmInstance, val changed: Boolean) : AlarmTransition
    data class Snooze(val instance: AlarmInstance, val until: Long) : AlarmTransition
    data class Rejected(val reason: String) : AlarmTransition
}

/**
 * Pure, side-effect-free transition function.
 *
 * The service layer is responsible for *effects* (stop audio, cancel notification,
 * reschedule). This class only decides what the new state is, and whether anything
 * actually changed — which is how we make repeated taps free.
 */
object AlarmStateMachine {

    fun onFired(instance: AlarmInstance, nowMillis: Long): AlarmTransition {
        if (instance.isTerminal) return AlarmTransition.Rejected("instance already terminal")
        if (instance.phase == AlarmPhase.Ringing) {
            return AlarmTransition.Applied(instance, changed = false)
        }
        return AlarmTransition.Applied(
            instance.copy(
                phase = AlarmPhase.Ringing,
                firedLatencyMillis = (nowMillis - instance.triggerAt).coerceAtLeast(0L),
            ),
            changed = true,
        )
    }

    /**
     * The user dismissed (tap, swipe, or in-app stop). Safe to call any number of times
     * from any thread; only the first call reports `changed = true`.
     */
    fun onDismissed(instance: AlarmInstance, nowMillis: Long): AlarmTransition {
        if (instance.isTerminal) return AlarmTransition.Applied(instance, changed = false)
        return AlarmTransition.Applied(instance.copy(phase = AlarmPhase.Dismissed), changed = true)
    }

    /** Snooze. Refused when already terminal or when the snooze budget is spent. */
    fun onSnooze(
        instance: AlarmInstance,
        nowMillis: Long,
        snoozeMinutes: Int,
        maxSnoozes: Int,
    ): AlarmTransition {
        if (instance.isTerminal) return AlarmTransition.Rejected("instance already terminal")
        if (maxSnoozes in 1..instance.snoozeCount) {
            // Budget spent: treat as a dismissal instead of silently doing nothing.
            return AlarmTransition.Applied(
                instance.copy(phase = AlarmPhase.Dismissed),
                changed = true,
            )
        }
        val until = AlarmMath.snoozeTarget(nowMillis, snoozeMinutes)
        return AlarmTransition.Snooze(
            instance.copy(phase = AlarmPhase.Snoozed, snoozeUntil = until, snoozeCount = instance.snoozeCount + 1),
            until,
        )
    }

    /** Ring timeout / silent give-up. */
    fun onExpired(instance: AlarmInstance): AlarmTransition {
        if (instance.isTerminal) return AlarmTransition.Applied(instance, changed = false)
        return AlarmTransition.Applied(instance.copy(phase = AlarmPhase.Expired), changed = true)
    }

    /** A newer firing of the same alarm replaced this one. */
    fun onSuperseded(instance: AlarmInstance): AlarmTransition {
        if (instance.isTerminal) return AlarmTransition.Applied(instance, changed = false)
        return AlarmTransition.Applied(instance.copy(phase = AlarmPhase.Superseded), changed = true)
    }

    /**
     * Whether a notification tap for [instanceId] should be honoured.
     * Stale taps (instance already dismissed, or superseded by a newer firing) are
     * ignored — this is the fix for "tap stops the wrong alarm".
     */
    fun shouldHonourTappedNotification(
        tappedInstanceId: String,
        liveInstances: Map<String, AlarmInstance>,
    ): Boolean {
        val live = liveInstances[tappedInstanceId] ?: return false
        return !live.isTerminal
    }

    /**
     * Given all instances for an alarm, the newest non-terminal one. Used to decide which
     * instance a ringtone belongs to when the alarm somehow fires twice.
     */
    fun newestActive(instances: Collection<AlarmInstance>): AlarmInstance? =
        instances.filter { !it.isTerminal }.maxByOrNull { it.triggerAt }
}
