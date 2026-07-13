package com.gamespace.protection

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Thay cho "Music Hub" (theo yêu cầu người dùng) — thay vì tự xây trình phát nhạc,
 * GAME SPACE bảo vệ các app nghe nhạc CÓ SẴN (Spotify, YouTube Music, Zing MP3,
 * NhacCuaTui, SoundCloud...) khỏi bị Optimizer Engine dọn nền/buộc dừng, để nhạc
 * không bị ngắt khi đang chơi game. Xem ADR-009 trong `ARCHITECTURE.md`.
 */
@Entity(tableName = "protected_apps")
data class ProtectedAppEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isEnabled: Boolean = true,
    /** true nếu người dùng tự thêm (app không tự nhận diện category AUDIO). */
    val addedManually: Boolean = false,
)
