package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sober.core.domain.model.AppLimit

/**
 * One limit per package. `packageName` is unique so a double-tapped "Set
 * limit" cannot leave two rows where every reader takes an arbitrary one.
 */
@Entity(
    tableName = "app_limits",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class AppLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val dailyOpenLimit: Int = 0,
    val sessionTimerEnabled: Boolean = false,
    val sessionRestMinutes: Int = AppLimit.DEFAULT_SESSION_REST_MINUTES
) {
    fun toDomainModel() = AppLimit(
        id = id,
        packageName = packageName,
        appName = appName,
        dailyLimitMinutes = dailyLimitMinutes,
        isActive = isActive,
        blockedSections = blockedSections,
        createdAt = createdAt,
        dailyOpenLimit = dailyOpenLimit,
        sessionTimerEnabled = sessionTimerEnabled,
        sessionRestMinutes = sessionRestMinutes
    )
}

fun AppLimit.toEntity() = AppLimitEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    dailyLimitMinutes = dailyLimitMinutes,
    isActive = isActive,
    blockedSections = blockedSections,
    createdAt = createdAt,
    dailyOpenLimit = dailyOpenLimit,
    sessionTimerEnabled = sessionTimerEnabled,
    sessionRestMinutes = sessionRestMinutes
)
