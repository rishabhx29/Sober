package com.sober.core.data.repository

import com.sober.core.data.local.dao.UsageSessionDao
import com.sober.core.data.local.entity.toEntity
import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.UsageSessionRepository
import com.sober.core.domain.usecase.SyncUsageSessionsUseCase
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
        // `usage_sessions` has a unique (packageName, date) index and the DAO
        // inserts with REPLACE, so this is one statement: a concurrent writer
        // cannot slip a second row in between a read and a write. Carrying the
        // row id is unnecessary now that the conflict target is explicit.
        dao.insertOrUpdateSession(session.copy(id = 0).toEntity())
    }

    override suspend fun getUsageForToday(packageName: String): UsageSession? {
        val today = today()
        return dao.getUsageForDate(packageName, today)?.toDomainModel()
    }

    override fun getAllUsageForDate(date: String): Flow<List<UsageSession>> {
        return dao.getAllUsageForDate(date).map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getUsageForDates(dates: List<String>): List<UsageSession> {
        if (dates.isEmpty()) return emptyList()
        return dao.getUsageForDates(dates).map { it.toDomainModel() }
    }

    override suspend fun deleteSession(packageName: String, date: String) {
        dao.deleteSession(packageName, date)
    }

    override suspend fun resetDailyUsage() {
        dao.resetDailyUsage(today(), System.currentTimeMillis())
    }

    /**
     * The rollup key has to match every other producer, which formats with
     * [Locale.US]. With a locale default, a Buddhist or Arabic-digit calendar
     * writes one key and reads another, so limits never see today's usage.
     */
    private fun today(): String = SimpleDateFormat(
        SyncUsageSessionsUseCase.DATE_FORMAT,
        Locale.US
    ).format(Date())
}
