package com.preet.detoxlauncher.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.preferences.LauncherSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val hasUsagePermission: Boolean = false,
    val isGrayscaleActive: Boolean = false,
    val isOperating: Boolean = false
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val userPrefs = app.userPreferences
    private val usageRepo = app.usageRepository
    private val backupRepo = app.backupRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    hasUsagePermission = usageRepo.hasUsagePermission()
                )
            }
        }
    }

    fun setPureAmoled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPureAmoled(enabled) }
    }

    fun setAccentColorIndex(index: Int) {
        viewModelScope.launch { userPrefs.setAccentColorIndex(index) }
    }

    fun setClockSize(size: String) {
        viewModelScope.launch { userPrefs.setClockSize(size) }
    }

    fun setClockVisible(visible: Boolean) {
        viewModelScope.launch { userPrefs.setClockVisible(visible) }
    }

    fun setDateVisible(visible: Boolean) {
        viewModelScope.launch { userPrefs.setDateVisible(visible) }
    }

    fun setMotivationalMessage(message: String) {
        viewModelScope.launch { userPrefs.setMotivationalMessage(message) }
    }

    fun setDailyScreenTimeTarget(minutes: Int) {
        viewModelScope.launch { userPrefs.setDailyScreenTimeTarget(minutes) }
    }

    fun setShowUsageSummary(visible: Boolean) {
        viewModelScope.launch { userPrefs.setShowUsageSummary(visible) }
    }

    fun setLightMode(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setLightMode(enabled) }
    }

    fun toggleGrayscale() {
        val next = !_uiState.value.isGrayscaleActive
        _uiState.value = _uiState.value.copy(isGrayscaleActive = next)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperating = true)
            val result = backupRepo.exportBackupToUri(uri)
            _uiState.value = _uiState.value.copy(isOperating = false)
            _events.emit(SettingsEvent.ShowToast(result.message))
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperating = true)
            val result = backupRepo.importBackupFromUri(uri)
            _uiState.value = _uiState.value.copy(isOperating = false)
            _events.emit(SettingsEvent.ShowToast(result.message))
        }
    }
}
