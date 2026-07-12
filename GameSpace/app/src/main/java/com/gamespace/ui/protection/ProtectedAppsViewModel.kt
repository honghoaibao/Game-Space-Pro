package com.gamespace.ui.protection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamespace.protection.ProtectedAppEntity
import com.gamespace.protection.ProtectedAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProtectedAppsUiState(
    val apps: List<ProtectedAppEntity> = emptyList(),
    val isSyncing: Boolean = true,
)

@HiltViewModel
class ProtectedAppsViewModel @Inject constructor(
    private val repository: ProtectedAppRepository,
) : ViewModel() {

    private val isSyncingFlow = MutableStateFlow(true)

    val uiState: StateFlow<ProtectedAppsUiState> = combine(
        repository.observeAll(),
        isSyncingFlow,
    ) { apps, isSyncing ->
        ProtectedAppsUiState(apps, isSyncing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProtectedAppsUiState())

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            isSyncingFlow.value = true
            repository.syncInstalledMusicApps()
            isSyncingFlow.value = false
        }
    }

    fun toggleEnabled(app: ProtectedAppEntity) {
        viewModelScope.launch { repository.setEnabled(app.packageName, !app.isEnabled) }
    }

    fun addManually(packageName: String) {
        if (packageName.isBlank()) return
        viewModelScope.launch { repository.addManually(packageName.trim()) }
    }

    fun removeManual(app: ProtectedAppEntity) {
        viewModelScope.launch { repository.removeManual(app.packageName) }
    }
}
