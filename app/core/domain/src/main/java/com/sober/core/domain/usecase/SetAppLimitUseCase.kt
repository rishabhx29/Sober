package com.sober.core.domain.usecase

import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.repository.AppLimitRepository
import javax.inject.Inject

class SetAppLimitUseCase @Inject constructor(
    private val appLimitRepository: AppLimitRepository
) {
    suspend operator fun invoke(appLimit: AppLimit) {
        val existing = appLimitRepository.getLimit(appLimit.packageName)
        if (existing != null) {
            appLimitRepository.updateLimit(appLimit.copy(id = existing.id, createdAt = existing.createdAt))
        } else {
            appLimitRepository.insertLimit(appLimit)
        }
    }
}
