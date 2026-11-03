package com.sober.core.data.repository

import com.sober.core.domain.model.ActiveChallenge
import com.sober.core.domain.model.ChallengeId
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeCodecTest {

    @Test
    fun `a record round-trips`() {
        val challenge = ActiveChallenge(ChallengeId.QUIET_WEEKEND, LocalDate.of(2026, 11, 3))

        val decoded = ChallengeCodec.decode(ChallengeCodec.encode(challenge))

        assertEquals(challenge, decoded)
    }

    @Test
    fun `an unknown id is dropped`() {
        assertNull(ChallengeCodec.decode("REGRESSION_WEEK|2026-11-03"))
    }

    @Test
    fun `a malformed date is dropped`() {
        assertNull(ChallengeCodec.decode("CLEAN_WEEK|not-a-date"))
    }

    @Test
    fun `a record without both fields is dropped`() {
        assertNull(ChallengeCodec.decode("CLEAN_WEEK"))
        assertNull(ChallengeCodec.decode("CLEAN_WEEK|"))
        assertNull(ChallengeCodec.decode("|2026-11-03"))
        assertNull(ChallengeCodec.decode(""))
    }
}
