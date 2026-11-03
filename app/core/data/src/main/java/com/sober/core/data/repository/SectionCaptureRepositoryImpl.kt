package com.sober.core.data.repository

import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.data.rules.SectionCaptureCodec
import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.SectionCaptureRequest
import com.sober.core.domain.repository.SectionCaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The in-flight recording, persisted rather than held in memory.
 *
 * The screen arms it and the accessibility service fills it in, so it has to
 * survive both of them being recreated: a process death while the user is in
 * the target app must not lose the screen they walked to.
 */
@Singleton
class SectionCaptureRepositoryImpl @Inject constructor(
    private val preferences: SoberPreferences
) : SectionCaptureRepository {

    override fun request(): Flow<SectionCaptureRequest?> =
        preferences.sectionCaptureRequestRaw.map(SectionCaptureCodec::decodeRequest)

    override fun capturedSection(): Flow<CapturedSection?> =
        preferences.sectionCaptureResultRaw.map(SectionCaptureCodec::decodeSection)

    override suspend fun start(packageName: String, now: Long) {
        // Result first: the reverse order would briefly pair a new request with
        // the previous app's review screen.
        preferences.setSectionCaptureResult(null)
        preferences.setSectionCaptureRequest(
            SectionCaptureCodec.encodeRequest(
                SectionCaptureRequest(packageName = packageName, requestedAt = now)
            )
        )
    }

    override suspend fun record(section: CapturedSection) {
        preferences.setSectionCaptureResult(SectionCaptureCodec.encodeSection(section))
    }

    override suspend fun finish() {
        preferences.setSectionCaptureRequest(null)
    }

    override suspend fun clear() {
        preferences.setSectionCaptureRequest(null)
        preferences.setSectionCaptureResult(null)
    }
}
