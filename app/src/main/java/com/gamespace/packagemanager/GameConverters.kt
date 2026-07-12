package com.gamespace.packagemanager

import androidx.room.TypeConverter
import com.gamespace.profile.ProfileType

class GameConverters {
    @TypeConverter
    fun fromProfileType(type: ProfileType?): String? = type?.name

    @TypeConverter
    fun toProfileType(value: String?): ProfileType? =
        value?.let { runCatching { ProfileType.valueOf(it) }.getOrNull() }
}
