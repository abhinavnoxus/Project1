package com.nexora.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexora.app.data.dao.AlarmDao
import com.nexora.app.data.dao.AlarmInstanceDao
import com.nexora.app.data.dao.ExtraDao
import com.nexora.app.data.dao.FocusDao
import com.nexora.app.data.dao.RoutineDao
import com.nexora.app.data.dao.TaskDao
import com.nexora.app.data.entity.AlarmEntity
import com.nexora.app.data.entity.AlarmInstanceEntity
import com.nexora.app.data.entity.FocusSessionEntity
import com.nexora.app.data.entity.RoutineEntity
import com.nexora.app.data.entity.RoutineLogEntity
import com.nexora.app.data.entity.TaskEntity
import com.nexora.app.data.entity.TimerHistoryEntity
import com.nexora.app.data.entity.WorldClockEntity

@Database(
    entities = [
        AlarmEntity::class,
        AlarmInstanceEntity::class,
        TaskEntity::class,
        RoutineEntity::class,
        RoutineLogEntity::class,
        FocusSessionEntity::class,
        WorldClockEntity::class,
        TimerHistoryEntity::class,
    ],
    version = NexoraDatabase.VERSION,
    exportSchema = true,
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmInstanceDao(): AlarmInstanceDao
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusDao(): FocusDao
    abstract fun extraDao(): ExtraDao

    companion object {
        const val VERSION = 2
        const val NAME = "nexora.db"

        /**
         * v1 → v2 adds the timer history table and the focus-session label column.
         * Written by hand and covered by a MigrationTest so an upgrade from the
         * first shipped build cannot destroy a user's alarms.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `timer_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `finishedAt` INTEGER NOT NULL,
                        `cancelled` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `focus_sessions` ADD COLUMN `label` TEXT NOT NULL DEFAULT 'Focus'")
            }
        }

        @Volatile
        private var instance: NexoraDatabase? = null

        fun get(context: Context): NexoraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NexoraDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                // No destructive fallback: a failed migration must surface, not delete data.
                .build()
                .also { instance = it }
        }
    }
}
