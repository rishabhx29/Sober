package com.sober.core.domain.usecase

import com.sober.core.domain.model.UsageSession
import com.sober.core.domain.repository.UsageSessionRepository
import javax.inject.Inject

class GetAppUsageUseCase @Inject constructor(
    private val usageRepository: UsageSessionRepository
) {
    suspend operator fun invoke(packageName: String): UsageSession? {
        return usageRepository.getUsageForToday(packageName)
    }
}
