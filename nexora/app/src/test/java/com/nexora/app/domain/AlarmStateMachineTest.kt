package com.nexora.app.domain

import com.nexora.app.domain.alarm.AlarmInstance
import com.nexora.app.domain.alarm.AlarmPhase
import com.nexora.app.domain.alarm.AlarmStateMachine
import com.nexora.app.domain.alarm.AlarmTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmStateMachineTest {

    private fun instance(phase: AlarmPhase = AlarmPhase.Scheduled, snoozes: Int = 0) = AlarmInstance(
        instanceId = "9@1700000000000",
        alarmId = 9L,
        triggerAt = 1_700_000_000_000L,
        phase = phase,
        snoozeCount = snoozes,
    )

    @Test
    fun `firing moves to ringing`() {
        val t = AlarmStateMachine.onFired(instance(), 1_700_000_000_500L)
        assertTrue(t is AlarmTransition.Applied)
        val applied = t as AlarmTransition.Applied
        assertEquals(AlarmPhase.Ringing, applied.instance.phase)
        assertEquals(500L, applied.instance.firedLatencyMillis)
        assertTrue(applied.changed)
    }

    @Test
    fun `firing an already ringing instance is a no-op`() {
        val t = AlarmStateMachine.onFired(instance(AlarmPhase.Ringing), 1_700_000_001_000L) as AlarmTransition.Applied
        assertEquals(AlarmPhase.Ringing, t.instance.phase)
        assertFalse("second fire must not report a change", t.changed)
    }

    @Test
    fun `firing a dismissed instance is rejected - this is the stale notification fix`() {
        val t = AlarmStateMachine.onFired(instance(AlarmPhase.Dismissed), 1_700_000_002_000L)
        assertTrue(t is AlarmTransition.Rejected)
    }

    @Test
    fun `repeated dismissals are idempotent`() {
        val first = AlarmStateMachine.onDismissed(instance(AlarmPhase.Ringing), 1000L) as AlarmTransition.Applied
        assertTrue(first.changed)
        assertEquals(AlarmPhase.Dismissed, first.instance.phase)

        val second = AlarmStateMachine.onDismissed(first.instance, 2000L) as AlarmTransition.Applied
        assertFalse("second dismiss must not report a change", second.changed)
        assertEquals(AlarmPhase.Dismissed, second.instance.phase)

        val third = AlarmStateMachine.onDismissed(second.instance, 3000L) as AlarmTransition.Applied
        assertFalse(third.changed)
    }

    @Test
    fun `dismiss works from any non terminal phase`() {
        listOf(AlarmPhase.Scheduled, AlarmPhase.Ringing, AlarmPhase.Snoozed).forEach { phase ->
            val t = AlarmStateMachine.onDismissed(instance(phase), 1000L) as AlarmTransition.Applied
            assertEquals("phase $phase", AlarmPhase.Dismissed, t.instance.phase)
            assertTrue(t.changed)
        }
    }

    @Test
    fun `snooze increments count and sets target`() {
        val t = AlarmStateMachine.onSnooze(instance(AlarmPhase.Ringing), 1_000_000L, 10, 3)
        assertTrue(t is AlarmTransition.Snooze)
        val s = t as AlarmTransition.Snooze
        assertEquals(1_000_000L + 10 * 60_000L, s.until)
        assertEquals(AlarmPhase.Snoozed, s.instance.phase)
        assertEquals(1, s.instance.snoozeCount)
    }

    @Test
    fun `snooze budget exhaustion dismisses instead of silently doing nothing`() {
        val t = AlarmStateMachine.onSnooze(instance(AlarmPhase.Ringing, snoozes = 3), 1_000_000L, 10, 3)
        assertTrue(t is AlarmTransition.Applied)
        assertEquals(AlarmPhase.Dismissed, (t as AlarmTransition.Applied).instance.phase)
    }

    @Test
    fun `snooze refused on a terminal instance`() {
        val t = AlarmStateMachine.onSnooze(instance(AlarmPhase.Dismissed), 1_000_000L, 10, 3)
        assertTrue(t is AlarmTransition.Rejected)
    }

    @Test
    fun `expiry is terminal and idempotent`() {
        val t = AlarmStateMachine.onExpired(instance(AlarmPhase.Ringing)) as AlarmTransition.Applied
        assertEquals(AlarmPhase.Expired, t.instance.phase)
        val again = AlarmStateMachine.onExpired(t.instance) as AlarmTransition.Applied
        assertFalse(again.changed)
    }

    @Test
    fun `superseding an old instance is terminal`() {
        val t = AlarmStateMachine.onSuperseded(instance(AlarmPhase.Ringing)) as AlarmTransition.Applied
        assertEquals(AlarmPhase.Superseded, t.instance.phase)
        assertTrue(t.instance.isTerminal)
    }

    @Test
    fun `tapping a stale notification is ignored`() {
        val live = mapOf(
            "9@1700000000000" to instance(AlarmPhase.Dismissed),
            "9@1700000060000" to instance(AlarmPhase.Ringing),
        )
        assertFalse(AlarmStateMachine.shouldHonourTappedNotification("9@1700000000000", live))
        assertTrue(AlarmStateMachine.shouldHonourTappedNotification("9@1700000060000", live))
        assertFalse(AlarmStateMachine.shouldHonourTappedNotification("unknown", live))
    }

    @Test
    fun `newest active instance wins for multiple firings`() {
        val older = AlarmInstance("a@1", 1, 100, AlarmPhase.Ringing)
        val newer = AlarmInstance("a@2", 1, 200, AlarmPhase.Ringing)
        val dead = AlarmInstance("a@3", 1, 300, AlarmPhase.Dismissed)
        assertEquals(newer, AlarmStateMachine.newestActive(listOf(older, newer, dead)))
    }

    @Test
    fun `two different alarms ringing simultaneously are both ringable`() {
        val first = instance(AlarmPhase.Ringing)
        val second = first.copy(instanceId = "10@1700000000000", alarmId = 10L)
        assertTrue(first.isRingable)
        assertTrue(second.isRingable)
        assertTrue(AlarmStateMachine.shouldHonourTappedNotification(first.instanceId, mapOf(first.instanceId to first)))
        assertTrue(AlarmStateMachine.shouldHonourTappedNotification(second.instanceId, mapOf(second.instanceId to second)))
    }
}
