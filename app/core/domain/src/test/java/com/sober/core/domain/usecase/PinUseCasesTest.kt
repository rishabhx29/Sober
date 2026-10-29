package com.sober.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetPinUseCaseTest {

    private fun useCase(repo: FakePinRepository = FakePinRepository()) =
        SetPinUseCase(repo) to repo

    @Test
    fun `accepts a four digit pin`() = runTest {
        val (setPin, repo) = useCase()

        assertTrue(setPin("1234"))
        assertTrue(repo.hasPin.first())
    }

    @Test
    fun `accepts a six digit pin`() = runTest {
        val (setPin, _) = useCase()

        assertTrue(setPin("123456"))
    }

    @Test
    fun `rejects a pin shorter than four digits`() = runTest {
        val (setPin, repo) = useCase()

        assertFalse(setPin("123"))
        assertFalse(repo.hasPin.first())
    }

    @Test
    fun `rejects a pin longer than six digits`() = runTest {
        val (setPin, repo) = useCase()

        assertFalse(setPin("1234567"))
        assertFalse(repo.hasPin.first())
    }

    @Test
    fun `rejects non digits`() = runTest {
        val (setPin, repo) = useCase()

        assertFalse(setPin("12a4"))
        assertFalse(repo.hasPin.first())
    }

    @Test
    fun `replaces an existing pin`() = runTest {
        val (setPin, repo) = useCase()

        assertTrue(setPin("1234"))
        assertTrue(setPin("987654"))

        assertTrue(repo.verifyPin("987654"))
        assertFalse(repo.verifyPin("1234"))
    }
}

class VerifyPinUseCaseTest {

    @Test
    fun `accepts the stored pin`() = runTest {
        val repo = FakePinRepository("4321")
        val verify = VerifyPinUseCase(repo)

        assertTrue(verify("4321"))
    }

    @Test
    fun `rejects a wrong pin`() = runTest {
        val verify = VerifyPinUseCase(FakePinRepository("4321"))

        assertFalse(verify("1234"))
    }

    @Test
    fun `rejects when no pin is configured`() = runTest {
        val repo = FakePinRepository()
        val verify = VerifyPinUseCase(repo)

        assertFalse(verify("1234"))
        // The whole point: unknown must not reach the repository and cannot
        // succeed by matching an empty record.
        assertTrue(repo.verifyCalls == 0)
    }

    @Test
    fun `rejects blank input without touching the repository`() = runTest {
        val repo = FakePinRepository("4321")
        val verify = VerifyPinUseCase(repo)

        assertFalse(verify("  "))
        assertTrue(repo.verifyCalls == 0)
    }
}
