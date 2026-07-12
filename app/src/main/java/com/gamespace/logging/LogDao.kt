package com.gamespace.logging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log_entries ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries ORDER BY timestampMillis ASC")
    suspend fun getAllForExport(): List<LogEntry>

    @Query("DELETE FROM log_entries WHERE timestampMillis < :olderThanMillis")
    suspend fun trimOlderThan(olderThanMillis: Long)

    @Query("DELETE FROM log_entries")
    suspend fun clearAll()
}
