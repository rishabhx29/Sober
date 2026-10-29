package com.sober.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManageMealBreakUseCaseTest {

    @Test
    fun `start opens a twenty minute window`() = runTest {
        val state = FakeSessionStateRepository()
        val useCase = ManageMealBreakUseCase(state)

        val until = useCase.start(now = 1_000_000L)

        assertEquals(1_000_000L + 20 * 60_000L, until)
        assertEquals(until, state.mealBreakUntil.value)
    }

    @Test
    fun `endNow clears the window`() = runTest {
        val state = FakeSessionStateRepository()
        val useCase = ManageMealBreakUseCase(state)
        useCase.start()

        useCase.endNow()

        assertNull(state.mealBreakUntil.value)
    }
}
