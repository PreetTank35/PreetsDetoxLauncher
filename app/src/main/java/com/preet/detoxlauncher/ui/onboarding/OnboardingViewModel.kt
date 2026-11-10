package com.preet.detoxlauncher.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
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

data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedAccentIndex: Int = 0,
    val isPureAmoled: Boolean = true,
    val targetMinutes: Int = 120,
    val installedApps: List<AppInfo> = emptyList(),
    val selectedDistractingPackages: Set<String> = emptySet(),
    val restrictionMode: RestrictionMode = RestrictionMode.SOFT,
    val isComplete: Boolean = false
)

sealed class OnboardingEvent {
    data object FinishOnboarding : OnboardingEvent()
    data class ShowMessage(val message: String) : OnboardingEvent()
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val userPrefs = app.userPreferences
    private val detoxRepo = app.detoxRepository
    private val appRepo = app.appRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            appRepo.getVisibleApps(null).collect { apps ->
                _uiState.value = _uiState.value.copy(installedApps = apps)
            }
        }
    }

    fun nextStep() {
        val next = _uiState.value.currentStep + 1
        if (next > 12) {
            finishOnboarding()
        } else {
            _uiState.value = _uiState.value.copy(currentStep = next)
        }
    }

    fun previousStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(currentStep = prev)
    }

    fun setAccentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedAccentIndex = index)
        viewModelScope.launch { userPrefs.setAccentColorIndex(index) }
    }

    fun setPureAmoled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isPureAmoled = enabled)
        viewModelScope.launch { userPrefs.setPureAmoled(enabled) }
    }

    fun setTargetMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(targetMinutes = minutes)
        viewModelScope.launch { userPrefs.setDailyScreenTimeTarget(minutes) }
    }

    fun toggleDistractingApp(packageName: String) {
        val current = _uiState.value.selectedDistractingPackages.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        _uiState.value = _uiState.value.copy(selectedDistractingPackages = current)
    }

    fun setRestrictionMode(mode: RestrictionMode) {
        _uiState.value = _uiState.value.copy(restrictionMode = mode)
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            // Apply selected distracting apps restrictions
            val mode = _uiState.value.restrictionMode
            for (pkg in _uiState.value.selectedDistractingPackages) {
                detoxRepo.saveRule(
                    DetoxRule(
                        packageName = pkg,
                        mode = mode,
                        dailyLimitMinutes = 45,
                        requiresIntentionalPrompt = true
                    )
                )
            }
            _events.emit(OnboardingEvent.FinishOnboarding)
        }
    }
}
