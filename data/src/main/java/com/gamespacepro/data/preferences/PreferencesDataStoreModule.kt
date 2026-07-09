package com.gamespacepro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFERENCES_FILE_NAME = "game_space_pro_prefs"

/**
 * Provides the single, app-wide [DataStore] of [Preferences].
 *
 * Deliberately generic: no preference keys are defined here. Each feature
 * module owns its own `Preferences.Key<T>` constants, co-located with the
 * repository that reads/writes them, and injects this shared instance
 * rather than creating its own `.preferences_pb` file — DataStore's own
 * docs call out that creating multiple DataStores for the same file is a
 * common source of bugs.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesDataStoreModule {

    @Provides
    @Singleton
    fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) },
    )
}
