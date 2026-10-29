package com.sober.core.data.di

import com.sober.core.data.repository.AccessibilityStateRepositoryImpl
import com.sober.core.data.repository.AchievementRepositoryImpl
import com.sober.core.data.repository.AppCategoryRepositoryImpl
import com.sober.core.data.repository.AppLimitRepositoryImpl
import com.sober.core.data.repository.CustomRuleRepositoryImpl
import com.sober.core.data.repository.DetectionRuleRepositoryImpl
import com.sober.core.data.repository.FocusSessionRepositoryImpl
import com.sober.core.data.repository.NotificationPreferencesRepositoryImpl
import com.sober.core.data.repository.OverrideLogRepositoryImpl
import com.sober.core.data.repository.OverrideStateRepositoryImpl
import com.sober.core.data.repository.PinRepositoryImpl
import com.sober.core.data.repository.ScheduleRuleRepositoryImpl
import com.sober.core.data.repository.StreakPreferencesRepositoryImpl
import com.sober.core.data.repository.StreakRecordRepositoryImpl
import com.sober.core.data.repository.ThemePreferencesRepositoryImpl
import com.sober.core.data.repository.UsageSessionRepositoryImpl
import com.sober.core.domain.repository.AccessibilityStateRepository
import com.sober.core.domain.repository.AchievementRepository
import com.sober.core.domain.repository.AppCategoryRepository
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.CustomRuleRepository
import com.sober.core.domain.repository.DetectionRuleRepository
import com.sober.core.domain.repository.FocusSessionRepository
import com.sober.core.domain.repository.NotificationPreferencesRepository
import com.sober.core.domain.repository.OverrideLogRepository
import com.sober.core.domain.repository.OverrideStateRepository
import com.sober.core.domain.repository.PinRepository
import com.sober.core.domain.repository.ScheduleRuleRepository
import com.sober.core.domain.repository.ScheduleSuggestionRepository
import com.sober.core.domain.repository.SectionRulePreferencesRepository
import com.sober.core.domain.repository.StreakPreferencesRepository
import com.sober.core.domain.repository.StreakRecordRepository
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.data.repository.ScheduleSuggestionRepositoryImpl
import com.sober.core.data.repository.SectionRulePreferencesRepositoryImpl
import com.sober.core.data.repository.SessionStateRepositoryImpl
import com.sober.core.domain.repository.SessionStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppLimitRepository(
        impl: AppLimitRepositoryImpl
    ): AppLimitRepository

    @Binds
    @Singleton
    abstract fun bindUsageSessionRepository(
        impl: UsageSessionRepositoryImpl
    ): UsageSessionRepository

    @Binds
    @Singleton
    abstract fun bindFocusSessionRepository(
        impl: FocusSessionRepositoryImpl
    ): FocusSessionRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRuleRepository(
        impl: ScheduleRuleRepositoryImpl
    ): ScheduleRuleRepository

    @Binds
    @Singleton
    abstract fun bindOverrideLogRepository(
        impl: OverrideLogRepositoryImpl
    ): OverrideLogRepository

    @Binds
    @Singleton
    abstract fun bindSessionStateRepository(
        impl: SessionStateRepositoryImpl
    ): SessionStateRepository

    @Binds
    @Singleton
    abstract fun bindPinRepository(
        impl: PinRepositoryImpl
    ): PinRepository

    @Binds
    @Singleton
    abstract fun bindOverrideStateRepository(
        impl: OverrideStateRepositoryImpl
    ): OverrideStateRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesRepository(
        impl: NotificationPreferencesRepositoryImpl
    ): NotificationPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindStreakPreferencesRepository(
        impl: StreakPreferencesRepositoryImpl
    ): StreakPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindStreakRecordRepository(
        impl: StreakRecordRepositoryImpl
    ): StreakRecordRepository

    @Binds
    @Singleton
    abstract fun bindDetectionRuleRepository(
        impl: DetectionRuleRepositoryImpl
    ): DetectionRuleRepository

    @Binds
    @Singleton
    abstract fun bindSectionRulePreferencesRepository(
        impl: SectionRulePreferencesRepositoryImpl
    ): SectionRulePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAccessibilityStateRepository(
        impl: AccessibilityStateRepositoryImpl
    ): AccessibilityStateRepository

    @Binds
    @Singleton
    abstract fun bindCustomRuleRepository(
        impl: CustomRuleRepositoryImpl
    ): CustomRuleRepository

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: ThemePreferencesRepositoryImpl
    ): ThemePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(
        impl: AchievementRepositoryImpl
    ): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindAppCategoryRepository(
        impl: AppCategoryRepositoryImpl
    ): AppCategoryRepository

    @Binds
    @Singleton
    abstract fun bindScheduleSuggestionRepository(
        impl: ScheduleSuggestionRepositoryImpl
    ): ScheduleSuggestionRepository
}
