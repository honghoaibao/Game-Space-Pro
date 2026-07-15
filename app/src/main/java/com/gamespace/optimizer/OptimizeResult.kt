package com.gamespace.optimizer

data class OptimizeResult(
    val ramFreedBytes: Long = 0,
    val backgroundAppsKilled: Int = 0,
    val cacheClearedBytes: Long = 0,
    val fstrimRan: Boolean = false,
    /** Số app nhạc đã được loại trừ khỏi Deep RAM Cleaner nhờ danh sách bảo vệ. */
    val protectedAppsSkipped: Int = 0,
    val skippedActions: List<String> = emptyList(),
) {
    val ramFreedMb: Double get() = ramFreedBytes / 1_048_576.0
    val cacheClearedMb: Double get() = cacheClearedBytes / 1_048_576.0
}
