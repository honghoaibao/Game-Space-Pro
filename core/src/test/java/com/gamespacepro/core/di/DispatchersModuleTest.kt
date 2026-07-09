package com.gamespacepro.core.di

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Only [Dispatchers.IO] and [Dispatchers.Default] are exercised here.
 * [Dispatchers.Main] throws in a plain JVM unit test unless a Main
 * dispatcher is installed via `Dispatchers.setMain(...)` (kotlinx-coroutines
 * -test) or an instrumentation/Robolectric runtime is present — asserting
 * on it here would test the test environment, not this module.
 */
class DispatchersModuleTest {

    @Test
    fun `providesIoDispatcher returns Dispatchers IO`() {
        assertSame(Dispatchers.IO, DispatchersModule.providesIoDispatcher())
    }

    @Test
    fun `providesDefaultDispatcher returns Dispatchers Default`() {
        assertSame(Dispatchers.Default, DispatchersModule.providesDefaultDispatcher())
    }
}
