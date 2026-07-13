package com.gamespace.ui.diagnostics

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.diagnostics.DiagnosticsGenerator
import com.gamespace.diagnostics.DiagnosticsReport
import com.gamespace.logging.LogManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val report: DiagnosticsReport? = null,
    val isLoading: Boolean = true,
    val shareUri: Uri? = null,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnosticsGenerator: DiagnosticsGenerator,
    private val logManager: LogManager,
) : ViewModel() {

    private val reportFlow = MutableStateFlow<DiagnosticsReport?>(null)
    private val isLoadingFlow = MutableStateFlow(true)
    private val shareUriFlow = MutableStateFlow<Uri?>(null)

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        reportFlow,
        isLoadingFlow,
        shareUriFlow,
    ) { report, isLoading, shareUri ->
        DiagnosticsUiState(report, isLoading, shareUri)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoadingFlow.value = true
            reportFlow.value = diagnosticsGenerator.generate()
            isLoadingFlow.value = false
        }
    }

    fun exportDiagnostics() {
        viewModelScope.launch {
            shareUriFlow.value = diagnosticsGenerator.exportToFile(reportFlow.value ?: diagnosticsGenerator.generate())
        }
    }

    fun exportLogs() {
        viewModelScope.launch {
            shareUriFlow.value = logManager.exportToFile()
        }
    }

    fun consumeShareEvent() {
        shareUriFlow.value = null
    }
}
