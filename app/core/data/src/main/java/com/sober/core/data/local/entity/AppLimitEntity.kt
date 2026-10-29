package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sober.core.domain.model.AppLimit

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true,
    val blockedSections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = AppLimit(
        id = id,
        packageName = packageName,
        appName = appName,
        dailyLimitMinutes = dailyLimitMinutes,
        isActive = isActive,
        blockedSections = blockedSections,
        createdAt = createdAt
    )
}

fun AppLimit.toEntity() = AppLimitEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    dailyLimitMinutes = dailyLimitMinutes,
    isActive = isActive,
    blockedSections = blockedSections,
    createdAt = createdAt
)
