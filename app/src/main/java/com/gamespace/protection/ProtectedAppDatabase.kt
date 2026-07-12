package com.gamespace.protection

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProtectedAppEntity::class], version = 1, exportSchema = false)
abstract class ProtectedAppDatabase : RoomDatabase() {
    abstract fun protectedAppDao(): ProtectedAppDao
}
