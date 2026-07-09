package com.gamespacepro.feature.accessibility.di

import com.gamespacepro.domain.accessibility.AccessibilityCapabilityDetector
import com.gamespacepro.domain.accessibility.ForegroundAppObserver
import com.gamespacepro.feature.accessibility.ForegroundAppRepository
import com.gamespacepro.feature.accessibility.GameSpaceAccessibilityCapabilityDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccessibilityBindingsModule {

    @Binds
    @Singleton
    abstract fun bindsForegroundAppObserver(
        impl: ForegroundAppRepository,
    ): ForegroundAppObserver

    @Binds
    @Singleton
    abstract fun bindsAccessibilityCapabilityDetector(
        impl: GameSpaceAccessibilityCapabilityDetector,
    ): AccessibilityCapabilityDetector
}
