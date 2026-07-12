package com.gamespace.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.overlay.HudMetrics
import com.gamespace.overlay.HudMetricsCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerformanceCenterUiState(
    val history: List<HudMetrics> = emptyList(),
) {
    val latest: HudMetrics? get() = history.lastOrNull()
}

/**
 * Mục "Performance Center" (Phiên 6) — tái dùng [HudMetricsCollector] (đã xây ở Phiên 3
 * cho Overlay HUD) làm nguồn dữ liệu, giữ một cửa sổ trượt gần nhất để vẽ biểu đồ.
 * An toàn chạy song song với Overlay HUD nhờ mỗi lần gọi `metricsFlow()` tự tạo bộ đếm
 * frame riêng (xem ghi chú trong `HudMetricsCollector`).
 */
@HiltViewModel
class PerformanceCenterViewModel @Inject constructor(
    private val metricsCollector: HudMetricsCollector,
) : ViewModel() {

    private val uiStateFlow = MutableStateFlow(PerformanceCenterUiState())
    val uiState: StateFlow<PerformanceCenterUiState> = uiStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            metricsCollector.metricsFlow(intervalMillis = 1_000).collect { metrics ->
                uiStateFlow.update { current ->
                    current.copy(history = (current.history + metrics).takeLast(MAX_SAMPLES))
                }
            }
        }
    }

    companion object {
        private const val MAX_SAMPLES = 60 // ~1 phút dữ liệu ở chu kỳ 1s
    }
}
