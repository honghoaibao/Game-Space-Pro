package com.gamespace.profile

import android.content.Intent

/**
 * Kết quả của [ProfileEngine.apply]. UI (Dashboard/Automation) dùng để hiển thị
 * "đã áp dụng gì" và, nếu có [pendingSystemIntents], mở màn hình hệ thống tương ứng
 * (vd: Battery Saver Settings) — GAME SPACE không bao giờ tự bật các cờ hệ thống
 * mà không qua xác nhận của người dùng khi không có Shizuku.
 */
data class ProfileApplyResult(
    val profileType: ProfileType,
    val appliedActions: List<String>,
    val skippedActions: List<String>,
    val pendingSystemIntents: List<Pair<String, Intent>>,
)
