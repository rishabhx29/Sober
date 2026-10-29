package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.StreakRecord
import com.sober.core.domain.model.withSuccessfulDay
import com.sober.core.domain.repository.StreakRecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class StreakRecordRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : StreakRecordRepository {

    override fun record(): Flow<StreakRecord> = preferences.streakRecord

    override suspend fun recordSuccessfulDay(streakEndingOn: Int, date: LocalDate) {
        preferences.updateStreakRecord { current ->
            current.withSuccessfulDay(streakEndingOn, date)
        }
    }
}
