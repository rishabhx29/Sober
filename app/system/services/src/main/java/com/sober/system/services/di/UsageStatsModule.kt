package com.sober.system.services.di

import com.sober.core.domain.repository.UsageStatsRepository
import com.sober.system.services.usage.UsageStatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsageStatsModule {

    @Binds
    @Singleton
    abstract fun bindUsageStatsRepository(
        usageStatsRepositoryImpl: UsageStatsRepositoryImpl
    ): UsageStatsRepository
}
