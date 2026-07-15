package com.gamespace.ui.ghome

import android.content.Context
import android.content.Intent
import com.gamespace.packagemanager.GameEntity
import com.gamespace.packagemanager.GameRepository
import com.gamespace.packagemanager.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val RECENTLY_PLAYED_LIMIT = 5

data class GHomeUiState(
    val games: List<GameEntity> = emptyList(),
    val recentlyPlayed: List<GameEntity> = emptyList(),
    val isLoading: Boolean = true,
)

data class AppPickerUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * G-Home: trang chủ để CHỌN GAME VÀ MỞ NGAY. KHÔNG tự quét/thêm app category=GAME (khác
 * [com.gamespace.ui.library.GameLibraryViewModel] cũ) — danh sách chỉ gồm game người dùng chủ
 * động thêm qua nút "+", đúng yêu cầu "ban đầu không có game, người dùng phải tự chọn".
 */
@HiltViewModel
class GHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
) : ViewModel() {

    val uiState: StateFlow<GHomeUiState> = gameRepository.observeGames()
        .map { games ->
            GHomeUiState(
                games = games.sortedBy { it.appLabel.lowercase() },
                recentlyPlayed = games
                    .filter { it.lastPlayedMillis != null }
                    .sortedByDescending { it.lastPlayedMillis }
                    .take(RECENTLY_PLAYED_LIMIT),
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GHomeUiState())

    private val appPickerFlow = MutableStateFlow(AppPickerUiState())
    val appPickerUiState: StateFlow<AppPickerUiState> = appPickerFlow.asStateFlow()

    /** Nạp danh sách app đã cài cho dialog "+" — chỉ quét lại nếu chưa có dữ liệu trong phiên. */
    fun loadInstalledApps() {
        if (appPickerFlow.value.apps.isNotEmpty() || appPickerFlow.value.isLoading) return
        viewModelScope.launch {
            appPickerFlow.value = AppPickerUiState(isLoading = true)
            val apps = gameRepository.queryAllLaunchableApps()
            appPickerFlow.value = AppPickerUiState(apps = apps, isLoading = false)
        }
    }

    fun addGame(packageName: String) {
        viewModelScope.launch { gameRepository.addManually(packageName) }
    }

    /** G-Home không có màn chi tiết — chạm vào game là mở game luôn. */
    fun launchGame(packageName: String) {
        val intent = gameRepository.getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
