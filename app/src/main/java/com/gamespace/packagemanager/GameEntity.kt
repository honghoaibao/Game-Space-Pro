package com.gamespace.packagemanager

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamespace.profile.ProfileType

/**
 * Một game trong "Thư viện Game" (mục "9. Package Manager").
 * `packageName` là khóa chính — mỗi app chỉ xuất hiện một lần.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isFavorite: Boolean = false,
    val assignedProfile: ProfileType? = null, // null = dùng Profile mặc định của hệ thống
    val lastPlayedMillis: Long? = null,
    val totalPlayTimeMillis: Long = 0,
    /** true nếu người dùng tự thêm thủ công (app không tự nhận diện category GAME). */
    val addedManually: Boolean = false,
)
