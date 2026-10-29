package com.sober.core.data.repository

import com.sober.core.data.local.dao.UsageSessionDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.UsageSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class UsageSessionRepositoryImpl @Inject constructor(
    private val dao: UsageSessionDao
) : UsageSessionRepository {

    override suspend fun insertOrUpdateSession(session: UsageSession) {
        // `usage_sessions` uses an auto-generated primary key, and the DAO's
        // insert uses OnConflictStrategy.REPLACE. Inserting a fresh domain
        // object (id = 0) every poll would therefore append a new row each
        // time rather than update the existing one, so the same app/day would
        // accumulate duplicates and every read would see a stale first match.
        // Carrying the existing row's id forward keeps exactly one row per
        // (packageName, date).
        val existing = dao.getUsageForDate(session.packageName, session.date)
        dao.insertOrUpdateSession(
            session.copy(id = existing?.id ?: 0).toEntity()
        )
    }

    override suspend fun getUsageForToday(packageName: String): UsageSession? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return dao.getUsageForDate(packageName, today)?.toDomainModel()
    }

    override fun getAllUsageForDate(date: String): Flow<List<UsageSession>> {
        return dao.getAllUsageForDate(date).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getUsageForDates(dates: List<String>): List<UsageSession> {
        if (dates.isEmpty()) return emptyList()
        return dao.getUsageForDates(dates).map { it.toDomainModel() }
    }

    override suspend fun resetDailyUsage() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.resetDailyUsage(today, System.currentTimeMillis())
    }
}
