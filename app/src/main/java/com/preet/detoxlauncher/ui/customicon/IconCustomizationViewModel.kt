package com.preet.detoxlauncher.ui.customicon

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.model.IconPackInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IconCustomizationUiState(
    val appInfo: AppInfo? = null,
    val installedIconPacks: List<IconPackInfo> = emptyList(),
    val activeIconPack: String? = null,
    val selectedCustomImageUri: Uri? = null,
    val customLabelText: String = "",
    val isLoading: Boolean = false
)

sealed class IconCustomizationEvent {
    data class ShowMessage(val message: String) : IconCustomizationEvent()
    data object Finish : IconCustomizationEvent()
}

class IconCustomizationViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val appRepo = app.appRepository
    private val iconPackRepo = app.iconPackRepository
    private val userPrefs = app.userPreferences
    private val appDao = app.database.appDao()

    private val _uiState = MutableStateFlow(IconCustomizationUiState())
    val uiState: StateFlow<IconCustomizationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<IconCustomizationEvent>()
    val events: SharedFlow<IconCustomizationEvent> = _events.asSharedFlow()

    init {
        loadIconPacks()
        viewModelScope.launch {
            userPrefs.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(activeIconPack = settings.activeIconPack)
            }
        }
    }

    fun loadApp(packageName: String) {
        viewModelScope.launch {
            val entity = appDao.getAppByPackage(packageName) ?: return@launch
            val activePack = _uiState.value.activeIconPack
            val appInfo = AppInfo(
                packageName = entity.packageName,
                activityName = entity.activityName,
                label = entity.systemLabel,
                customLabel = entity.customLabel,
                customIconPath = entity.customIconPath,
                iconDrawable = appRepo.loadAppIcon(entity, activePack)
            )
            _uiState.value = _uiState.value.copy(
                appInfo = appInfo,
                customLabelText = appInfo.displayLabel
            )
        }
    }

    fun loadIconPacks() {
        viewModelScope.launch {
            val packs = iconPackRepo.getInstalledIconPacks()
            _uiState.value = _uiState.value.copy(installedIconPacks = packs)
        }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(selectedCustomImageUri = uri)
    }

    fun onLabelChanged(newLabel: String) {
        _uiState.value = _uiState.value.copy(customLabelText = newLabel)
    }

    fun saveCustomizations() {
        val targetApp = _uiState.value.appInfo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Save Label
            val labelToSave = _uiState.value.customLabelText.trim()
            if (labelToSave.isNotEmpty() && labelToSave != targetApp.label) {
                appRepo.updateCustomLabel(targetApp.packageName, labelToSave)
            } else if (labelToSave.isEmpty()) {
                appRepo.updateCustomLabel(targetApp.packageName, null)
            }

            // 2. Save Custom Icon Image if picked
            val uri = _uiState.value.selectedCustomImageUri
            if (uri != null) {
                appRepo.saveCustomIcon(targetApp.packageName, uri)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
            _events.emit(IconCustomizationEvent.ShowMessage("Customization saved!"))
            _events.emit(IconCustomizationEvent.Finish)
        }
    }

    fun resetToDefault() {
        val targetApp = _uiState.value.appInfo ?: return
        viewModelScope.launch {
            appRepo.resetCustomIcon(targetApp.packageName)
            appRepo.updateCustomLabel(targetApp.packageName, null)
            _events.emit(IconCustomizationEvent.ShowMessage("Reset to system defaults."))
            _events.emit(IconCustomizationEvent.Finish)
        }
    }

    fun applyIconPack(packPackage: String?) {
        viewModelScope.launch {
            userPrefs.setActiveIconPack(packPackage)
            _events.emit(IconCustomizationEvent.ShowMessage(
                if (packPackage != null) "Icon pack applied!" else "Reverted to default icons."
            ))
        }
    }
}
