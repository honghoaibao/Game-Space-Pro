package com.gamespace.optimizer

import android.app.ActivityManager
import android.content.Context
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.protection.ProtectedAppRepository
import com.gamespace.shizuku.CapabilityDetector
import com.gamespace.shizuku.ShellExecutor
import com.gamespace.shizuku.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Triển khai mục "2. Optimizer Engine". Mỗi hành động độc lập, có thể gọi riêng
 * (nút "Dọn RAM" trên Dashboard/Overlay) hoặc gộp qua [runFullOptimize] (Automation
 * gọi trước khi vào game — mục "cleanBackgroundBeforeLaunch" trong ProfileConfig).
 *
 * "Smart Compilation", "Force Stop app nền" (triệt để) và "Storage Trim" (`sm fstrim`)
 * cần Shizuku; nếu chưa cấp quyền, các bước đó bị bỏ qua và ghi vào
 * [OptimizeResult.skippedActions] thay vì báo lỗi.
 *
 * Deep RAM Cleaner luôn loại trừ app trong danh sách [ProtectedAppRepository] (thay cho
 * "Music Hub" — xem ADR-009) để nhạc nền không bị ngắt khi dọn RAM. Giới hạn thật cần nói
 * rõ: đây chỉ ngăn CHÍNH GAME SPACE chủ động dọn/buộc dừng các app đó — không thể ngăn
 * Android tự kill app nền khi hệ thống thiếu RAM (nằm ngoài khả năng của app thứ ba).
 */
