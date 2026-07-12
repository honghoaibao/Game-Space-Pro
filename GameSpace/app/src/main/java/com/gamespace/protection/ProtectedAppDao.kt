package com.gamespace.protection

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectedAppDao {
    @Query("SELECT * FROM protected_apps ORDER BY isEnabled DESC, appLabel ASC")
    fun observeAll(): Flow<List<ProtectedAppEntity>>

    @Query("SELECT packageName FROM protected_apps WHERE isEnabled = 1")
    fun observeEnabledPackages(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(apps: List<ProtectedAppEntity>)

    @Query("UPDATE protected_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, isEnabled: Boolean)

    @Query("DELETE FROM protected_apps WHERE packageName = :packageName AND addedManually = 1")
    suspend fun removeManual(packageName: String)
}
