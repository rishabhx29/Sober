package com.sober.core.domain.repository

import com.sober.core.domain.model.CapturedSection
import com.sober.core.domain.model.SectionCaptureRequest
import kotlinx.coroutines.flow.Flow

/**
 * The one in-flight recording, shared by the screen that starts it and the
 * accessibility service that fills it in.
 *
 * A single slot, not a queue: the user is walking to one screen, and a second
 * armed request would race the first one's result.
 */
interface SectionCaptureRepository {

    /** Non-null while a recording is armed. */
    fun request(): Flow<SectionCaptureRequest?>

    /** The last screen seen by an armed recording, kept after it ends. */
    fun capturedSection(): Flow<CapturedSection?>

    suspend fun start(packageName: String, now: Long)

    /** Called by the service as the screen changes; the newest result wins. */
    suspend fun record(section: CapturedSection)

    /** The user is back in Sober: stop recording but keep the result to review. */
    suspend fun finish()

    /** Drops both the request and the result. */
    suspend fun clear()
}
