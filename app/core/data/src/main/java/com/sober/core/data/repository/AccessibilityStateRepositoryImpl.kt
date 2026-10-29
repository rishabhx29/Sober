package com.sober.core.data.repository

import android.content.Context
import com.sober.core.common.permissions.PermissionUtils
import com.sober.core.domain.repository.AccessibilityStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AccessibilityStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AccessibilityStateRepository {

    override fun isSectionDetectionAvailable(): Boolean =
        PermissionUtils.hasAccessibilityPermission(context)
}
