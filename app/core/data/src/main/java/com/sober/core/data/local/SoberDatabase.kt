package com.sober.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sober.core.data.local.dao.AppLimitDao
import com.sober.core.data.local.dao.FocusSessionDao
import com.sober.core.data.local.dao.OverrideLogDao
import com.sober.core.data.local.dao.ScheduleRuleDao
import com.sober.core.data.local.dao.UsageSessionDao
import com.sober.core.data.local.entity.AppLimitEntity
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
        OverrideLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoberDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun overrideLogDao(): OverrideLogDao
}
