package com.gamespace.logging

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogCategory {
    SHELL_COMMAND,
    ACCESSIBILITY,
    SHIZUKU,
    PROFILE,
    GAME_LAUNCH,
    THERMAL,
    ERROR,
}

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val category: LogCategory,
    val message: String,
)
