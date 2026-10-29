package com.sober.core.domain.usecase

import com.sober.core.domain.repository.StreakRecordRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Folds the day that just ended into the persisted streak record.
 *
 * Recording happens for yesterday, not today: today's usage can still cross a
 * limit, and writing it down as a success would let a broken streak survive in
 * the record. Calling this more than once a day is safe — the record ignores a
 * day it already knows.
 */
class RecordStreakUseCase @Inject constructor(
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val streakRecordRepository: StreakRecordRepository
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now(ZoneId.systemDefault())) {
        val yesterday = today.minusDays(1)
        val streak = calculateStreakUseCase.streakEndingOn(yesterday)
        if (streak <= 0) return
        streakRecordRepository.recordSuccessfulDay(streak, yesterday)
    }
}
