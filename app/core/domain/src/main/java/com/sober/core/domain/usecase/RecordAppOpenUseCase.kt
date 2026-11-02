package com.sober.core.domain.usecase

import com.sober.core.domain.repository.AppOpenRepository
import javax.inject.Inject

/**
 * Counts apps opened today for the opens-per-day budget.
 *
 * The engine calls this on every foreground transition and remembers the last
 * package itself, so a 15-second poll of one open app does not count fifteen
 * opens. This class therefore stays a plain write plus a pruning helper.
 */
class RecordAppOpenUseCase @Inject constructor(
    private val appOpenRepository: AppOpenRepository
) {
    suspend operator fun invoke(packageName: String, now: Long = System.currentTimeMillis()) {
        appOpenRepository.recordOpen(packageName, now)
    }

    /** Drops rows older than [days] so the table stays bounded. */
    suspend fun prune(days: Int = RETAIN_DAYS, now: Long = System.currentTimeMillis()) {
        appOpenRepository.deleteBefore(now - days * 24 * 60 * 60 * 1000L)
    }

    companion object {
        const val RETAIN_DAYS = 30
    }
}
