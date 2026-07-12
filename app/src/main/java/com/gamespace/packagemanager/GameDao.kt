package com.gamespace.packagemanager

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gamespace.profile.ProfileType
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY isFavorite DESC, lastPlayedMillis DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    @Query("UPDATE games SET assignedProfile = :profile WHERE packageName = :packageName")
    suspend fun setAssignedProfile(packageName: String, profile: ProfileType?)

    @Query(
        "UPDATE games SET lastPlayedMillis = :timestamp, " +
            "totalPlayTimeMillis = totalPlayTimeMillis + :sessionDurationMillis WHERE packageName = :packageName",
    )
    suspend fun recordSession(packageName: String, timestamp: Long, sessionDurationMillis: Long)

    @Query("DELETE FROM games WHERE packageName = :packageName AND addedManually = 0")
    suspend fun removeIfAutoDetected(packageName: String)
}
