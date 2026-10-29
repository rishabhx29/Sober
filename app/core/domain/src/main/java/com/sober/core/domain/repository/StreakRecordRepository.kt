package com.sober.core.domain.repository

import com.sober.core.domain.model.StreakRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StreakRecordRepository {
    fun record(): Flow<StreakRecord>

    /**
     * Folds a finished, successful day into the stored record.
     *
     * The read-modify-write lives in the implementation so two callers (Home
     * and Focus) cannot clobber each other's update.
     */
    suspend fun recordSuccessfulDay(streakEndingOn: Int, date: LocalDate)
}
