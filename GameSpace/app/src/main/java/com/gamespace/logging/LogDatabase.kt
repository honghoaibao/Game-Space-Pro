package com.gamespace.logging

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
@TypeConverters(LogConverters::class)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}
