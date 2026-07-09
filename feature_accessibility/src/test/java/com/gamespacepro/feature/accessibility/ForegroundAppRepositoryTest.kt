package com.gamespacepro.feature.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppRepositoryTest {

    private val repository = ForegroundAppRepository()

    @Test
    fun `initial foreground app is null`() {
        assertNull(repository.foregroundApp.value)
    }

    @Test
    fun `onForegroundAppChanged updates the exposed state`() {
        repository.onForegroundAppChanged("com.example.game")

        assertEquals("com.example.game", repository.foregroundApp.value)
    }

    @Test
    fun `a later call overwrites the previous value`() {
        repository.onForegroundAppChanged("com.example.game")
        repository.onForegroundAppChanged("com.example.launcher")

        assertEquals("com.example.launcher", repository.foregroundApp.value)
    }

    @Test
    fun `onForegroundAppChanged with null clears the state`() {
        repository.onForegroundAppChanged("com.example.game")
        repository.onForegroundAppChanged(null)

        assertNull(repository.foregroundApp.value)
    }
}
