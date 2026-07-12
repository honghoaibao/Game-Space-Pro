package com.gamespace.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile_prefs")
private val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_profile")

/**
 * Nguồn sự thật duy nhất cho Profile đang active. Dùng DataStore để [com.gamespace.recovery]
 * (Phiên 4) có thể đọc lại state này sau khi app bị kill/crash.
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val activeProfile = context.profileDataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE]?.let { runCatching { ProfileType.valueOf(it) }.getOrNull() }
            ?: ProfileType.BALANCED
    }

    suspend fun setActiveProfile(type: ProfileType) {
        context.profileDataStore.edit { prefs -> prefs[KEY_ACTIVE_PROFILE] = type.name }
    }
}
