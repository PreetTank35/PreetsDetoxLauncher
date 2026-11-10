package com.preet.detoxlauncher.ui.detox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.local.entity.AppItemEntity
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.model.DetoxRule
import com.preet.detoxlauncher.data.model.RestrictionMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetoxSettingsUiState(
    val appInfo: AppInfo? = null,
    val rule: DetoxRule = DetoxRule(""),
    val isSaved: Boolean = false
)

sealed class DetoxSettingsEvent {
    data class ShowMessage(val message: String) : DetoxSettingsEvent()
    data object Finish : DetoxSettingsEvent()
}

class DetoxSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val detoxRepo = app.detoxRepository
    private val appRepo = app.appRepository
    private val appDao = app.database.appDao()

    private val _uiState = MutableStateFlow(DetoxSettingsUiState())
    val uiState: StateFlow<DetoxSettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DetoxSettingsEvent>()
    val events: SharedFlow<DetoxSettingsEvent> = _events.asSharedFlow()

    fun loadRule(packageName: String) {
        viewModelScope.launch {
            val entity = appDao.getAppByPackage(packageName) ?: return@launch
            val appInfo = AppInfo(
                packageName = entity.packageName,
                activityName = entity.activityName,
                label = entity.systemLabel,
                customLabel = entity.customLabel,
                iconDrawable = appRepo.loadAppIcon(entity, null)
            )

            detoxRepo.getRuleForPackage(packageName).collect { existingRule ->
                _uiState.value = _uiState.value.copy(
                    appInfo = appInfo,
                    rule = existingRule ?: DetoxRule(packageName = packageName)
                )
            }
        }
    }

    fun setRestrictionMode(mode: RestrictionMode) {
        _uiState.value = _uiState.value.copy(
            rule = _uiState.value.rule.copy(mode = mode)
        )
    }

    fun setDailyLimitMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            rule = _uiState.value.rule.copy(dailyLimitMinutes = minutes)
        )
    }

    fun setRequiresIntentionalPrompt(required: Boolean) {
        _uiState.value = _uiState.value.copy(
            rule = _uiState.value.rule.copy(requiresIntentionalPrompt = required)
        )
    }

    fun setDelaySeconds(seconds: Int) {
        _uiState.value = _uiState.value.copy(
            rule = _uiState.value.rule.copy(delaySeconds = seconds)
        )
    }

    fun setScheduleWindow(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        _uiState.value = _uiState.value.copy(
            rule = _uiState.value.rule.copy(
                scheduleStartHour = startHour,
                scheduleStartMinute = startMin,
                scheduleEndHour = endHour,
                scheduleEndMinute = endMin
            )
        )
    }

    fun saveRule() {
        viewModelScope.launch {
            val currentRule = _uiState.value.rule
            detoxRepo.saveRule(currentRule)
            _events.emit(DetoxSettingsEvent.ShowMessage("Detox boundary saved."))
            _events.emit(DetoxSettingsEvent.Finish)
        }
    }

    fun clearRule() {
        viewModelScope.launch {
            val pkg = _uiState.value.rule.packageName
            detoxRepo.deleteRule(pkg)
            _events.emit(DetoxSettingsEvent.ShowMessage("Detox rule removed."))
            _events.emit(DetoxSettingsEvent.Finish)
        }
    }
}
