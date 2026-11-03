package com.sober.core.domain.model

/**
 * A recording the user asked for while they walk to the screen they mean.
 *
 * The request is a package, not a rule: what to match is only known after the
 * accessibility service has seen the screen. It expires on its own, because a
 * request left behind by a phone call must not silently record something else
 * an hour later.
 */
data class SectionCaptureRequest(
    val packageName: String,
    val requestedAt: Long
)

/**
 * What the accessibility service saw while a capture was armed.
 *
 * It holds suggestions, not a rule: only the user can tell a bottom-bar tab
 * from a decorative selected chip, so the review screen names and picks one.
 * An empty [candidates] list means the service looked and found nothing it can
 * match on, which is a result worth showing rather than an error.
 */
data class CapturedSection(
    val packageName: String,
    val activityName: String?,
    val capturedAt: Long,
    val candidates: List<CapturedCandidate> = emptyList()
)

/**
 * A rule shape the capture can prove exists on the captured screen.
 *
 * [isBroad] marks a whole-activity match. In a single-activity app that is the
 * whole app, so the review screen warns before the user saves one.
 */
data class CapturedCandidate(
    val type: DetectionType,
    val value: String,
    val label: String,
    val isBroad: Boolean = false
)
