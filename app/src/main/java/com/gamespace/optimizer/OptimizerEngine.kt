package com.gamespace.optimizer

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.permissions.PermissionChecker
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
    private val permissionChecker: PermissionChecker,
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

    /** Mốc thời gian lần Deep RAM Cleaner chạy gần nhất — dùng cho cooldown, xem [deepRamClean]. */
    @Volatile
    private var lastRamCleanAtMillis: Long = 0L

    /**
     * Deep RAM Cleaner + Background Process Cleaner: kill các process nền có
     * importance >= [ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND]
     * (không đụng foreground/foreground_service/perceptible/visible), trừ app hệ thống
     * và app nhạc đang được bảo vệ (xem [ProtectedAppRepository]).
     *
     * Có cooldown [RAM_CLEAN_COOLDOWN_MILLIS]: dọn RAM liên tục (spam nút "Dọn RAM" trên
     * Dashboard/Overlay) khiến hàng loạt app bị kill rồi hệ thống lập tức respawn lại nhiều
     * process cùng lúc — CPU phải làm việc liên tục để khởi động lại các app đó, dễ làm máy
     * nóng hơn là giúp ích (nóng máy vì dọn RAM liên tục — không phải do bản thân việc kill
     * process, mà do chu kỳ kill→respawn lặp lại quá nhanh). Trong cooldown, trả về kết quả
     * rỗng kèm lý do trong `skippedActions` thay vì chạy lại.
     */
    suspend fun deepRamClean(): OptimizeResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val elapsedSinceLastClean = now - lastRamCleanAtMillis
        if (elapsedSinceLastClean < RAM_CLEAN_COOLDOWN_MILLIS) {
            val waitSeconds = (RAM_CLEAN_COOLDOWN_MILLIS - elapsedSinceLastClean) / 1_000
            return@withContext OptimizeResult(
                skippedActions = listOf(
                    "Vừa dọn RAM cách đây chưa lâu — đợi thêm ${waitSeconds}s (dọn RAM liên tục dễ làm nóng máy)",
                ),
            )
        }
        lastRamCleanAtMillis = now

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
     * Cache Cleaner: xóa cache của chính GAME SPACE ngay (luôn khả dụng), sau đó chỉ dọn cache
     * của CÁC APP KHÁC có dung lượng cache vượt [CACHE_THRESHOLD_BYTES] (100MB) — không trim mù
     * toàn hệ thống như trước (từng dùng `pm trim-caches 999G`, dọn cả app chỉ có vài KB cache,
     * không cần thiết và tốn thời gian I/O hơn hẳn).
     *
     * Cần Shizuku (để chạy `pm clear --cache-only`, public API không cho app thường xóa cache
     * app khác trực tiếp — chỉ chủ app hoặc shell mới có quyền) VÀ quyền Usage Access (để đọc
     * dung lượng cache từng app qua [StorageStatsManager.queryStatsForPackage] — public API chặn
     * đọc thông tin storage của app khác nếu thiếu quyền này, xem [PermissionChecker]).
     * Thiếu Usage Access thì không lọc được app nào >100MB — fallback về trim toàn hệ thống như
     * hành vi cũ để tính năng vẫn hoạt động, chỉ là không lọc theo ngưỡng được.
     */
    suspend fun clearCache(): OptimizeResult = withContext(Dispatchers.IO) {
        val ownCacheBytes = sizeOf(context.cacheDir)
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()

        val skipped = mutableListOf<String>()
        var otherAppsCleared = 0
        var otherAppsCacheBytes = 0L

        when {
            !capabilityDetector.canRunShellCommands() -> {
                skipped += "Dọn cache app khác (cần Shizuku — chưa cấp quyền)"
            }
            !permissionChecker.hasUsageAccessPermission() -> {
                skipped += "Không lọc được app >100MB cache (cần quyền Usage Access) — trim toàn hệ thống thay thế"
                val result = shellExecutor.execute("pm trim-caches 999G")
                if (result !is ShellResult.Success) skipped += "pm trim-caches thất bại: $result"
            }
            else -> {
                val largeCacheApps = findAppsWithCacheOver(CACHE_THRESHOLD_BYTES)
                largeCacheApps.forEach { (packageName, cacheBytes) ->
                    val result = shellExecutor.execute("pm clear --cache-only $packageName")
                    if (result is ShellResult.Success) {
                        otherAppsCleared += 1
                        otherAppsCacheBytes += cacheBytes
                    } else {
                        skipped += "Dọn cache $packageName thất bại"
                    }
                }
            }
        }

        logManager.log(
            LogCategory.SHELL_COMMAND,
            "Cache Cleaner: dọn ${ownCacheBytes / 1024}KB cache riêng" +
                if (otherAppsCleared > 0) {
                    ", $otherAppsCleared app khác >100MB (~${otherAppsCacheBytes / 1_048_576}MB)"
                } else {
                    ""
                },
        )
        OptimizeResult(
            cacheClearedBytes = ownCacheBytes + otherAppsCacheBytes,
            skippedActions = skipped,
        )
    }

    /**
     * Duyệt toàn bộ app đã cài (trừ chính GAME SPACE), trả về những app có `cacheBytes` vượt
     * [thresholdBytes]. Yêu cầu quyền Usage Access — gọi hàm này khi chưa có quyền sẽ chỉ trả
     * về danh sách rỗng thay vì crash (mỗi package đọc lỗi cũng bị bỏ qua tương tự, vì
     * `queryStatsForPackage` có thể ném lỗi cho app đã gỡ giữa lúc liệt kê và lúc đọc).
     */
    private fun findAppsWithCacheOver(thresholdBytes: Long): List<Pair<String, Long>> {
        val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                ?: return emptyList()
        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstalledApplications(0)
        }
        val userHandle = Process.myUserHandle()

        return installedApps
            .filterNot { it.packageName == context.packageName }
            .mapNotNull { appInfo ->
                runCatching {
                    storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, appInfo.packageName, userHandle)
                }.getOrNull()?.let { stats -> appInfo.packageName to stats.cacheBytes }
            }
            .filter { (_, cacheBytes) -> cacheBytes > thresholdBytes }
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

    companion object {
        /** Ngưỡng dung lượng cache để một app bị đưa vào diện dọn (Cache Cleaner). */
        private const val CACHE_THRESHOLD_BYTES = 100L * 1024 * 1024

        /** Khoảng cách tối thiểu giữa hai lần Deep RAM Cleaner — xem KDoc ở [deepRamClean]. */
        private const val RAM_CLEAN_COOLDOWN_MILLIS = 30_000L
    }
}
