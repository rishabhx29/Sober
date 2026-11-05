package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.domain.model.AppSession
import com.sober.core.domain.repository.AppSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionRepositoryImpl @Inject constructor(
    private val soberPreferences: SoberPreferences
) : AppSessionRepository {

    override val sessions: Flow<Map<String, AppSession>> = soberPreferences.appSessions

    override suspend fun sessionFor(packageName: String): AppSession? =
        sessions.first()[packageName]

    override suspend fun start(session: AppSession) = soberPreferences.setAppSession(session)

    override suspend fun clear(packageName: String) =
        soberPreferences.clearAppSession(packageName)

    override suspend fun clearConcluded(now: Long) =
        soberPreferences.clearConcludedAppSessions(now)
}
