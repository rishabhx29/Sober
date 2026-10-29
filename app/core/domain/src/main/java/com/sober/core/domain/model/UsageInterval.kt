package com.sober.core.domain.model

/** A stretch of time one app was in the foreground, clipped by the query range. */
data class UsageInterval(
    val startMillis: Long,
    val endMillis: Long
)
