package com.preet.detoxlauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.model.IntentionalReason
import com.preet.detoxlauncher.data.preferences.LauncherSettings
import com.preet.detoxlauncher.data.repository.LaunchDecision
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val favoriteApps: List<AppInfo> = emptyList(),
    val todayScreenTimeMinutes: Long = 0L,
    val todayUnlockCount: Int = 0,
    val pendingLaunchApp: AppInfo? = null,
    val activeDecision: LaunchDecision? = null
)

sealed class HomeUiEvent {
    data class ShowToast(val message: String) : HomeUiEvent()
    data class OpenStrictBlock(val packageName: String, val reason: String, val resetTime: String) : HomeUiEvent()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val appRepo = app.appRepository
    private val usageRepo = app.usageRepository
    private val detoxRepo = app.detoxRepository
    private val userPrefs = app.userPreferences

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }

        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                appRepo.getFavoriteApps(settings.activeIconPack).collect { favorites ->
                    _uiState.value = _uiState.value.copy(favoriteApps = favorites)
                }
            }
        }

        refreshUsageMetrics()
    }

    fun refreshUsageMetrics() {
        viewModelScope.launch {
            val screenTimeMillis = usageRepo.getTodayTotalScreenTimeMillis()
            val screenTimeMinutes = screenTimeMillis / (1000 * 60)
            val unlocks = usageRepo.getTodayUnlockCount()
            _uiState.value = _uiState.value.copy(
                todayScreenTimeMinutes = screenTimeMinutes,
                todayUnlockCount = unlocks
            )
        }
    }

    fun onAppClicked(appInfo: AppInfo) {
        viewModelScope.launch {
            // Essential apps always bypass restriction friction
            if (appInfo.isEssential) {
                appRepo.launchApp(appInfo.packageName, appInfo.activityName)
                return@launch
            }

            val decision = detoxRepo.evaluateAppLaunch(appInfo.packageName)
            when (decision) {
                is LaunchDecision.Allow -> {
                    appRepo.launchApp(appInfo.packageName, appInfo.activityName)
                }
                is LaunchDecision.RequireIntentionalPrompt,
                is LaunchDecision.RequireDelay,
                is LaunchDecision.SoftBlock -> {
                    _uiState.value = _uiState.value.copy(
                        pendingLaunchApp = appInfo,
                        activeDecision = decision
                    )
                }
                is LaunchDecision.StrictBlock -> {
                    _events.emit(
                        HomeUiEvent.OpenStrictBlock(
                            packageName = appInfo.packageName,
                            reason = "Daily limit of ${decision.limitMinutes}m reached (${decision.currentUsageMinutes}m used).",
                            resetTime = decision.resetTimeFormatted
                        )
                    )
                }
                is LaunchDecision.ScheduledBlock -> {
                    _events.emit(
                        HomeUiEvent.OpenStrictBlock(
                            packageName = appInfo.packageName,
                            reason = "Restricted by active schedule (${decision.windowString}).",
                            resetTime = "End of schedule"
                        )
                    )
                }
            }
        }
    }

    fun onIntentionalReasonConfirmed(reason: IntentionalReason) {
        val pending = _uiState.value.pendingLaunchApp ?: return
        val currentDecision = _uiState.value.activeDecision

        viewModelScope.launch {
            detoxRepo.logIntentionalOpen(pending.packageName, reason)

            // If there's an opening delay attached to this app, transition to delay
            val delaySeconds = (currentDecision as? LaunchDecision.RequireIntentionalPrompt)?.delaySeconds ?: 0
            if (delaySeconds > 0) {
                _uiState.value = _uiState.value.copy(
                    activeDecision = LaunchDecision.RequireDelay(delaySeconds)
                )
            } else {
                dismissPendingLaunch()
                appRepo.launchApp(pending.packageName, pending.activityName)
            }
        }
    }

    fun onDelayCountdownFinished() {
        val pending = _uiState.value.pendingLaunchApp ?: return
        dismissPendingLaunch()
        viewModelScope.launch {
            appRepo.launchApp(pending.packageName, pending.activityName)
        }
    }

    fun onSoftBlockContinueAnyway() {
        val pending = _uiState.value.pendingLaunchApp ?: return
        dismissPendingLaunch()
        viewModelScope.launch {
            appRepo.launchApp(pending.packageName, pending.activityName)
        }
    }

    fun dismissPendingLaunch() {
        _uiState.value = _uiState.value.copy(
            pendingLaunchApp = null,
            activeDecision = null
        )
    }
}
