package com.gamespace.ui.playtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.packagemanager.GameEntity
import com.gamespace.packagemanager.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlaytimeUiState(
    val games: List<GameEntity> = emptyList(),
    val totalPlayTimeMillis: Long = 0L,
    val isLoading: Boolean = true,
)

/**
 * "Thời gian chơi" — chỉ ĐỌC dữ liệu đã có sẵn từ [GameRepository] (`totalPlayTimeMillis` /
 * `lastPlayedMillis`, cột đã tồn tại trong [GameEntity] từ trước). Dữ liệu này do
 * [com.gamespace.automation.GameDetectionService] ghi nhận qua `recordSession()` khi service
 * chạy nền — bật/tắt tính năng này nằm ở tab Hồ sơ (mục "Tự động tối ưu & theo dõi thời gian
 * chơi"), không lặp lại ở đây.
 */
@HiltViewModel
class PlaytimeViewModel @Inject constructor(
    private val gameRepository: GameRepository,
) : ViewModel() {

    val uiState: StateFlow<PlaytimeUiState> = gameRepository.observeGames()
        .map { games ->
            val played = games.filter { it.totalPlayTimeMillis > 0 }
                .sortedByDescending { it.totalPlayTimeMillis }
            PlaytimeUiState(
                games = played,
                totalPlayTimeMillis = played.sumOf { it.totalPlayTimeMillis },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaytimeUiState())
}
