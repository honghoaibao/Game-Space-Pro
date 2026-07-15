package com.gamespace.logging

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Điểm ghi log duy nhất cho toàn bộ app: Shell Commands, Accessibility, Shizuku,
 * Profile, Game Launch, Thermal, Errors (mục "11. Logging" trong đặc tả).
 *
 * Mọi module khác (ShellExecutor, ProfileEngine, ThermalMonitor, AccessibilityService...)
 * nên inject [LogManager] thay vì tự viết log riêng, để đảm bảo export report luôn đầy đủ.
 */
@Singleton
class LogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LogDatabase,
) {
    private val dao get() = database.logDao()

    suspend fun log(category: LogCategory, message: String) {
        dao.insert(LogEntry(timestampMillis = System.currentTimeMillis(), category = category, message = message))
    }

    fun observeRecent(limit: Int = 500): Flow<List<LogEntry>> = dao.observeRecent(limit)

    suspend fun trimOlderThanDays(days: Int = 7) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        dao.trimOlderThan(cutoff)
    }

    suspend fun clearAll() = dao.clearAll()

    /**
     * Xuất toàn bộ log ra file .txt trong cache dir và trả về Uri chia sẻ được
     * (qua FileProvider) để UI gọi ACTION_SEND.
     */
    suspend fun exportToFile(): android.net.Uri {
        val entries = dao.getAllForExport()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val content = buildString {
            appendLine("GAME SPACE — Log export")
            appendLine("Tạo lúc: ${formatter.format(Date())}")
            appendLine("=".repeat(48))
            entries.forEach { entry ->
                appendLine("[${formatter.format(Date(entry.timestampMillis))}] [${entry.category}] ${entry.message}")
            }
        }
        val exportDir = File(context.cacheDir, "logs").apply { mkdirs() }
        val file = File(exportDir, "gamespace_log_${System.currentTimeMillis()}.txt")
        file.writeText(content)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
