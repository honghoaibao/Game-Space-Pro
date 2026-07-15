package com.gamespace.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.optimizer.OptimizerEngine
import com.gamespace.packagemanager.GameEntity
import com.gamespace.packagemanager.GameRepository
import com.gamespace.packagemanager.InstalledAppInfo
import com.gamespace.profile.ProfileType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameLibraryUiState(
    val games: List<GameEntity> = emptyList(),
    val isSyncing: Boolean = true,
    val isCompiling: Set<String> = emptySet(),
)

data class AppPickerUiState(
    val apps: List<InstalledAppInfo> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
    private val optimizerEngine: OptimizerEngine,
) : ViewModel() {

    private val isSyncingFlow = MutableStateFlow(true)
    private val compilingFlow = MutableStateFlow<Set<String>>(emptySet())
    private val appPickerFlow = MutableStateFlow(AppPickerUiState())

    val uiState: StateFlow<GameLibraryUiState> = combine(
        gameRepository.observeGames(),
        isSyncingFlow,
        compilingFlow,
    ) { games, isSyncing, compiling ->
        GameLibraryUiState(games, isSyncing, compiling)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameLibraryUiState())

    val appPickerUiState: StateFlow<AppPickerUiState> = appPickerFlow.asStateFlow()

    init {
        sync()
    }

    /**
     * Nạp danh sách app đã cài cho dialog "Chọn ứng dụng" (mở khi bấm nút +). Chỉ quét lại
     * nếu chưa có dữ liệu — `queryIntentActivities` quét toàn bộ app trên máy nên khá tốn,
     * không cần lặp lại mỗi lần người dùng mở/đóng dialog trong cùng một phiên màn hình.
     */
    fun loadInstalledApps() {
        if (appPickerFlow.value.apps.isNotEmpty() || appPickerFlow.value.isLoading) return
        viewModelScope.launch {
            appPickerFlow.value = AppPickerUiState(isLoading = true)
            val apps = gameRepository.queryAllLaunchableApps()
            appPickerFlow.value = AppPickerUiState(apps = apps, isLoading = false)
        }
    }

    fun sync() {
        viewModelScope.launch {
            isSyncingFlow.value = true
            gameRepository.syncInstalledGames()
            isSyncingFlow.value = false
        }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch { gameRepository.toggleFavorite(game.packageName, !game.isFavorite) }
    }

    fun assignProfile(game: GameEntity, profile: ProfileType?) {
        viewModelScope.launch { gameRepository.assignProfile(game.packageName, profile) }
    }

    fun launch(game: GameEntity) {
        val intent = gameRepository.getLaunchIntent(game.packageName) ?: return
        // Dùng Application Context để start Activity bắt buộc phải có FLAG_ACTIVITY_NEW_TASK,
        // nếu không hệ thống sẽ ném "Calling startActivity() from outside of an Activity context".
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun addManually(packageName: String) {
        if (packageName.isBlank()) return
        viewModelScope.launch { gameRepository.addManually(packageName.trim()) }
    }

    fun smartCompile(game: GameEntity) {
        viewModelScope.launch {
            compilingFlow.value = compilingFlow.value + game.packageName
            optimizerEngine.smartCompile(game.packageName)
            compilingFlow.value = compilingFlow.value - game.packageName
        }
    }
}
