package com.sober.core.data.di

import android.content.Context
import androidx.room.Room
import com.sober.core.data.local.SoberDatabase
import com.sober.core.data.local.dao.AppLimitDao
import com.sober.core.data.local.dao.FocusSessionDao
import com.sober.core.data.local.dao.OverrideLogDao
import com.sober.core.data.local.dao.ScheduleRuleDao
import com.sober.core.data.local.dao.UsageSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSoberDatabase(@ApplicationContext context: Context): SoberDatabase {
        return Room.databaseBuilder(
            context,
            SoberDatabase::class.java,
            "sober_database"
        ).build()
    }

    @Provides
    fun provideAppLimitDao(database: SoberDatabase): AppLimitDao = database.appLimitDao()

    @Provides
    fun provideUsageSessionDao(database: SoberDatabase): UsageSessionDao = database.usageSessionDao()

    @Provides
    fun provideFocusSessionDao(database: SoberDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideScheduleRuleDao(database: SoberDatabase): ScheduleRuleDao = database.scheduleRuleDao()

    @Provides
    fun provideOverrideLogDao(database: SoberDatabase): OverrideLogDao = database.overrideLogDao()
}
