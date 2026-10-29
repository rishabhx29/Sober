package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sober.core.domain.model.ScheduleRule

@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleName: String,
    val blockedPackages: List<String> = emptyList(),
    val daysOfWeek: List<Int> = emptyList(), // e.g., 1=Mon, 7=Sun
    val startTime: String, // HH:MM
    val endTime: String, // HH:MM
    val isActive: Boolean = true
) {
    fun toDomainModel() = ScheduleRule(
        id = id,
        ruleName = ruleName,
        blockedPackages = blockedPackages,
        daysOfWeek = daysOfWeek,
        startTime = startTime,
        endTime = endTime,
        isActive = isActive
    )
}

fun ScheduleRule.toEntity() = ScheduleRuleEntity(
    id = id,
    ruleName = ruleName,
    blockedPackages = blockedPackages,
    daysOfWeek = daysOfWeek,
    startTime = startTime,
    endTime = endTime,
    isActive = isActive
)
