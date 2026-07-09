package com.gamespacepro.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation
 * for the app-level dependency graph that every feature module's Hilt
 * components attach to.
 */
@HiltAndroidApp
class GameSpaceProApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Release logging (structured, exportable-as-ZIP per the product
        // spec's Logging requirement) is wired up in feature_logs, not here.
    }
}
