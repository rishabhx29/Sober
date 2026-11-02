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
    version = 2,
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
    }
}
