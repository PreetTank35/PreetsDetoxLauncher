package com.preet.detoxlauncher.ui.drawer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.local.entity.CategoryEntity
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.model.IntentionalReason
import com.preet.detoxlauncher.data.model.RestrictionMode
import com.preet.detoxlauncher.data.preferences.LauncherSettings
import com.preet.detoxlauncher.data.repository.LaunchDecision
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AppDrawerUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val categories: List<String> = listOf("All", "Favorites", "Recents"),
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val selectedAppForContext: AppInfo? = null,
    val pendingLaunchApp: AppInfo? = null,
    val activeDecision: LaunchDecision? = null
)

sealed class AppDrawerEvent {
    data class OpenStrictBlock(val packageName: String, val reason: String, val resetTime: String) : AppDrawerEvent()
    data class OpenAppInfo(val packageName: String) : AppDrawerEvent()
    data class ShowMessage(val message: String) : AppDrawerEvent()
}

class AppDrawerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val appRepo = app.appRepository
    private val detoxRepo = app.detoxRepository
    private val userPrefs = app.userPreferences
    private val categoryDao = app.database.categoryDao()

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppDrawerEvent>()
    val events: SharedFlow<AppDrawerEvent> = _events.asSharedFlow()

    init {
        // Collect Settings
        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }

        // Collect Categories
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { catEntities ->
                val base = listOf("All", "Favorites", "Recents")
                val custom = catEntities.map { it.name }.filterNot { base.contains(it) }
                _uiState.value = _uiState.value.copy(categories = base + custom)
            }
        }

        // Collect Visible Apps
        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                appRepo.getVisibleApps(settings.activeIconPack).collect { apps ->
                    _uiState.value = _uiState.value.copy(allApps = apps)
                    applyFilter()
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilter()
    }

    private fun applyFilter() {
        val current = _uiState.value
        val query = current.searchQuery.trim().lowercase()
        val cat = current.selectedCategory

        val baseList = when (cat) {
            "Favorites" -> current.allApps.filter { it.isFavorite }
            "Recents" -> current.allApps.sortedByDescending { it.lastLaunchTime }
            "All" -> current.allApps
            else -> current.allApps.filter { it.category.equals(cat, ignoreCase = true) }
        }

        val filtered = if (query.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.displayLabel.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        _uiState.value = _uiState.value.copy(filteredApps = filtered)
    }

    fun onAppClicked(appInfo: AppInfo) {
        viewModelScope.launch {
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
                        AppDrawerEvent.OpenStrictBlock(
                            packageName = appInfo.packageName,
                            reason = "Daily limit of ${decision.limitMinutes}m reached (${decision.currentUsageMinutes}m used).",
                            resetTime = decision.resetTimeFormatted
                        )
                    )
                }
                is LaunchDecision.ScheduledBlock -> {
                    _events.emit(
                        AppDrawerEvent.OpenStrictBlock(
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

    fun onDelayFinished() {
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

    fun selectAppForContext(appInfo: AppInfo?) {
        _uiState.value = _uiState.value.copy(selectedAppForContext = appInfo)
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch {
            appRepo.setFavorite(appInfo.packageName, !appInfo.isFavorite)
            selectAppForContext(null)
        }
    }

    fun hideApp(appInfo: AppInfo) {
        viewModelScope.launch {
            appRepo.setHidden(appInfo.packageName, true)
            selectAppForContext(null)
            _events.emit(AppDrawerEvent.ShowMessage("${appInfo.displayLabel} hidden from launcher."))
        }
    }

    fun renameApp(appInfo: AppInfo, newName: String) {
        viewModelScope.launch {
            appRepo.updateCustomLabel(appInfo.packageName, newName)
            selectAppForContext(null)
        }
    }

    fun resetAppIcon(appInfo: AppInfo) {
        viewModelScope.launch {
            appRepo.resetCustomIcon(appInfo.packageName)
            selectAppForContext(null)
        }
    }
}
