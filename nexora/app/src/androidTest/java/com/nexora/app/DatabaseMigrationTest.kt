package com.nexora.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexora.app.data.NexoraDatabase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration coverage.
 *
 * Runs only when an emulator or device is attached (`connectedDebugAndroidTest`). The CI
 * pipeline has neither, so these are reported as *not run* rather than claimed as passing.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val dbName = "nexora-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NexoraDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesAlarmsAndAddsTimerHistory() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO alarms (id, label, hour, minute, repeatBits, enabled, ringtoneUri, vibrate, " +
                    "snoozeMinutes, maxSnoozes, fadeInSeconds, kind, oneShotAt, skipNext, createdAt) " +
                    "VALUES (1, 'Wake', 6, 30, 127, 1, NULL, 1, 10, 3, 8, 'Repeating', NULL, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 2, true, NexoraDatabase.MIGRATION_1_2).use { db ->
            val cursor = db.query("SELECT label, hour, minute FROM alarms WHERE id = 1")
            assertTrue("alarm row must survive the migration", cursor.moveToFirst())
            assertTrue(cursor.getString(0) == "Wake")
            assertTrue(cursor.getInt(1) == 6)
            cursor.close()

            val tables = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='timer_history'")
            assertTrue("timer_history must be created", tables.moveToFirst())
            tables.close()

            val cols = db.query("PRAGMA table_info(focus_sessions)")
            var hasLabel = false
            while (cols.moveToNext()) {
                if (cols.getString(1) == "label") hasLabel = true
            }
            cols.close()
            assertTrue("focus_sessions.label must be added", hasLabel)
        }
    }
}
