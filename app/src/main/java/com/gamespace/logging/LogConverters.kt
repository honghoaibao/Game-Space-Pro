package com.gamespace.logging

import androidx.room.TypeConverter

class LogConverters {
    @TypeConverter
    fun fromCategory(category: LogCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): LogCategory = LogCategory.valueOf(value)
}