@Singleton
class OptimizerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val capabilityDetector: CapabilityDetector,
    private val shellExecutor: ShellExecutor,
    private val protectedAppRepository: ProtectedAppRepository,
    private val logManager: LogManager,
) {

    /** Gói app hệ thống/thiết yếu không bao giờ bị Deep RAM Cleaner đụng tới. */
    private val protectedPackagePrefixes = listOf(
        context.packageName, // chính GAME SPACE
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "android",
    )

    /**
     * Deep RAM Cleaner + Background Process Cleaner: kill các process nền có
     * importance >= [ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND]
     * (không đụng foreground/foreground_service/perceptible/visible), trừ app hệ thống
     * và app nhạc đang được bảo vệ (xem [ProtectedAppRepository]).
     */
    suspend fun deepRamClean(): OptimizeResult = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@withContext OptimizeResult(skippedActions = listOf("Không truy cập được ActivityManager"))

        val before = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
        val protectedMusicPackages = protectedAppRepository.observeEnabledPackages().first()

        val allBackgroundPackages = activityManager.runningAppProcesses.orEmpty()
            .filter { it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND }
            .flatMap { it.pkgList?.toList().orEmpty() }
            .distinct()

        val candidates = allBackgroundPackages
            .filterNot { pkg -> protectedPackagePrefixes.any { pkg.startsWith(it) } }
            .filterNot { pkg -> pkg in protectedMusicPackages }
        val protectedSkippedCount = allBackgroundPackages.count { it in protectedMusicPackages }

        candidates.forEach { pkg ->
            runCatching { activityManager.killBackgroundProcesses(pkg) }
        }

        val after = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
        val freed = (after - before).coerceAtLeast(0)

        logManager.log(
            LogCategory.SHELL_COMMAND,
            "Deep RAM Cleaner: kill ${candidates.size} tiến trình nền, giải phóng ~${freed / 1_048_576}MB" +
                if (protectedSkippedCount > 0) ", bảo vệ $protectedSkippedCount app nhạc" else "",
        )
        OptimizeResult(
            ramFreedBytes = freed,
            backgroundAppsKilled = candidates.size,
            protectedAppsSkipped = protectedSkippedCount,
        )
    }

    /**
     * Cache Cleaner: xóa cache của chính GAME SPACE ngay (luôn khả dụng). Nếu có
     * Shizuku, chạy thêm `pm trim-caches` để hệ thống dọn cache của các app khác
     * (public API không cho phép app thường xóa cache app khác trực tiếp).
     */
    suspend fun clearCache(): OptimizeResult = withContext(Dispatchers.IO) {
        val ownCacheBytes = sizeOf(context.cacheDir)
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()

        var trimRan = false
        val skipped = mutableListOf<String>()
        if (capabilityDetector.canRunShellCommands()) {
            // Yêu cầu trim tới một ngưỡng dung lượng rất lớn để hệ thống dọn gần hết cache có thể thu hồi.
            val result = shellExecutor.execute("pm trim-caches 999G")
            trimRan = result is ShellResult.Success
            if (!trimRan) skipped += "pm trim-caches thất bại: $result"
        } else {
            skipped += "Dọn cache app khác (cần Shizuku — chưa cấp quyền)"
        }

        logManager.log(
            LogCategory.SHELL_COMMAND,
            "Cache Cleaner: dọn ${ownCacheBytes / 1024}KB cache riêng, trim hệ thống=$trimRan",
        )
        OptimizeResult(cacheClearedBytes = ownCacheBytes, skippedActions = skipped)
    }

    /** Storage Trim (`sm fstrim`) — cần Shizuku. */
    suspend fun storageTrim(): OptimizeResult = withContext(Dispatchers.IO) {
        if (!capabilityDetector.canRunFstrim()) {
            return@withContext OptimizeResult(skippedActions = listOf("Storage Trim (cần Shizuku — chưa cấp quyền)"))
        }
        val result = shellExecutor.fstrim()
        val success = result is ShellResult.Success
        logManager.log(LogCategory.SHELL_COMMAND, "Storage Trim: thành công=$success")
        OptimizeResult(fstrimRan = success, skippedActions = if (success) emptyList() else listOf("fstrim thất bại: $result"))
    }

    /**
     * Package Optimizer / Smart Compilation: biên dịch lại một game sang chế độ
     * "speed" (`cmd package compile -m speed <package>`) — cần Shizuku.
     */
    suspend fun smartCompile(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!capabilityDetector.canRunShellCommands()) {
            logManager.log(LogCategory.SHELL_COMMAND, "Bỏ qua Smart Compilation cho $packageName (thiếu Shizuku)")
            return@withContext false
        }
        val result = shellExecutor.execute("cmd package compile -m speed $packageName")
        val success = result is ShellResult.Success
        logManager.log(LogCategory.SHELL_COMMAND, "Smart Compilation $packageName: thành công=$success")
        success
    }

    /** Force Stop triệt để một package cụ thể qua Shizuku (mạnh hơn killBackgroundProcesses). */
    suspend fun forceStop(packageName: String): Boolean {
        if (packageName in protectedAppRepository.observeEnabledPackages().first()) {
            logManager.log(LogCategory.SHELL_COMMAND, "Từ chối force-stop $packageName (đang được bảo vệ)")
            return false
        }
        val result = shellExecutor.forceStopPackage(packageName)
        return result is ShellResult.Success
    }

    /** Gộp Deep RAM Cleaner + Cache Cleaner + Storage Trim — dùng trước khi vào game. */
    suspend fun runFullOptimize(): OptimizeResult {
        val ram = deepRamClean()
        val cache = clearCache()
        val trim = storageTrim()
        return OptimizeResult(
            ramFreedBytes = ram.ramFreedBytes,
            backgroundAppsKilled = ram.backgroundAppsKilled,
            cacheClearedBytes = cache.cacheClearedBytes,
            fstrimRan = trim.fstrimRan,
            protectedAppsSkipped = ram.protectedAppsSkipped,
            skippedActions = ram.skippedActions + cache.skippedActions + trim.skippedActions,
        )
    }

    private fun sizeOf(file: java.io.File): Long =
        if (file.isDirectory) file.listFiles().orEmpty().sumOf { sizeOf(it) } else file.length()
}
