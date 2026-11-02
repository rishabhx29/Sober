package com.sober.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One foreground entry into an app. Rows are tiny and pruned after 30 days;
 * counting them answers "how often" where `usage_sessions` answers "how long".
 */
@Entity(
    tableName = "app_opens",
    indices = [Index("packageName"), Index("openedAt")]
)
data class AppOpenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val openedAt: Long
)
