package com.gamespace.packagemanager

/**
 * Một app đã cài trên máy, dùng để hiển thị trong danh sách chọn khi thêm game
 * (thay cho việc bắt người dùng tự gõ package name — xem [GameRepository.queryAllLaunchableApps]).
 */
data class InstalledAppInfo(
    val packageName: String,
    val appLabel: String,
)
