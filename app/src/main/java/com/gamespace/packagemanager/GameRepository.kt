package com.gamespace.packagemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.gamespace.logging.LogCategory
import com.gamespace.logging.LogManager
import com.gamespace.profile.ProfileType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Nguồn dữ liệu cho "Thư viện Game" (mục "9. Package Manager").
 * Quét app có `ApplicationInfo.category == CATEGORY_GAME` (API 26+) hoặc cờ
 * `FLAG_IS_GAME` (legacy, trước API 30) — game do người dùng tự thêm thủ công
 * (`addedManually = true`) không bao giờ bị dọn khỏi danh sách khi resync.
 */
@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GameDatabase,
    private val logManager: LogManager,
) {
    private val dao get() = database.gameDao()
    private val packageManager get() = context.packageManager

    fun observeGames(): Flow<List<GameEntity>> = dao.observeAll()

    suspend fun getGame(packageName: String): GameEntity? = dao.getByPackage(packageName)

    /** Quét lại toàn bộ app đã cài, thêm app mới nhận diện là Game vào DB (không ghi đè dữ liệu đã có). */
    suspend fun syncInstalledGames() = withContext(Dispatchers.IO) {
        val installedGames = queryInstalledGameApps()
        dao.insertAllIfAbsent(installedGames)
        logManager.log(LogCategory.GAME_LAUNCH, "Đồng bộ thư viện game: tìm thấy ${installedGames.size} app")
    }

    private fun queryInstalledGameApps(): List<GameEntity> {
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        return apps.filter(::isGameApp).map { appInfo ->
            GameEntity(
                packageName = appInfo.packageName,
                appLabel = appInfo.loadLabel(packageManager).toString(),
                addedManually = false,
            )
        }
    }

    private fun isGameApp(appInfo: ApplicationInfo): Boolean {
        val isCategoryGame = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appInfo.category == ApplicationInfo.CATEGORY_GAME
        @Suppress("DEPRECATION")
        val isLegacyGameFlag = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        return isCategoryGame || isLegacyGameFlag
    }

    suspend fun toggleFavorite(packageName: String, isFavorite: Boolean) =
        dao.setFavorite(packageName, isFavorite)

    suspend fun assignProfile(packageName: String, profile: ProfileType?) =
        dao.setAssignedProfile(packageName, profile)

    suspend fun recordSession(packageName: String, sessionDurationMillis: Long) =
        dao.recordSession(packageName, System.currentTimeMillis(), sessionDurationMillis)

    suspend fun addManually(packageName: String) {
        val label = runCatching {
            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
        dao.insertAllIfAbsent(listOf(GameEntity(packageName = packageName, appLabel = label, addedManually = true)))
    }

    fun getAppIcon(packageName: String): Drawable? = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    fun getLaunchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)
}
