package com.sober.core.domain.repository

import com.sober.core.domain.model.UsageSession
import kotlinx.coroutines.flow.Flow

interface UsageSessionRepository {
    suspend fun insertOrUpdateSession(session: UsageSession)
    suspend fun getUsageForToday(packageName: String): UsageSession?
    fun getAllUsageForDate(date: String): Flow<List<UsageSession>>
    suspend fun getUsageForDates(dates: List<String>): List<UsageSession>
    suspend fun resetDailyUsage() // Zeroes out for midnight
}
