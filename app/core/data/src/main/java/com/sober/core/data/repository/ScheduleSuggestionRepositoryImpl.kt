package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.ScheduleSuggestionKind
import com.sober.core.domain.repository.ScheduleSuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduleSuggestionRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : ScheduleSuggestionRepository {

    override fun getDismissed(): Flow<Set<ScheduleSuggestionKind>> =
        preferences.dismissedScheduleSuggestionsRaw.map { raw ->
            raw?.split(',')
                ?.mapNotNull { runCatching { ScheduleSuggestionKind.valueOf(it) }.getOrNull() }
                ?.toSet()
                .orEmpty()
        }

    override suspend fun dismiss(kind: ScheduleSuggestionKind) {
        val current = getDismissed().first()
        if (kind in current) return
        preferences.setDismissedScheduleSuggestionsRaw(
            (current + kind).joinToString(",") { it.name }
        )
    }
}
