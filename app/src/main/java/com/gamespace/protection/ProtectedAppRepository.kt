package com.gamespace.protection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Thay cho "Music Hub" — bảo vệ app nghe nhạc CÓ SẴN trên máy khỏi bị
 * [com.gamespace.optimizer.OptimizerEngine] dọn nền/buộc dừng khi tối ưu (ADR-009).
 *
 * Phát hiện app nhạc qua `ApplicationInfo.category == CATEGORY_AUDIO` (API 26+, cờ do
 * chính nhà phát triển app khai báo trong Manifest của họ — không phải mọi app nhạc
 * đều khai đúng). KHÔNG hard-code danh sách package name cụ thể (vd. đoán package của
 * Zing MP3/NhacCuaTui) vì dễ sai/lỗi thời — người dùng luôn có thể tự thêm thủ công
 * qua [addManually] nếu app nhạc của họ không tự nhận diện được.
 */
@Singleton
class ProtectedAppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ProtectedAppDatabase,
    private val logManager: LogManager,
) {
    private val dao get() = database.protectedAppDao()
    private val packageManager get() = context.packageManager

    fun observeAll(): Flow<List<ProtectedAppEntity>> = dao.observeAll()

    /** Danh sách package hiện đang được bảo vệ — [com.gamespace.optimizer.OptimizerEngine] dùng để loại trừ. */
    fun observeEnabledPackages(): Flow<Set<String>> = dao.observeEnabledPackages().map { it.toSet() }

    suspend fun syncInstalledMusicApps() = withContext(Dispatchers.IO) {
        val apps = queryInstalledAudioApps()
        dao.insertAllIfAbsent(apps)
        logManager.log(LogCategory.SHELL_COMMAND, "Đồng bộ app nhạc: tìm thấy ${apps.size} app category AUDIO")
    }

    private fun queryInstalledAudioApps(): List<ProtectedAppEntity> {
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        return apps.filter { it.category == ApplicationInfo.CATEGORY_AUDIO }
            .map { appInfo ->
                ProtectedAppEntity(
                    packageName = appInfo.packageName,
                    appLabel = appInfo.loadLabel(packageManager).toString(),
                    isEnabled = true, // mặc định bảo vệ ngay khi phát hiện — an toàn hơn cho trải nghiệm nghe nhạc
                    addedManually = false,
                )
            }
    }

    suspend fun setEnabled(packageName: String, isEnabled: Boolean) = dao.setEnabled(packageName, isEnabled)

    suspend fun addManually(packageName: String) {
        val label = runCatching {
            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
        dao.insertAllIfAbsent(
            listOf(ProtectedAppEntity(packageName = packageName, appLabel = label, addedManually = true)),
        )
    }

    suspend fun removeManual(packageName: String) = dao.removeManual(packageName)
}
