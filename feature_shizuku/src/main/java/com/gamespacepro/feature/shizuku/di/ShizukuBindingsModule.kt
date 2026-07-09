package com.gamespacepro.feature.shizuku.di

import com.gamespacepro.domain.shell.ShellCapabilityDetector
import com.gamespacepro.domain.shell.ShellExecutor
import com.gamespacepro.domain.shell.ShellPermissionManager
import com.gamespacepro.feature.shizuku.ShizukuCapabilityDetector
import com.gamespacepro.feature.shizuku.ShizukuPermissionManager
import com.gamespacepro.feature.shizuku.ShizukuShellExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain-layer shell contracts to their Shizuku-backed
 * implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ShizukuBindingsModule {

    @Binds
    @Singleton
    abstract fun bindsShellCapabilityDetector(
        impl: ShizukuCapabilityDetector,
    ): ShellCapabilityDetector

    @Binds
    @Singleton
    abstract fun bindsShellPermissionManager(
        impl: ShizukuPermissionManager,
    ): ShellPermissionManager

    @Binds
    @Singleton
    abstract fun bindsShellExecutor(
        impl: ShizukuShellExecutor,
    ): ShellExecutor
}
