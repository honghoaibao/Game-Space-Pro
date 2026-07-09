package com.gamespacepro.domain.usecase

import com.gamespacepro.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {

    private val succeeding = object : UseCase<Int, Int>() {
        override suspend fun execute(params: Int): Int = params * 2
    }

    private val failing = object : UseCase<Unit, Unit>() {
        override suspend fun execute(params: Unit) {
            throw IllegalStateException("boom")
        }
    }

    private val cancelling = object : UseCase<Unit, Unit>() {
        override suspend fun execute(params: Unit) {
            throw CancellationException("cancelled")
        }
    }

    @Test
    fun `invoke wraps successful execution in AppResult Success`() = runTest {
        val result = succeeding(21)

        assertEquals(AppResult.Success(42), result)
    }

    @Test
    fun `invoke wraps a thrown exception in AppResult Error instead of propagating it`() = runTest {
        val result = failing(Unit)

        assertTrue(result is AppResult.Error)
        assertEquals("boom", (result as AppResult.Error).exception.message)
    }

    @Test(expected = CancellationException::class)
    fun `invoke rethrows CancellationException instead of wrapping it`() = runTest {
        cancelling(Unit)
    }
}
