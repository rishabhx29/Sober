package com.sober.system.services.core

import android.content.Context
import com.sober.core.domain.repository.EnforcementController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Thin adapter so callers in feature modules can start the engine without a
 * compile-time dependency on `system:services`.
 */
class EnforcementControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : EnforcementController {
    override fun ensureRunning() {
        SoberCoreService.start(context)
    }
}
