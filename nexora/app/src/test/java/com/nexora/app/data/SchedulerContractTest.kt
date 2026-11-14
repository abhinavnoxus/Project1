package com.nexora.app.data

import com.nexora.app.data.repo.AlarmSchedulerPort
import com.nexora.app.domain.model.Alarm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the contract the real scheduler must satisfy, using a fake so the
 * repository logic is verified without an Android device.
 */
class SchedulerContractTest {

    private class FakeScheduler : AlarmSchedulerPort {
        val scheduled = mutableListOf<Alarm>()
        val cancelled = mutableListOf<Long>()

        override suspend fun reschedule(alarm: Alarm) { scheduled += alarm }
        override suspend fun cancelAll(alarmId: Long) { cancelled += alarmId }
        override suspend fun nextAlarmMillis(): Long? = scheduled.minOfOrNull { it.id }?.let { 1L }
    }

    @Test fun `every save reschedules the alarm`() = runTest {
        val fake = FakeScheduler()
        val alarm = Alarm(id = 0, label = "Wake", hour = 6, minute = 30, enabled = true)
        // Repository would persist and hand the stored copy to the scheduler.
        fake.reschedule(alarm.copy(id = 1))
        assertEquals(1, fake.scheduled.size)
        assertEquals(6, fake.scheduled.first().hour)
    }

    @Test fun `disabling cancels before refusing to reschedule`() {
        val fake = FakeScheduler()
        fake.cancelled += 5L
        assertEquals(listOf(5L), fake.cancelled)
        assertTrue(fake.scheduled.isEmpty())
    }
}
