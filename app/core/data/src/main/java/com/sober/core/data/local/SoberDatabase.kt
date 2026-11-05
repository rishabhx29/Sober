package com.sober.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sober.core.data.local.dao.AppLimitDao
import com.sober.core.data.local.dao.AppOpenDao
import com.sober.core.data.local.dao.FocusSessionDao
import com.sober.core.data.local.dao.OverrideLogDao
import com.sober.core.data.local.dao.ScheduleRuleDao
import com.sober.core.data.local.dao.UsageSessionDao
import com.sober.core.data.local.entity.AppLimitEntity
import com.sober.core.data.local.entity.AppOpenEntity
import com.sober.core.data.local.entity.FocusSessionEntity
import com.sober.core.data.local.entity.OverrideLogEntity
import com.sober.core.data.local.entity.ScheduleRuleEntity
import com.sober.core.data.local.entity.UsageSessionEntity
import com.sober.core.data.local.util.Converters

@Database(
    entities = [
        AppLimitEntity::class,
        UsageSessionEntity::class,
        FocusSessionEntity::class,
        ScheduleRuleEntity::class,
        OverrideLogEntity::class,
        AppOpenEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoberDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun overrideLogDao(): OverrideLogDao
    abstract fun appOpenDao(): AppOpenDao

    companion object {
        /**
         * v2 adds the opens-per-day budget and the table that counts opens.
         * Both are additive with defaults, so an upgrade keeps every existing
         * limit and history row untouched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_limits ADD COLUMN dailyOpenLimit INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_opens` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, " +
                        "`openedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_opens_packageName` " +
                        "ON `app_opens` (`packageName`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_opens_openedAt` " +
                        "ON `app_opens` (`openedAt`)"
                )
            }
        }

        /**
         * v3 makes "one row per app per day" and "one limit per app" real
         * constraints. Both tables could previously hold duplicates, so the
         * migration keeps the newest row of each pair before creating the
         * unique index — creating it first would fail on exactly the devices
         * that hit the race.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM usage_sessions WHERE id NOT IN (" +
                        "SELECT MAX(id) FROM usage_sessions GROUP BY packageName, date)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_sessions_packageName_date` " +
                        "ON `usage_sessions` (`packageName`, `date`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_usage_sessions_date` " +
                        "ON `usage_sessions` (`date`)"
                )

                db.execSQL(
                    "DELETE FROM app_limits WHERE id NOT IN (" +
                        "SELECT MAX(id) FROM app_limits GROUP BY packageName)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_app_limits_packageName` " +
                        "ON `app_limits` (`packageName`)"
                )
            }
        }

        /**
         * v4 adds the per-app session timer flag. Additive with an "off"
         * default, so every existing limit keeps behaving exactly as it did.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_limits ADD COLUMN sessionTimerEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * v5 adds the rest time that follows a session. Five minutes is the
         * same rest the old build gave a five-minute session, and it is the
         * value the app shows as the default for every new limit.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_limits ADD COLUMN sessionRestMinutes INTEGER NOT NULL DEFAULT 5"
                )
            }
        }
    }
}
