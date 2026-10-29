package com.sober.core.domain.repository

import com.sober.core.domain.model.ScheduleSuggestionKind
import kotlinx.coroutines.flow.Flow

/** Dismissals are stored without an expiry: "not this again" means for good. */
interface ScheduleSuggestionRepository {
    fun getDismissed(): Flow<Set<ScheduleSuggestionKind>>

    suspend fun dismiss(kind: ScheduleSuggestionKind)
}
