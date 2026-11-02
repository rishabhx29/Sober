package com.sober.core.data.repository

import com.sober.core.data.local.dao.AppOpenDao
import com.sober.core.data.local.entity.AppOpenEntity
import com.sober.core.domain.repository.AppOpenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenRepositoryImpl @Inject constructor(
    private val dao: AppOpenDao
) : AppOpenRepository {

    override suspend fun recordOpen(packageName: String, at: Long) {
        dao.insert(AppOpenEntity(packageName = packageName, openedAt = at))
    }

    override suspend fun countToday(packageName: String, now: Long): Int =
        dao.countSince(packageName, startOfToday(now))

    /**
     * The window is fixed when the flow is created. A screen that stays open
     * across midnight should re-subscribe (its refresh already does) rather
     * than have an old window silently keep counting.
     */
    override fun observeTodayCounts(now: Long): Flow<Map<String, Int>> =
        dao.countsSince(startOfToday(now))
            .map { rows -> rows.associate { it.packageName to it.openCount } }

    override suspend fun deleteBefore(cutoff: Long) = dao.deleteBefore(cutoff)

    private fun startOfToday(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
