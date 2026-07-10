package com.gamespacepro.core.di

import javax.inject.Qualifier

/**
 * Qualifier annotations for injecting [kotlinx.coroutines.CoroutineDispatcher]
 * instances via Hilt.
 *
 * Every class that switches coroutine context (repositories, shell command
 * executors, accessibility event handlers, etc.) should inject a qualified
 * dispatcher through its constructor instead of referencing
 * [kotlinx.coroutines.Dispatchers] directly. That keeps every dispatcher
 * usage swappable for a deterministic
 * [kotlinx.coroutines.test.TestDispatcher] in unit tests.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainImmediateDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
