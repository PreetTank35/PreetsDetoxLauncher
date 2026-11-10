package com.preet.detoxlauncher.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.local.entity.FocusSessionEntity
import com.preet.detoxlauncher.data.model.PomodoroState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FocusUiState(
    val pomodoroState: PomodoroState = PomodoroState(),
    val todayFocusMinutes: Int = 0,
    val recentSessions: List<FocusSessionEntity> = emptyList()
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DetoxApplication
    private val focusRepo = app.focusRepository
    private val database = app.database

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            focusRepo.pomodoroState.collect { state ->
                _uiState.value = _uiState.value.copy(pomodoroState = state)
            }
        }

        viewModelScope.launch {
            database.focusSessionDao().getAllSessions().collect { sessions ->
                _uiState.value = _uiState.value.copy(recentSessions = sessions)
                refreshMetrics()
            }
        }
    }

    fun startStandardFocus(minutes: Int = 25) {
        focusRepo.startFocusSession(minutes)
    }

    fun startBreak(minutes: Int = 5) {
        focusRepo.startBreak(minutes)
    }

    fun pause() {
        focusRepo.pause()
    }

    fun resume() {
        focusRepo.resume()
    }

    fun stop() {
        focusRepo.stop()
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            val focusMins = focusRepo.getTodayFocusMinutes()
            _uiState.value = _uiState.value.copy(todayFocusMinutes = focusMins)
        }
    }
}
